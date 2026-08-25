package com.example.weatherly.util

import android.content.Context
import com.example.weatherly.data.model.TideStation
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nearest-tide-station lookup against the bundled `assets/tide_stations.json` (trimmed NOAA
 * CO-OPS station list, ~3500 US tide-prediction stations — see [TideStation]'s doc comment for
 * why it's bundled rather than fetched at runtime). This is the "is this location coastal" gate:
 * if nothing is within [nearest]'s `maxKm`, the location isn't treated as coastal and no tide
 * data is fetched at all — `WeatherRepository` never even calls `TideApi` for an inland location.
 */
object TideStations {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, TideStation::class.java)
    private val adapter = moshi.adapter<List<TideStation>>(listType)

    @Volatile
    private var cached: List<TideStation>? = null

    private fun load(context: Context): List<TideStation> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("tide_stations.json").bufferedReader().use { it.readText() }
            val stations = adapter.fromJson(json).orEmpty()
            cached = stations
            return stations
        }
    }

    /**
     * Nearest station within [maxKm] of (lat, lon), or null if nothing qualifies.
     *
     * Default 10km, chosen empirically after finding that a looser threshold produces real false
     * positives: NOAA's tide-prediction network follows tidal *rivers*, not just the open coast,
     * and those rivers reach surprisingly far inland — "New Brunswick, NJ" (a real station) is
     * only 12.4km from this app's own usual inland test location (Franklin Park, NJ), and
     * "Trenton, N.J." (New Jersey's state capital, ~50 miles from the ocean, but the Delaware
     * River is tidal that far upstream) has a station just 3.6km away — *closer* than a genuinely
     * coastal town like Ocean City, NJ measures to its own nearest station (4.8km). That gap
     * proves distance-to-nearest-station alone cannot perfectly separate "coastal" from
     * "far upriver but still technically tidal" — no threshold value can exclude Trenton without
     * also excluding some real coastal towns. 10km was picked to correctly exclude this app's
     * known false positives (Franklin Park 12.4km, Hillsborough Township 17.7km) while keeping
     * every genuinely coastal town checked (all under 5km); deep-tidal-river state capitals like
     * Trenton remain a known, unsolved edge case — fixing that properly would need real coastline
     * geometry (not just station proximity), which isn't in the bundled data.
     *
     * A plain linear scan over ~3500 stations (simple double-precision distance, no sorting/
     * indexing) — negligible (sub-millisecond) next to the network calls this is called
     * alongside, so not worth the complexity of a spatial index for this station count.
     */
    fun nearest(context: Context, lat: Double, lon: Double, maxKm: Double = 10.0): TideStation? {
        val stations = load(context)
        var best: TideStation? = null
        var bestDist = Double.MAX_VALUE
        for (s in stations) {
            val d = haversineKm(lat, lon, s.lat, s.lng)
            if (d < bestDist) {
                bestDist = d
                best = s
            }
        }
        return best?.takeIf { bestDist <= maxKm }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

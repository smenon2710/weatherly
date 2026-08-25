package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** NOAA CO-OPS `datagetter` response (api.tidesandcurrents.noaa.gov). Free, public domain, no key. */
data class TidePredictionsResponse(
    @Json(name = "predictions") val predictions: List<TidePredictionPoint>?
)

data class TidePredictionPoint(
    // "yyyy-MM-dd HH:mm", station-local time (requested with time_zone=lst_ldt).
    @Json(name = "t") val time: String?,
    // Height in feet or meters depending on the requested `units` — see UnitSystem.tideHeightLabel.
    @Json(name = "v") val height: String?,
    // "H" (high) or "L" (low).
    @Json(name = "type") val type: String?
)

/** One row from the bundled `assets/tide_stations.json` — trimmed from NOAA's full station
 * metadata (which also carries disclaimers, offsets, affiliations, etc. this app never uses) to
 * just what's needed for a nearest-station lookup, keeping the bundled asset small (~283KB for
 * all ~3500 US tide-prediction stations vs. ~1.9MB for the full untrimmed response). */
data class TideStation(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lng") val lng: Double
)

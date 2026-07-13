package com.example.weatherly.data.prefs

import android.content.Context
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.data.model.UnitSystem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Locale

/** Lightweight persistence for unit preference, saved places, and selection. */
class PreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("weatherly_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val placesType = Types.newParameterizedType(List::class.java, SavedPlace::class.java)
    private val placesAdapter = moshi.adapter<List<SavedPlace>>(placesType)
    private val placeAdapter = moshi.adapter(SavedPlace::class.java)

    fun getUnitSystem(): UnitSystem =
        prefs.getString(KEY_UNITS, null)?.let {
            runCatching { UnitSystem.valueOf(it) }.getOrNull()
        } ?: defaultUnits()

    fun setUnitSystem(units: UnitSystem) {
        prefs.edit().putString(KEY_UNITS, units.name).apply()
    }

    fun getPlaces(): List<SavedPlace> =
        prefs.getString(KEY_PLACES, null)?.let {
            runCatching { placesAdapter.fromJson(it) }.getOrNull()
        } ?: emptyList()

    fun setPlaces(places: List<SavedPlace>) {
        prefs.edit().putString(KEY_PLACES, placesAdapter.toJson(places)).apply()
    }

    /** null selection means "use current device location". */
    fun getSelected(): SavedPlace? =
        prefs.getString(KEY_SELECTED, null)?.let {
            runCatching { placeAdapter.fromJson(it) }.getOrNull()
        }

    fun setSelected(place: SavedPlace?) {
        prefs.edit().apply {
            if (place == null) remove(KEY_SELECTED)
            else putString(KEY_SELECTED, placeAdapter.toJson(place))
        }.apply()
    }

    // --- AI chat (OpenRouter) ---------------------------------------------
    /** Returns a user-entered key, falling back to the build-time key. */
    fun getOpenRouterKey(buildDefault: String): String =
        prefs.getString(KEY_OR_KEY, null)?.takeIf { it.isNotBlank() } ?: buildDefault

    fun setOpenRouterKey(key: String) {
        prefs.edit().apply {
            if (key.isBlank()) remove(KEY_OR_KEY) else putString(KEY_OR_KEY, key.trim())
        }.apply()
    }

    /** Whether an effective key is configured, without exposing its value to callers. */
    fun hasOpenRouterKey(buildDefault: String): Boolean =
        getOpenRouterKey(buildDefault).isNotBlank()

    fun getOpenRouterModel(buildDefault: String): String =
        prefs.getString(KEY_OR_MODEL, null)?.takeIf { it.isNotBlank() } ?: buildDefault

    fun setOpenRouterModel(model: String) {
        prefs.edit().apply {
            if (model.isBlank()) remove(KEY_OR_MODEL) else putString(KEY_OR_MODEL, model.trim())
        }.apply()
    }

    /** US (and a couple of others) default to Imperial; everyone else Metric. */
    private fun defaultUnits(): UnitSystem =
        if (Locale.getDefault().country in setOf("US", "LR", "MM")) UnitSystem.IMPERIAL
        else UnitSystem.METRIC

    // --- Appearance ---------------------------------------------------------
    fun getThemePreference(): ThemePreference =
        prefs.getString(KEY_THEME, null)?.let {
            runCatching { ThemePreference.valueOf(it) }.getOrNull()
        } ?: ThemePreference.SYSTEM

    fun setThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
    }

    companion object {
        private const val KEY_UNITS = "units"
        private const val KEY_PLACES = "places"
        private const val KEY_SELECTED = "selected"
        private const val KEY_OR_KEY = "openrouter_key"
        private const val KEY_OR_MODEL = "openrouter_model"
        private const val KEY_THEME = "theme_preference"
    }
}

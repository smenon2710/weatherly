package com.example.weatherly.data.prefs

import android.content.Context
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.data.model.TrackedAlert
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
    private val trackedAlertsType = Types.newParameterizedType(List::class.java, TrackedAlert::class.java)
    private val trackedAlertsAdapter = moshi.adapter<List<TrackedAlert>>(trackedAlertsType)

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

    /** True only when the user has entered their own key in Settings — distinct from
     * [hasOpenRouterKey], which also returns true for the build-time fallback. Gates the daily
     * LLM usage cap below: the cap protects the developer's own shared build-time key from
     * runaway cost, so it must never apply once a user is paying for their own key. */
    fun hasOwnOpenRouterKey(): Boolean =
        prefs.getString(KEY_OR_KEY, null)?.isNotBlank() == true

    /** Count of chat messages actually routed to the LLM today (never chip taps or local-intent
     * matches, which cost nothing) — resets automatically once the stored date is no longer
     * today, rather than needing an explicit midnight reset job. */
    fun getLlmUsageCountToday(): Int =
        if (prefs.getString(KEY_LLM_USAGE_DATE, null) == todayKey()) prefs.getInt(KEY_LLM_USAGE_COUNT, 0) else 0

    fun incrementLlmUsageToday() {
        prefs.edit()
            .putString(KEY_LLM_USAGE_DATE, todayKey())
            .putInt(KEY_LLM_USAGE_COUNT, getLlmUsageCountToday() + 1)
            .apply()
    }

    private fun todayKey(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    fun getOpenRouterModel(buildDefault: String): String =
        prefs.getString(KEY_OR_MODEL, null)?.takeIf { it.isNotBlank() } ?: buildDefault

    /** The model actually used for an LLM call — distinct from [getOpenRouterModel], which just
     * reads back whatever is stored (used by the Settings screen to populate the input field).
     * A stored model override only takes effect once the user has also entered their own
     * OpenRouter key ([hasOwnOpenRouterKey]): without this gate, a user on the developer's shared
     * build-time key could redirect calls to a paid or "auto"-routed model — the key stays
     * invisible to them, but every call still bills against it. Same protection principle as the
     * daily usage cap, enforced here instead so it can't be bypassed by any caller that forgets
     * to check [hasOwnOpenRouterKey] itself. */
    fun getEffectiveOpenRouterModel(buildDefault: String): String =
        if (hasOwnOpenRouterKey()) getOpenRouterModel(buildDefault) else buildDefault

    fun setOpenRouterModel(model: String) {
        prefs.edit().apply {
            if (model.isBlank()) remove(KEY_OR_MODEL) else putString(KEY_OR_MODEL, model.trim())
        }.apply()
    }

    /** US (and a couple of others) default to Imperial; everyone else Metric. */
    private fun defaultUnits(): UnitSystem =
        if (Locale.getDefault().country in setOf("US", "LR", "MM")) UnitSystem.IMPERIAL
        else UnitSystem.METRIC

    // --- Weather alerts -------------------------------------------------------
    /** The set of alert IDs+events last shown for the current place, used to detect resolution
     * (an alert that was tracked but is no longer active) across app restarts and background
     * refreshes. Callers should clear this (setTrackedAlerts(emptyList())) on a location change,
     * since a previously-tracked alert from a different place isn't a real "resolution." */
    fun getTrackedAlerts(): List<TrackedAlert> =
        prefs.getString(KEY_TRACKED_ALERTS, null)?.let {
            runCatching { trackedAlertsAdapter.fromJson(it) }.getOrNull()
        } ?: emptyList()

    fun setTrackedAlerts(alerts: List<TrackedAlert>) {
        prefs.edit().putString(KEY_TRACKED_ALERTS, trackedAlertsAdapter.toJson(alerts)).apply()
    }

    // --- Appearance ---------------------------------------------------------
    fun getThemePreference(): ThemePreference =
        prefs.getString(KEY_THEME, null)?.let {
            runCatching { ThemePreference.valueOf(it) }.getOrNull()
        } ?: ThemePreference.SYSTEM

    fun setThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
    }

    // --- Widget ---------------------------------------------------------------
    /** Whether the home-screen widget's background renders translucent instead of opaque.
     * Defaults to opaque (false) — matches the app's existing card design (GlassCard is always
     * opaque; a translucent in-app card fill was tried and reverted, see WeatherBackground's
     * doc comment) and doesn't change existing widgets' appearance unless the user opts in. */
    fun getWidgetTransparent(): Boolean = prefs.getBoolean(KEY_WIDGET_TRANSPARENT, false)

    fun setWidgetTransparent(transparent: Boolean) {
        prefs.edit().putBoolean(KEY_WIDGET_TRANSPARENT, transparent).apply()
    }

    // --- Haptics --------------------------------------------------------------
    /** Whether a single restrained haptic pulse fires when a fresh forecast loads for a notable
     * condition (active severe alert, thunderstorm, or heavy rain/snow) — see
     * util/WeatherHaptics.kt. Defaults to on; toggleable since any unsolicited vibration is
     * undesirable for some users (accessibility, quiet environments). */
    fun getHapticFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_UNITS = "units"
        private const val KEY_PLACES = "places"
        private const val KEY_SELECTED = "selected"
        private const val KEY_OR_KEY = "openrouter_key"
        private const val KEY_OR_MODEL = "openrouter_model"
        private const val KEY_THEME = "theme_preference"
        private const val KEY_TRACKED_ALERTS = "tracked_alerts"
        private const val KEY_WIDGET_TRANSPARENT = "widget_transparent"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_LLM_USAGE_DATE = "llm_usage_date"
        private const val KEY_LLM_USAGE_COUNT = "llm_usage_count"
    }
}

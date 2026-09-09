package com.example.weatherly.data.repository

import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherAlert

/**
 * Diffs a fresh alert list against a persisted "last seen" set and updates that set — the same
 * "what's new, what cleared" computation `WeatherViewModel` used to do inline for its in-app
 * resolved-alert cards, now shared with `WeatherAlertWorker`'s background notification path so
 * both read/write tracked-alert state through one function instead of two independently-written
 * computations that could quietly drift (the same reasoning behind
 * `WeatherRepository.hourEntryAt()` being a single shared function rather than two).
 *
 * Where the tracked set actually lives is up to the caller ([get]/[set]) — `WeatherViewModel` and
 * `WeatherAlertWorker` intentionally use two *different* `PreferencesStore` slots, since they can
 * be tracking two different locations at once (see `PreferencesStore.getBackgroundTrackedAlerts`'s
 * doc comment for why sharing one slot would corrupt the diff).
 */
class AlertTracker(
    private val get: () -> List<TrackedAlert>,
    private val set: (List<TrackedAlert>) -> Unit
) {
    data class Diff(val newlyAppeared: List<WeatherAlert>, val newlyResolved: List<TrackedAlert>)

    fun diffAndUpdate(current: List<WeatherAlert>): Diff {
        val previous = get()
        val previousIds = previous.map { it.id }.toSet()
        val currentIds = current.map { it.id }.toSet()
        val newlyAppeared = current.filter { it.id !in previousIds }
        val newlyResolved = previous.filter { it.id !in currentIds }
        set(current.map { TrackedAlert(it.id, it.event) })
        return Diff(newlyAppeared, newlyResolved)
    }
}

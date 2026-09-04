package com.example.weatherly.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.WeatherData

/**
 * A single, restrained haptic pulse for a freshly-loaded forecast, fired only for a notable
 * condition — an active severe alert, a thunderstorm, or genuinely heavy rain/snow. Ordinary
 * conditions (the common case) stay silent by design: this is meant to be felt occasionally for
 * something worth noticing, not a buzz on every screen open. Most-severe-wins, same principle
 * WeatherBackground's classify() already uses for the animated scene. Callers gate this on
 * [com.example.weatherly.data.prefs.PreferencesStore.getHapticFeedbackEnabled] and should only
 * call it for a foreground, user-visible load — never a silent background/periodic refresh.
 *
 * [VibrationEffect.createWaveform] with an amplitude array has existed since API 26, the app's
 * own minSdk, so this needs no version fallback beyond [VibratorManager] itself (API 31+).
 */
fun playConditionHaptic(context: Context, data: WeatherData) {
    val vibrator = context.systemVibrator() ?: return
    if (!vibrator.hasVibrator()) return
    val effect = hapticEffectFor(data) ?: return
    vibrator.vibrate(effect)
}

private fun Context.systemVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun hapticEffectFor(data: WeatherData): VibrationEffect? {
    val hasSevereAlert = data.alerts.any {
        it.severity == AlertSeverity.EXTREME || it.severity == AlertSeverity.SEVERE
    }
    return when {
        hasSevereAlert -> severeAlertEffect()
        data.currentIcon in THUNDER_CODES -> thunderEffect()
        data.currentIcon in HEAVY_PRECIP_CODES -> heavyPrecipEffect()
        else -> null
    }
}

// Same WMO codes util/WeatherIcon.kt's wmoText() already labels "Thunderstorm"/"Thunderstorm,
// hail", so this reuses an existing categorization rather than inventing a new one.
private val THUNDER_CODES = setOf(95, 96, 99)

// "Heavy rain" (65), "Heavy snow" (75), "Violent showers" (82) per wmoText() — deliberately
// excludes 85/86 (plain "Snow showers", no heavy/violent distinction in that label).
private val HEAVY_PRECIP_CODES = setOf(65, 75, 82)

/** Three sharp, escalating pulses — the strongest pattern, reserved for an active severe hazard. */
private fun severeAlertEffect(): VibrationEffect = VibrationEffect.createWaveform(
    longArrayOf(0, 80, 60, 80, 60, 160),
    intArrayOf(0, 180, 0, 220, 0, 255),
    -1
)

/** Uneven timing and amplitude to read as a distant rumble rather than a mechanical buzz. */
private fun thunderEffect(): VibrationEffect = VibrationEffect.createWaveform(
    longArrayOf(0, 40, 30, 90, 40, 50, 200, 70),
    intArrayOf(0, 110, 0, 190, 0, 90, 0, 160),
    -1
)

/** A few light, evenly-spaced clicks — a "pitter-patter" impression, not a rumble. */
private fun heavyPrecipEffect(): VibrationEffect = VibrationEffect.createWaveform(
    longArrayOf(0, 25, 45, 25, 45, 25),
    intArrayOf(0, 80, 0, 80, 0, 80),
    -1
)

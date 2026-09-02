package com.example.weatherly.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Live-updating "8:42 PM"-style clock for an IANA zone id (e.g. "America/New_York"), as returned
 * by Open-Meteo's forecast (`timezone`, via the existing `timezone=auto` request) and geocoding
 * (`timezone` per result) responses — no separate timezone lookup or API call needed.
 *
 * Recomputes from [ZonedDateTime.now] on every tick rather than caching a fixed UTC offset once,
 * so a session left open across a DST transition still reads correctly. Returns null for a null
 * or unrecognized zone id (defensive against unexpected upstream data, same as this app's other
 * `runCatching` guards around external API fields) so callers can simply omit the row/label
 * rather than render a broken clock.
 */
@Composable
fun rememberLocalTimeText(timezone: String?): String? {
    if (timezone == null) return null
    var text by remember(timezone) { mutableStateOf(formatLocalTime(timezone)) }
    LaunchedEffect(timezone) {
        while (true) {
            delay(30_000)
            text = formatLocalTime(timezone)
        }
    }
    return text
}

private fun formatLocalTime(timezone: String): String? = runCatching {
    // Locale.getDefault() read fresh on every call, not cached in a top-level val — a cached
    // formatter would freeze in whatever locale was active the first time this ran, so a live
    // in-session locale change (no process restart) would silently stop reflecting it. Matches
    // WeatherRepository.formatNwsTime()'s existing pattern for the same reason.
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    ZonedDateTime.now(ZoneId.of(timezone)).format(formatter)
}.getOrNull()

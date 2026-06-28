package com.example.weatherly.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.weatherly.MainActivity
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.prefs.ForecastCache
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.WeatherRepository
import com.example.weatherly.location.LocationProvider
import com.example.weatherly.util.weatherEmoji
import com.example.weatherly.util.wmoText
import java.util.Calendar

// ── Size breakpoints ──────────────────────────────────────────────────────────
// Glance picks the largest size that fits the widget's actual dimensions.
private val SMALL  = DpSize(110.dp,  50.dp)  // 2×1: temp + emoji only
private val MEDIUM = DpSize(110.dp, 110.dp)  // 2×2: chrono-dynamic vertical stack
private val WIDE   = DpSize(250.dp,  50.dp)  // 4×1: current + upcoming hourly strip
private val LARGE  = DpSize(250.dp, 110.dp)  // 4×2: header + full hourly strip

private enum class TimeOfDay { MORNING, DAYTIME, NIGHT }

private fun currentTimeOfDay(): TimeOfDay {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..10  -> TimeOfDay.MORNING   // 5 AM – 10 AM: plan the day
        h in 11..17 -> TimeOfDay.DAYTIME   // 11 AM – 5 PM: current conditions
        else        -> TimeOfDay.NIGHT     // 6 PM – 4 AM: prep for tomorrow
    }
}

// ── Colors ────────────────────────────────────────────────────────────────────

private data class WColors(
    val bg: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
)

// Convert an Android ARGB color int to a Compose Color.
private fun Int.toComposeColor() = Color(
    alpha = (this ushr 24 and 0xFF) / 255f,
    red   = (this ushr 16 and 0xFF) / 255f,
    green = (this ushr  8 and 0xFF) / 255f,
    blue  = (this          and 0xFF) / 255f,
)

private fun ctx2color(context: Context, @ColorRes id: Int) =
    ContextCompat.getColor(context, id).toComposeColor()

// Resolve widget colors outside the composable so there's no theme dependency.
// On API 31+, reads system dynamic accent colors derived from the user's wallpaper
// (Material You). Reads dark mode state from the context so the right palette
// shade is selected at each widget update.
private fun resolveWidgetColors(context: Context): WColors {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // system_accent1 palette: 100 = near-white tint, 900 = near-black tint.
        // Light mode: light container (100) + dark text (900).
        // Dark mode: dark container (700) + light text (100).
        val isDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bg   = if (isDark) ctx2color(context, android.R.color.system_accent1_700)
                   else        ctx2color(context, android.R.color.system_accent1_100)
        val text = if (isDark) ctx2color(context, android.R.color.system_accent1_100)
                   else        ctx2color(context, android.R.color.system_accent1_900)
        return WColors(
            bg            = ColorProvider(bg),
            textPrimary   = ColorProvider(text),
            textSecondary = ColorProvider(text.copy(alpha = 0.65f)),
        )
    }
    // Pre-API 31: static dusty-blue palette matching the app's primary color.
    return WColors(
        bg            = ColorProvider(Color(0xFF6B86A3)),
        textPrimary   = ColorProvider(Color.White),
        textSecondary = ColorProvider(Color.White.copy(alpha = 0.75f)),
    )
}

// ── Widget ────────────────────────────────────────────────────────────────────

class WeatherWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data   = loadWeather(context)
        val colors = resolveWidgetColors(context)
        provideContent { WidgetContent(data, colors) }
    }

    private suspend fun loadWeather(context: Context): WeatherData? {
        return try {
            val prefs = PreferencesStore(context)
            val units = prefs.getUnitSystem()
            val selected = prefs.getSelected()
            val repo = WeatherRepository(context)
            val fresh = if (selected != null) {
                repo.getWeather(selected.lat, selected.lon, units, placeName = selected.name).getOrNull()
            } else {
                val latLon = LocationProvider(context).currentLatLon()
                if (latLon != null) repo.getWeather(latLon.first, latLon.second, units).getOrNull()
                else null
            }
            // Fall back to the app's last cached forecast so the widget always
            // shows real data once the user has opened the app at least once.
            fresh ?: ForecastCache(context).load()?.first
        } catch (e: Exception) {
            ForecastCache(context).load()?.first
        }
    }
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(data: WeatherData?, c: WColors) {
    val time = currentTimeOfDay()
    val open = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

    val base = GlanceModifier
        .fillMaxSize()
        .cornerRadius(20.dp)
        .background(c.bg)
        .clickable(open)

    when (LocalSize.current) {
        LARGE  -> LargeWidget(base.padding(12.dp), data, time, c)
        WIDE   -> WideWidget(base.padding(horizontal = 12.dp, vertical = 6.dp), data, c)
        MEDIUM -> MediumWidget(base.padding(12.dp), data, time, c)
        else   -> SmallWidget(base.padding(8.dp), data, c)
    }
}

// ── Small (2×1): temp + emoji only ───────────────────────────────────────────

@Composable
private fun SmallWidget(mod: GlanceModifier, data: WeatherData?, c: WColors) {
    Column(
        modifier = mod,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (data == null) {
            Text("—", style = TextStyle(color = c.textPrimary, fontSize = 18.sp))
        } else {
            Text(
                "${weatherEmoji(data.currentIcon, data.isDay)} ${data.currentTempC}°",
                style = TextStyle(color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

// ── Medium (2×2): chrono-dynamic stack ───────────────────────────────────────

@Composable
private fun MediumWidget(mod: GlanceModifier, data: WeatherData?, time: TimeOfDay, c: WColors) {
    Column(modifier = mod) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            Text(
                data.locationName,
                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(6.dp))
            when (time) {
                TimeOfDay.MORNING -> MorningFocus(data, c)
                TimeOfDay.DAYTIME -> DaytimeFocus(data, c)
                TimeOfDay.NIGHT   -> NightFocus(data, c)
            }
        }
    }
}

// Morning: today's high + rain chance — plan what to wear
@Composable
private fun MorningFocus(data: WeatherData, c: WColors) {
    Text(
        "${weatherEmoji(data.currentIcon, data.isDay)} ${data.condition}",
        style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
        maxLines = 1,
    )
    Spacer(GlanceModifier.height(4.dp))
    Text(
        "High ${data.highTodayC}°",
        style = TextStyle(color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
    )
    val rain = data.daily.firstOrNull()?.precipProbMax ?: 0
    if (rain > 0) {
        Spacer(GlanceModifier.height(4.dp))
        Text(
            "💧 $rain% chance of rain",
            style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
        )
    }
}

// Daytime: current temperature is the hero
@Composable
private fun DaytimeFocus(data: WeatherData, c: WColors) {
    Text(
        "${weatherEmoji(data.currentIcon, data.isDay)} ${data.currentTempC}°",
        style = TextStyle(color = c.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold),
    )
    Spacer(GlanceModifier.height(2.dp))
    Text(
        data.condition,
        style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
        maxLines = 1,
    )
    Spacer(GlanceModifier.height(4.dp))
    Text(
        "H:${data.highTodayC}°  L:${data.lowTodayC}°",
        style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
    )
}

// Night: tomorrow's forecast — wake up informed
@Composable
private fun NightFocus(data: WeatherData, c: WColors) {
    Text("Tomorrow", style = TextStyle(color = c.textSecondary, fontSize = 11.sp))
    Spacer(GlanceModifier.height(6.dp))
    val tomorrow = data.daily.getOrNull(1)
    if (tomorrow != null) {
        Text(
            "${weatherEmoji(tomorrow.icon, true)} ${wmoText(tomorrow.icon)}",
            style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            "H:${tomorrow.highC}°  L:${tomorrow.lowC}°",
            style = TextStyle(color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
        )
    }
}

// ── Wide (4×1): current + compact hourly text ─────────────────────────────────

@Composable
private fun WideWidget(mod: GlanceModifier, data: WeatherData?, c: WColors) {
    Column(
        modifier = mod,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
            )
        } else {
            Text(
                "${weatherEmoji(data.currentIcon, data.isDay)} ${data.currentTempC}°  ·  ${data.locationName}",
                style = TextStyle(color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            val upcoming = data.hourly.drop(1).take(4)
            if (upcoming.isNotEmpty()) {
                Text(
                    upcoming.joinToString("   ") {
                        "${it.hourLabel} ${weatherEmoji(it.icon, it.isDay)} ${it.tempC}°"
                    },
                    style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Large (4×2): chrono header + hourly strip ─────────────────────────────────

@Composable
private fun LargeWidget(mod: GlanceModifier, data: WeatherData?, time: TimeOfDay, c: WColors) {
    Column(modifier = mod) {
        if (data == null) {
            Text(
                "Open SkySpeak to set up",
                style = TextStyle(color = c.textSecondary, fontSize = 12.sp),
            )
        } else {
            LargeHeader(data, time, c)
            Spacer(GlanceModifier.height(10.dp))
            HourlyStrip(data.hourly.take(5), c)
        }
    }
}

@Composable
private fun LargeHeader(data: WeatherData, time: TimeOfDay, c: WColors) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Left: location + current condition (always shown)
        Column(modifier = GlanceModifier.width(116.dp)) {
            Text(
                data.locationName,
                style = TextStyle(color = c.textSecondary, fontSize = 10.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(3.dp))
            Text(
                "${weatherEmoji(data.currentIcon, data.isDay)} ${data.condition}",
                style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        // Right: chrono-dynamic key metric
        Column {
            when (time) {
                TimeOfDay.MORNING -> {
                    Text(
                        "High ${data.highTodayC}°",
                        style = TextStyle(color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    )
                    val rain = data.daily.firstOrNull()?.precipProbMax ?: 0
                    if (rain > 0) {
                        Text(
                            "💧 $rain% rain",
                            style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
                        )
                    }
                }
                TimeOfDay.DAYTIME -> {
                    Text(
                        "${data.currentTempC}°",
                        style = TextStyle(color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "H:${data.highTodayC}°  L:${data.lowTodayC}°",
                        style = TextStyle(color = c.textSecondary, fontSize = 11.sp),
                    )
                }
                TimeOfDay.NIGHT -> {
                    val tomorrow = data.daily.getOrNull(1)
                    Text("Tomorrow", style = TextStyle(color = c.textSecondary, fontSize = 10.sp))
                    if (tomorrow != null) {
                        Text(
                            "H:${tomorrow.highC}°  L:${tomorrow.lowC}°",
                            style = TextStyle(color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

// ── Shared hourly strip ───────────────────────────────────────────────────────

@Composable
private fun HourlyStrip(hours: List<HourEntry>, c: WColors) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        hours.take(5).forEach { entry ->
            Column(
                modifier = GlanceModifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    entry.hourLabel,
                    style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    weatherEmoji(entry.icon, entry.isDay),
                    style = TextStyle(color = c.textPrimary, fontSize = 12.sp),
                )
                Text(
                    "${entry.tempC}°",
                    style = TextStyle(color = c.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
                val precip = entry.precipChance
                if (precip != null && precip >= 20) {
                    Text(
                        "$precip%",
                        style = TextStyle(color = c.textSecondary, fontSize = 9.sp),
                    )
                }
            }
        }
    }
}

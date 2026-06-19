package com.example.weatherly.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.weatherly.MainActivity
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.WeatherRepository
import com.example.weatherly.location.LocationProvider
import com.example.weatherly.util.weatherEmoji

/**
 * Home-screen widget built with Jetpack Glance. It fetches the current weather
 * (location + Open-Meteo) when the system asks it to update, then renders a
 * compact summary. Tapping it opens the full app.
 */
class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWeather(context)
        provideContent {
            WidgetContent(data)
        }
    }

    private suspend fun loadWeather(context: Context): WeatherData? = try {
        val prefs = PreferencesStore(context)
        val units = prefs.getUnitSystem()
        val selected = prefs.getSelected()
        val repo = WeatherRepository(context)
        if (selected != null) {
            repo.getWeather(selected.lat, selected.lon, units, placeName = selected.name).getOrNull()
        } else {
            val latLon = LocationProvider(context).currentLatLon()
            if (latLon == null) null
            else repo.getWeather(latLon.first, latLon.second, units).getOrNull()
        }
    } catch (e: Exception) {
        null
    }
}

private val White = ColorProvider(Color.White)
private val White80 = ColorProvider(Color.White.copy(alpha = 0.8f))

@Composable
private fun WidgetContent(data: WeatherData?) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(Color(0xFF6B86A3))
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (data == null) {
            Text(
                "Open Weatherly to set up",
                style = TextStyle(color = White80, fontSize = 14.sp)
            )
        } else {
            Text(
                data.locationName,
                style = TextStyle(color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                weatherEmoji(data.currentIcon, data.isDay) + "  ${data.currentTempC}°",
                style = TextStyle(color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                data.condition,
                style = TextStyle(color = White80, fontSize = 13.sp)
            )
            Text(
                "H:${data.highTodayC}°   L:${data.lowTodayC}°",
                style = TextStyle(color = White80, fontSize = 12.sp)
            )
        }
    }
}

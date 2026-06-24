package com.example.weatherly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.MetricChart
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip

// --- Colours resolved from the active Material 3 colour scheme ---
val AppBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// --- Muted pastel accent family (names kept; values softened) ---
val Cyan = Color(0xFF6B86A3)    // dusty blue — the primary accent
val Amber = Color(0xFFC8A86A)   // muted sand
val Coral = Color(0xFFC18A92)   // muted rose
val Green = Color(0xFF8AA68C)   // muted sage
val Indigo = Color(0xFF6B86A3)  // dusty blue
val Purple = Color(0xFF9C8AA8)  // muted mauve
val Orange = Color(0xFFC0876B)  // muted clay
val Teal = Color(0xFF6FA39B)    // muted teal

private fun tempColor(c: Int): Color = when {
    c <= 0 -> Color(0xFF7FA8C9)
    c <= 8 -> Color(0xFF8FB6C2)
    c <= 15 -> Color(0xFF93B58C)
    c <= 22 -> Color(0xFFCBB36B)
    c <= 28 -> Color(0xFFCD9A6B)
    else -> Color(0xFFC58587)
}

/**
 * Returns a two-stop vertical gradient: a sky tone for the weather condition
 * at top, fading to the app background at the bottom so the hero section
 * blends seamlessly into the card area below.
 */
@Composable
fun conditionGradient(code: Int, isDay: Boolean): List<Color> {
    val isDark = isSystemInDarkTheme()
    val base = MaterialTheme.colorScheme.background
    val sky = when {
        code in 95..99 ->
            if (isDark) Color(0xFF1C1230) else Color(0xFF3A2F50)
        code in 71..86 ->
            if (isDark) Color(0xFF1A2230) else Color(0xFFD0E0EE)
        code in 51..82 ->
            if (isDark) Color(0xFF0E1C2A) else Color(0xFFBFD4E6)
        code in 45..48 ->
            if (isDark) Color(0xFF18202A) else Color(0xFFCDD3D8)
        !isDay ->
            if (isDark) Color(0xFF04091A) else Color(0xFF1A2448)
        code <= 2 ->
            if (isDark) Color(0xFF102030) else Color(0xFFB8D8F0)
        else ->
            if (isDark) Color(0xFF141C24) else Color(0xFFC8D2DC)
    }
    return listOf(sky, base)
}

// Soft tinted pill — light pastels in light mode, dark tints with contrasting text in dark mode.
@Composable
private fun tipColors(tone: TipTone): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return when (tone) {
        TipTone.HOT ->
            if (isDark) Color(0xFF2C1A06) to Color(0xFFE8BE7A)
            else Color(0xFFEFE4D0) to Color(0xFF6E5C3C)
        TipTone.RAIN ->
            if (isDark) Color(0xFF0D1E2E) to Color(0xFF7FA8C9)
            else Color(0xFFDCE6EF) to Color(0xFF3F5670)
        TipTone.SNOW ->
            if (isDark) Color(0xFF0F1F2C) to Color(0xFF8AB8CF)
            else Color(0xFFE2ECF1) to Color(0xFF3F5670)
        TipTone.COLD ->
            if (isDark) Color(0xFF1A1828) to Color(0xFF9C96BE)
            else Color(0xFFE4E2EF) to Color(0xFF4C4A66)
        TipTone.WIND ->
            if (isDark) Color(0xFF0E1F1C) to Color(0xFF7AB8A8)
            else Color(0xFFDDEAE6) to Color(0xFF3E5A52)
        TipTone.NICE ->
            if (isDark) Color(0xFF0F1E10) to Color(0xFF8AB88E)
            else Color(0xFFE3EBDD) to Color(0xFF4C5A40)
        TipTone.NEUTRAL ->
            if (isDark) Color(0xFF1A1810) to TextPrimary
            else Color(0xFFEAE6DE) to TextPrimary
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fill: Color = Color.Unspecified,
    corner: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface
    // Light mode: soft 1dp shadow keeps cards airy; dark mode: 6dp lifts them off the background.
    val elevation = if (isDark) 6.dp else 1.dp
    val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.07f)
    val actualFill = if (fill == Color.Unspecified) surface else fill
    val shape = RoundedCornerShape(corner)
    var m = modifier
        .shadow(elevation = elevation, shape = shape, clip = false)
        .clip(shape)
        .background(actualFill)
        .border(1.dp, stroke, shape)
    if (onClick != null) m = m.clickable { onClick() }
    m = m.padding(16.dp)
    Box(m) { content() }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun CurrentHeader(
    data: WeatherData,
    textColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location — small caps, light weight, recedes so temperature can lead
        Text(
            data.locationName.uppercase(),
            color = subColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        // Condition + glyph on the same line — supporting context, not the hero
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherGlyph(code = data.currentIcon, isDay = data.isDay, size = 20.dp)
            Spacer(Modifier.width(6.dp))
            Text(data.condition, color = subColor, fontSize = 15.sp, fontWeight = FontWeight.Normal)
        }
        // Temperature — the undisputed hero
        Text("${data.currentTempC}°", color = textColor, fontSize = 96.sp, fontWeight = FontWeight.Thin)
        // H/L and feels-like as compact secondary info
        Text(
            "H:${data.highTodayC}°  ·  L:${data.lowTodayC}°",
            color = subColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
        data.realFeelC?.let {
            Text("Feels like $it°", color = subColor, fontSize = 13.sp, fontWeight = FontWeight.Normal)
        }
        data.comparedToYesterday?.let {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(textColor.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(it, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
fun TipBanner(tip: WeatherTip, modifier: Modifier = Modifier) {
    val (_, fg) = tipColors(tip.tone)
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(fg.copy(alpha = if (isDark) 0.08f else 0.10f))
            .drawBehind { drawRect(color = fg, size = Size(4.dp.toPx(), size.height)) }
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tipIcon(tip.tone), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(tip.text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

private fun tipIcon(tone: TipTone): ImageVector = when (tone) {
    TipTone.HOT -> Icons.Filled.WbSunny
    TipTone.RAIN -> Icons.Filled.WaterDrop
    TipTone.SNOW -> Icons.Filled.AcUnit
    TipTone.COLD -> Icons.Filled.AcUnit
    TipTone.WIND -> Icons.Filled.Air
    TipTone.NICE -> Icons.Filled.WbSunny
    TipTone.NEUTRAL -> Icons.Filled.Info
}

@Composable
fun HourlyCard(data: WeatherData, modifier: Modifier = Modifier) {
    val hourlyTemps = data.hourly.map { it.tempC }
    val high = hourlyTemps.maxOrNull() ?: data.highTodayC
    val low = hourlyTemps.minOrNull() ?: data.lowTodayC
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(Icons.Filled.Schedule, "Next ${data.hourly.size} hours", Amber)
                Text("H:$high°  L:$low°", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(data.hourly) { h ->
                    val isNow = h.hourLabel == "Now"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            h.hourLabel,
                            color = if (isNow) TextPrimary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(8.dp))
                        WeatherGlyph(code = h.icon, size = 26.dp)
                        h.precipChance?.takeIf { it > 0 }?.let {
                            Text("$it%", color = Indigo, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${h.tempC}°",
                            color = if (isNow) TextPrimary else TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/** Hourly bar chart shown inside a metric's detail popup. */
@Composable
private fun MetricBarChart(chart: MetricChart, accent: Color) {
    val values = chart.values
    if (values.size < 2) return
    val mn = values.min()
    val mx = values.max()
    val unitSuffix = if (chart.unit.isBlank()) "" else " ${chart.unit}"

    Text("Throughout the day", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val rng = (mx - mn).coerceAtLeast(0.1f)
        val n = values.size
        val gap = 3.dp.toPx()
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        values.forEachIndexed { i, v ->
            val norm = (v - mn) / rng
            val barH = size.height * (0.14f + 0.82f * norm)
            val x = i * (barW + gap)
            drawRoundRect(
                color = if (i == 0) accent else accent.copy(alpha = 0.40f),
                topLeft = Offset(x, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = radius
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    val n = chart.labels.size
    val idx = listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).distinct().filter { it in chart.labels.indices }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        idx.forEach { Text(chart.labels[it], color = TextSecondary, fontSize = 11.sp) }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Now ${values.first().toInt()}$unitSuffix   ·   High ${mx.toInt()}$unitSuffix   ·   Low ${mn.toInt()}$unitSuffix",
        color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium
    )
}

@Composable
fun DailyCard(
    data: WeatherData,
    onDayClick: (DayEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            SectionLabel(Icons.Filled.CalendarMonth, "${data.daily.size}-day forecast", Green)
            Spacer(Modifier.height(8.dp))
            data.daily.forEachIndexed { index, day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDayClick(day) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.dayLabel, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(52.dp))
                    Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                        WeatherGlyph(code = day.icon, size = 24.dp)
                    }
                    Text(
                        "${day.lowC}°", color = TextSecondary, fontSize = 15.sp,
                        modifier = Modifier.width(34.dp), textAlign = TextAlign.End
                    )
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        TempRangeBar(
                            low = day.lowC, high = day.highC,
                            weekMin = data.weekMinC, weekMax = data.weekMaxC,
                            currentC = if (index == 0) data.currentTempC else null
                        )
                    }
                    Text(
                        "${day.highC}°", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(34.dp), textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun TempRangeBar(low: Int, high: Int, weekMin: Int, weekMax: Int, currentC: Int?) {
    val dotColor = TextPrimary
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(h / 2f, h / 2f)
        val range = (weekMax - weekMin).coerceAtLeast(1).toFloat()
        drawRoundRect(color = Color(0x1A12263A), size = Size(w, h), cornerRadius = r)
        val x1 = ((low - weekMin) / range) * w
        val x2 = ((high - weekMin) / range) * w
        val segW = (x2 - x1).coerceAtLeast(h)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(tempColor(low), tempColor(high)), startX = x1, endX = x1 + segW
            ),
            topLeft = Offset(x1, 0f),
            size = Size(segW, h),
            cornerRadius = r
        )
        if (currentC != null) {
            val cx = (((currentC - weekMin) / range) * w).coerceIn(h, w - h)
            drawCircle(color = dotColor, radius = h * 0.95f, center = Offset(cx, h / 2f))
        }
    }
}

@Composable
fun MetricTile(data: MetricTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier.height(118.dp), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            SectionLabel(data.icon, data.label, data.accent)
            Column {
                Text(data.value, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                data.sub?.let { Text(it, color = TextSecondary, fontSize = 13.sp) }
            }
        }
    }
}

data class MetricTileData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val sub: String?,
    val accent: Color,
    val description: String,
    val chart: MetricChart? = null
)

sealed interface DetailSheet {
    data class Metric(
        val icon: ImageVector,
        val accent: Color,
        val title: String,
        val value: String,
        val description: String,
        val chart: MetricChart? = null
    ) : DetailSheet

    data class Day(val day: DayEntry, val windUnit: String, val precipUnit: String) : DetailSheet
}

@Composable
fun MetricsGrid(
    data: WeatherData,
    onMetricClick: (DetailSheet.Metric) -> Unit,
    modifier: Modifier = Modifier
) {
    val tiles = buildMetricTiles(data)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { tile ->
                    MetricTile(
                        data = tile,
                        onClick = {
                            onMetricClick(
                                DetailSheet.Metric(tile.icon, tile.accent, tile.label, tile.value, tile.description, tile.chart)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun buildMetricTiles(d: WeatherData): List<MetricTileData> = buildList {
    fun chart(values: List<Int>, unit: String): MetricChart? =
        if (values.size >= 2) MetricChart(d.hourLabels, values.map { it.toFloat() }, unit) else null

    val uvAdvice = when (d.uvLabel) {
        "Low" -> "Minimal risk — no protection needed."
        "Moderate" -> "Wear sunscreen and a hat on bright days."
        "High" -> "Protection essential. Use SPF 30+ and seek shade midday."
        "Very High" -> "Take extra care. Avoid the sun between 10am and 4pm."
        "Extreme" -> "Avoid sun exposure midday; take all precautions."
        else -> "Sun strength for today."
    }
    add(MetricTileData(Icons.Filled.WbSunny, "UV Index", d.uvIndex?.toString() ?: "--", d.uvLabel, Amber,
        "The UV index measures the strength of the sun's ultraviolet radiation. " +
            "Current level: ${d.uvLabel ?: "unknown"}. $uvAdvice",
        chart(d.hourlyUv, "")))

    val aqiAdvice = when {
        d.aqi == null -> "Air quality data isn't available right now."
        d.aqi <= 50 -> "Air quality is good — a great time to be outdoors."
        d.aqi <= 100 -> "Acceptable air quality. Unusually sensitive people may want to limit long, intense outdoor activity."
        d.aqi <= 150 -> "Sensitive groups (children, older adults, and people with heart or lung conditions) should limit prolonged outdoor exertion."
        d.aqi <= 200 -> "Everyone may start to notice effects; sensitive groups should avoid prolonged outdoor exertion."
        d.aqi <= 300 -> "Health alert — everyone may experience more serious effects. Limit time outdoors."
        else -> "Hazardous air. Avoid outdoor activity and keep windows closed."
    }
    add(MetricTileData(Icons.Filled.Eco, "Air Quality", d.aqi?.toString() ?: "--", d.aqiLabel, aqiColor(d.aqi),
        "The US Air Quality Index (AQI) summarises pollutants like fine particles and ozone on a 0–500+ scale " +
            "(lower is better). Current reading: ${d.aqiLabel ?: "unknown"}. $aqiAdvice",
        if (d.aqi != null) chart(d.hourlyAqi, "") else null))

    add(MetricTileData(Icons.Filled.WbTwilight, "Sunrise", d.sunrise ?: "--", d.sunset?.let { "Sunset $it" }, Orange,
        "The sun rises at ${d.sunrise ?: "--"} and sets at ${d.sunset ?: "--"} today."))
    add(MetricTileData(Icons.Filled.Air, "Wind", d.windKmh?.let { "$it ${d.windUnit}" } ?: "--",
        listOfNotNull(d.windDir, d.windGustKmh?.let { "gusts $it" }).joinToString(" · ").ifBlank { null }, Teal,
        "Wind is blowing" + (d.windDir?.let { " from the $it" } ?: "") +
            " at ${d.windKmh ?: 0} ${d.windUnit}" + (d.windGustKmh?.let { ", with gusts up to $it ${d.windUnit}." } ?: "."),
        chart(d.hourlyWind, d.windUnit)))
    add(MetricTileData(Icons.Filled.Thermostat, "Feels like", d.realFeelC?.let { "$it°" } ?: "--", "Apparent temp", Coral,
        "Apparent temperature combines the air temperature with humidity and wind. " +
            "Right now it feels like ${d.realFeelC ?: d.currentTempC}°, while the actual air temperature is ${d.currentTempC}°.",
        chart(d.hourlyFeels, "°")))
    add(MetricTileData(Icons.Filled.WaterDrop, "Humidity", d.humidity?.let { "$it%" } ?: "--",
        d.cloudCoverPct?.let { "Cloud $it%" }, Cyan,
        "Relative humidity is ${d.humidity ?: 0}%" + (d.cloudCoverPct?.let { ", with $it% cloud cover" } ?: "") +
            ". Higher humidity makes warm air feel hotter and cold air feel colder.",
        chart(d.hourlyHumidity, "%")))
    add(MetricTileData(Icons.Filled.Visibility, "Visibility", d.visibility?.let { "$it ${d.visibilityUnit}" } ?: "--", null, Indigo,
        "You can currently see clearly for about ${d.visibility ?: 0} ${d.visibilityUnit}.",
        chart(d.hourlyVisibility, d.visibilityUnit)))
    add(MetricTileData(Icons.Filled.Speed, "Pressure", d.pressureHpa?.toString() ?: "--", "hPa", Purple,
        "Atmospheric pressure is ${d.pressureHpa ?: 0} hPa. Around 1013 hPa is average; " +
            "falling pressure often signals incoming unsettled weather, while rising pressure means clearing.",
        chart(d.hourlyPressure, "hPa")))
    add(MetricTileData(Icons.Filled.Grain, "Precipitation", d.precipMm?.let { "$it ${d.precipUnit}" } ?: "0 ${d.precipUnit}", "In last hour", Cyan,
        "${d.precipMm ?: 0.0} ${d.precipUnit} of precipitation fell in the last hour. " +
            "The chart shows the chance of precipitation over the coming hours.",
        chart(d.hourlyPrecipProb, "%")))
}

/** US AQI band → colour (green → maroon). */
private fun aqiColor(aqi: Int?): Color = when {
    aqi == null -> Indigo
    aqi <= 50 -> Color(0xFF2ECC71)
    aqi <= 100 -> Color(0xFFE3B505)
    aqi <= 150 -> Color(0xFFFF8A3D)
    aqi <= 200 -> Color(0xFFFF5A5F)
    aqi <= 300 -> Color(0xFF8B5CF6)
    else -> Color(0xFF8E3B46)
}

@Composable
fun DetailSheetContent(sheet: DetailSheet) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
        when (sheet) {
            is DetailSheet.Metric -> {
                SectionLabel(sheet.icon, sheet.title, sheet.accent)
                Spacer(Modifier.height(14.dp))
                Text(sheet.value, color = sheet.accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(sheet.description, color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                sheet.chart?.let { chart ->
                    Spacer(Modifier.height(22.dp))
                    MetricBarChart(chart, sheet.accent)
                }
            }
            is DetailSheet.Day -> {
                val day = sheet.day
                Text(day.fullDateLabel, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherGlyph(code = day.icon, isDay = true, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(day.phrase ?: "", color = TextPrimary, fontSize = 18.sp)
                        Text("H:${day.highC}°   L:${day.lowC}°", color = TextSecondary, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                DetailRow("Sunrise", day.sunrise ?: "--")
                DetailRow("Sunset", day.sunset ?: "--")
                DetailRow("Max UV index", day.uvMax?.toString() ?: "--")
                DetailRow("Chance of rain", day.precipProbMax?.let { "$it%" } ?: "--")
                DetailRow("Precipitation", day.precipSumMm?.let { "$it ${sheet.precipUnit}" } ?: "0 ${sheet.precipUnit}")
                DetailRow("Max wind", day.windMaxKmh?.let { "$it ${sheet.windUnit}" } ?: "--")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 15.sp)
        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AttributionFooter(textColor: Color, modifier: Modifier = Modifier) {
    Text(
        "Weather data by Open-Meteo.com (CC BY 4.0)",
        color = textColor.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)
    )
}

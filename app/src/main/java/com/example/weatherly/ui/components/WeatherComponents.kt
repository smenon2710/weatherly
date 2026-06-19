package com.example.weatherly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

// --- Minimalist surfaces: warm off-white background, white cards, calm text ---
val AppBackground = Color(0xFFF4F1EB)   // warm off-white
val TextPrimary = Color(0xFF2B2F36)     // soft dark slate
val TextSecondary = Color(0xFF828A93)   // muted grey
private val CardFill = Color.White
private val CardStroke = Color(0x0F2B2F36)

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

// Soft tinted pill + a darker readable text colour for each tone.
private fun tipColors(tone: TipTone): Pair<Color, Color> = when (tone) {
    TipTone.HOT -> Color(0xFFEFE4D0) to Color(0xFF6E5C3C)
    TipTone.RAIN -> Color(0xFFDCE6EF) to Color(0xFF3F5670)
    TipTone.SNOW -> Color(0xFFE2ECF1) to Color(0xFF3F5670)
    TipTone.COLD -> Color(0xFFE4E2EF) to Color(0xFF4C4A66)
    TipTone.WIND -> Color(0xFFDDEAE6) to Color(0xFF3E5A52)
    TipTone.NICE -> Color(0xFFE3EBDD) to Color(0xFF4C5A40)
    TipTone.NEUTRAL -> Color(0xFFEAE6DE) to TextPrimary
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fill: Color = CardFill,
    corner: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    var m = modifier
        .shadow(elevation = 2.dp, shape = shape, clip = false)
        .clip(shape)
        .background(fill)
        .border(1.dp, CardStroke, shape)
    if (onClick != null) m = m.clickable { onClick() }
    m = m.padding(16.dp)
    Box(m) { content() }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text.uppercase(), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
        Text(data.locationName, color = textColor, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        WeatherGlyph(code = data.currentIcon, isDay = data.isDay, size = 76.dp)
        Spacer(Modifier.height(2.dp))
        Text("${data.currentTempC}°", color = textColor, fontSize = 92.sp, fontWeight = FontWeight.Thin)
        data.realFeelC?.let {
            Text("Feels like $it°", color = subColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(data.condition, color = subColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text("H:${data.highTodayC}°   L:${data.lowTodayC}°", color = subColor, fontSize = 16.sp)
        data.comparedToYesterday?.let {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(textColor.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(it, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TipBanner(tip: WeatherTip, modifier: Modifier = Modifier) {
    val (bg, fg) = tipColors(tip.tone)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            SectionLabel(Icons.Filled.Schedule, "Hourly forecast", Amber)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(data.hourly) { h ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(h.hourLabel, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        WeatherGlyph(code = h.icon, size = 26.dp)
                        h.precipChance?.takeIf { it > 0 }?.let {
                            Text("$it%", color = Indigo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${h.tempC}°", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TemperatureChartCard(data: WeatherData, modifier: Modifier = Modifier) {
    val temps = data.hourly.map { it.tempC }
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            SectionLabel(Icons.Filled.ShowChart, "Temperature trend", Teal)
            if (temps.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Next ${data.hourly.size} hrs · High ${temps.max()}°  Low ${temps.min()}°",
                    color = TextSecondary, fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            TemperatureChart(data.hourly)
            if (data.hourly.size >= 2) {
                Spacer(Modifier.height(8.dp))
                val n = data.hourly.size
                val idx = listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).distinct()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    idx.forEach { i -> Text(data.hourly[i].hourLabel, color = TextSecondary, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun TemperatureChart(hours: List<com.example.weatherly.data.model.HourEntry>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val temps = hours.map { it.tempC }
        if (temps.size < 2) return@Canvas
        val mn = temps.min()
        val mx = temps.max()
        val rng = (mx - mn).coerceAtLeast(1).toFloat()
        val w = size.width
        val h = size.height
        val padTop = h * 0.18f
        val usable = h * 0.64f
        val n = temps.size
        fun px(i: Int) = i / (n - 1).toFloat() * w
        fun py(t: Int) = padTop + (1f - (t - mn) / rng) * usable

        val area = Path().apply {
            moveTo(px(0), h)
            temps.forEachIndexed { i, t -> lineTo(px(i), py(t)) }
            lineTo(px(n - 1), h)
            close()
        }
        val line = Path().apply {
            moveTo(px(0), py(temps[0]))
            for (i in 1 until n) lineTo(px(i), py(temps[i]))
        }
        drawPath(area, Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.16f), Color.Transparent)))
        drawPath(
            line,
            color = Cyan,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(Cyan, radius = 4.dp.toPx(), center = Offset(px(0), py(temps[0])))
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
            drawCircle(color = TextPrimary, radius = h * 0.95f, center = Offset(cx, h / 2f))
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

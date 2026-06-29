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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
        (data.headline ?: data.comparedToYesterday)?.let {
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
                        WeatherGlyph(code = h.icon, isDay = h.isDay, size = 26.dp)
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
                    Column(
                        modifier = Modifier.width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeatherGlyph(code = day.icon, size = 24.dp)
                        day.precipProbMax?.takeIf { it > 0 && day.icon in 45..99 }?.let {
                            Text(
                                "$it%",
                                color = Indigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    val chart: MetricChart? = null,
    val gaugeFraction: Float? = null,
)

/** Arc-gauge tile: UV, AQI, Humidity, Pressure, Visibility. */
@Composable
fun ArcGaugeTile(data: MetricTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val trackColor = TextSecondary.copy(alpha = 0.15f)
    val textColor = TextPrimary
    val textSecColor = TextSecondary
    GlassCard(modifier = modifier, onClick = onClick, corner = 22.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel(data.icon, data.label, data.accent)
            Spacer(Modifier.height(8.dp))
            val fraction = (data.gaugeFraction ?: 0f).coerceIn(0f, 1f)
            val accent = data.accent
            Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 10.dp.toPx()
                    val diameter = size.minDimension - sw
                    val tl = Offset(sw / 2f, sw / 2f)
                    val arcSz = Size(diameter, diameter)
                    // background track
                    drawArc(color = trackColor, startAngle = 150f, sweepAngle = 240f,
                        useCenter = false, topLeft = tl, size = arcSz,
                        style = Stroke(width = sw, cap = StrokeCap.Round))
                    // filled arc
                    if (fraction > 0.01f) {
                        drawArc(color = accent, startAngle = 150f, sweepAngle = 240f * fraction,
                            useCenter = false, topLeft = tl, size = arcSz,
                            style = Stroke(width = sw, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(data.value, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    data.sub?.let {
                        Text(it, color = textSecColor, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Full-width sparkline tile: Wind, Feels Like, Precipitation. */
@Composable
fun SparklineTile(data: MetricTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textSecColor = TextSecondary
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(data.icon, data.label, data.accent)
                Text(data.value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            data.sub?.let {
                Text(it, color = textSecColor, fontSize = 12.sp)
            }
            val chart = data.chart
            if (chart != null && chart.values.size >= 2) {
                Spacer(Modifier.height(10.dp))
                val accent = data.accent
                val fillTop = accent.copy(alpha = 0.22f)
                val fillBot = accent.copy(alpha = 0.03f)
                val dividerColor = textSecColor.copy(alpha = 0.30f)
                val dayChangeI = chart.dayChangeIndex
                Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    val values = chart.values
                    val n = values.size
                    val mn = values.min()
                    val mx = values.max()
                    val rng = (mx - mn).coerceAtLeast(1f)
                    val pad = 4.dp.toPx()

                    fun xAt(i: Int) = (i.toFloat() / (n - 1)) * size.width
                    fun yAt(v: Float) = size.height - pad - ((v - mn) / rng) * (size.height - 2 * pad)

                    val linePath = Path()
                    val fillPath = Path()
                    values.forEachIndexed { i, v ->
                        val x = xAt(i); val y = yAt(v)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(size.width, size.height)
                    fillPath.close()

                    drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillTop, fillBot)))
                    drawPath(linePath, color = accent,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(color = accent, radius = 3.5.dp.toPx(),
                        center = Offset(xAt(0), yAt(values.first())))

                    // Day-change marker
                    if (dayChangeI != null && dayChangeI in 1 until n) {
                        val x = xAt(dayChangeI)
                        drawLine(
                            color = dividerColor,
                            start = Offset(x, 0f), end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
                        )
                    }
                }
                // Label row — inject "Tomorrow" chip at the day-change position
                if (chart.labels.isNotEmpty()) {
                    val n = chart.labels.size
                    val idxs = listOf(0, n / 3, 2 * n / 3, n - 1).distinct().filter { it in chart.labels.indices }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        idxs.forEach { i ->
                            val isTomorrow = dayChangeI != null && i == idxs.firstOrNull { it >= dayChangeI }
                            Text(
                                if (isTomorrow) "tmrw" else chart.labels[i],
                                color = if (isTomorrow) accent.copy(alpha = 0.70f) else textSecColor,
                                fontSize = 10.sp,
                                fontWeight = if (isTomorrow) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sun & Moon tile — always relevant.
 * Day: shows sun position on arc between sunrise and sunset.
 * Night: shows moon position on arc between today's sunset and tomorrow's sunrise.
 * The `sub` field encodes "Sunset HH:MM|TomorrowRise HH:MM".
 */
@Composable
fun SunMoonTile(data: MetricTileData, isDay: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val parts = data.sub?.split("|")?.associate {
        val kv = it.trim().split(" ", limit = 2)
        kv.getOrElse(0) { "" } to kv.getOrElse(1) { "" }
    } ?: emptyMap()
    val sunsetTime = parts["Sunset"]
    val tomorrowRise = parts["TomorrowRise"]

    val accentCapture = data.accent
    val moonColor = Color(0xFF93A1B8)
    val trackColor = TextSecondary.copy(alpha = 0.15f)
    val textPrimary = TextPrimary
    val textSec = TextSecondary

    GlassCard(modifier = modifier, onClick = onClick, corner = 22.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel(data.icon, data.label, data.accent)
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.BottomCenter) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 2.5.dp.toPx()
                    val r = (size.width / 2f - sw).coerceAtMost(size.height - sw)
                    val cx = size.width / 2f
                    val cy = size.height
                    val arcTL = Offset(cx - r, cy - r)
                    val arcSz = Size(r * 2f, r * 2f)

                    drawArc(color = trackColor, startAngle = 180f, sweepAngle = 180f,
                        useCenter = false, topLeft = arcTL, size = arcSz,
                        style = Stroke(width = sw, cap = StrokeCap.Round))

                    val cal = Calendar.getInstance()
                    val nowH = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f

                    val frac: Float
                    val dotColor: Color
                    val dotGlow: Color
                    if (isDay) {
                        val riseH = parseTimeHour(data.value)
                        val setH = parseTimeHour(sunsetTime) ?: riseH?.plus(12f)
                        frac = if (riseH != null && setH != null && setH > riseH)
                            ((nowH - riseH) / (setH - riseH)).coerceIn(0f, 1f) else 0.5f
                        dotColor = accentCapture
                        dotGlow = accentCapture.copy(alpha = 0.25f)
                    } else {
                        // Night arc: left = sunset, right = next sunrise
                        val setH = parseTimeHour(sunsetTime) ?: 20f
                        var riseH = parseTimeHour(tomorrowRise) ?: 6f
                        // Normalize: tomorrow's sunrise is always after sunset
                        if (riseH <= setH) riseH += 24f
                        val nowNorm = if (nowH < setH) nowH + 24f else nowH
                        frac = ((nowNorm - setH) / (riseH - setH)).coerceIn(0f, 1f)
                        dotColor = moonColor
                        dotGlow = moonColor.copy(alpha = 0.25f)
                    }

                    // arc: frac=0 → left (180°), frac=1 → right (0°)
                    val angleDeg = 180.0 - frac * 180.0
                    val angleRad = angleDeg * (PI / 180.0)
                    val dotX = (cx + r * cos(angleRad)).toFloat()
                    val dotY = (cy - r * sin(angleRad)).toFloat()
                    drawCircle(color = dotGlow, radius = 9.dp.toPx(), center = Offset(dotX, dotY))
                    drawCircle(color = dotColor, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                }
            }

            Spacer(Modifier.height(8.dp))
            if (isDay) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("↑ sunrise", color = textSec, fontSize = 10.sp)
                        Text(data.value, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("↓ sunset", color = textSec, fontSize = 10.sp)
                        Text(sunsetTime ?: "--", color = textSec, fontSize = 14.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("↓ set", color = textSec, fontSize = 10.sp)
                        Text(sunsetTime ?: "--", color = textSec, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("↑ rise tmrw", color = textSec, fontSize = 10.sp)
                        Text(tomorrowRise ?: "--", color = textSec, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun parseTimeHour(time: String?): Float? {
    if (time == null || time == "--") return null
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toFloatOrNull() ?: return null
    val m = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
    return h + m / 60f
}

sealed interface DetailSheet {
    data class Metric(
        val icon: ImageVector,
        val accent: Color,
        val title: String,
        val value: String,
        val description: String,
        val chart: MetricChart? = null,
        // Extra context for specialized detail views
        val windDir: String? = null,
        val windGust: String? = null,
        val hourlyActualTemps: List<Int>? = null,
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
    val m = tiles.associateBy { it.label }

    fun click(t: MetricTileData) =
        onMetricClick(DetailSheet.Metric(t.icon, t.accent, t.label, t.value, t.description, t.chart))

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Arc gauges: UV + AQI
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            m["UV Index"]?.let { t -> ArcGaugeTile(t, { click(t) }, Modifier.weight(1f)) }
            m["Air Quality"]?.let { t -> ArcGaugeTile(t, { click(t) }, Modifier.weight(1f)) }
        }
        // Arc gauges: Humidity + Pressure
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            m["Humidity"]?.let { t -> ArcGaugeTile(t, { click(t) }, Modifier.weight(1f)) }
            m["Pressure"]?.let { t -> ArcGaugeTile(t, { click(t) }, Modifier.weight(1f)) }
        }
        // Sparklines: Wind, Feels Like, Precipitation — each with its own rich detail payload
        m["Wind"]?.let { t ->
            SparklineTile(t, onClick = {
                onMetricClick(DetailSheet.Metric(
                    t.icon, t.accent, t.label, t.value, t.description, t.chart,
                    windDir = data.windDir,
                    windGust = data.windGustKmh?.let { "$it ${data.windUnit}" },
                ))
            }, Modifier.fillMaxWidth())
        }
        m["Feels like"]?.let { t ->
            SparklineTile(t, onClick = {
                onMetricClick(DetailSheet.Metric(
                    t.icon, t.accent, t.label, t.value, t.description, t.chart,
                    hourlyActualTemps = data.hourly.map { it.tempC },
                ))
            }, Modifier.fillMaxWidth())
        }
        m["Precipitation"]?.let { t -> SparklineTile(t, { click(t) }, Modifier.fillMaxWidth()) }
        // Sun & Moon arc card + Visibility gauge
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            m["Sun & Moon"]?.let { t -> SunMoonTile(t, data.isDay, { click(t) }, Modifier.weight(1f)) }
            m["Visibility"]?.let { t -> ArcGaugeTile(t, { click(t) }, Modifier.weight(1f)) }
        }
    }
}

private fun buildMetricTiles(d: WeatherData): List<MetricTileData> = buildList {
    val dayChangeIdx = d.hourLabels.indexOfFirst { it == "12 AM" }.takeIf { it >= 0 }
    fun chart(values: List<Int>, unit: String): MetricChart? =
        if (values.size >= 2) MetricChart(d.hourLabels, values.map { it.toFloat() }, unit, dayChangeIdx) else null

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
        chart(d.hourlyUv, ""),
        gaugeFraction = d.uvIndex?.let { (it / 11f).coerceIn(0f, 1f) }))

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
        if (d.aqi != null) chart(d.hourlyAqi, "") else null,
        gaugeFraction = d.aqi?.let { (it / 200f).coerceIn(0f, 1f) }))

    val tomorrowSunrise = d.daily.getOrNull(1)?.sunrise
    // sub encodes: "Sunset HH:MM | TomorrowRise HH:MM" so SunMoonTile can parse both
    val sunMoonSub = listOfNotNull(
        d.sunset?.let { "Sunset $it" },
        tomorrowSunrise?.let { "TomorrowRise $it" }
    ).joinToString("|")
    add(MetricTileData(Icons.Filled.WbTwilight, "Sun & Moon", d.sunrise ?: "--", sunMoonSub.ifBlank { null }, Orange,
        "The sun rises at ${d.sunrise ?: "--"} and sets at ${d.sunset ?: "--"} today. Tomorrow's sunrise: ${tomorrowSunrise ?: "--"}."))
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
        chart(d.hourlyHumidity, "%"),
        gaugeFraction = d.humidity?.let { it / 100f }))
    add(MetricTileData(Icons.Filled.Visibility, "Visibility", d.visibility?.let { "$it ${d.visibilityUnit}" } ?: "--", null, Indigo,
        "You can currently see clearly for about ${d.visibility ?: 0} ${d.visibilityUnit}.",
        chart(d.hourlyVisibility, d.visibilityUnit),
        gaugeFraction = d.visibility?.let { v ->
            val max = if (d.visibilityUnit.contains("mi")) 10f else 16f
            (v / max).coerceIn(0f, 1f)
        }))
    add(MetricTileData(Icons.Filled.Speed, "Pressure", d.pressureHpa?.toString() ?: "--", "hPa", Purple,
        "Atmospheric pressure is ${d.pressureHpa ?: 0} hPa. Around 1013 hPa is average; " +
            "falling pressure often signals incoming unsettled weather, while rising pressure means clearing.",
        chart(d.hourlyPressure, "hPa"),
        gaugeFraction = d.pressureHpa?.let { ((it - 960) / 90f).coerceIn(0f, 1f) }))
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

// ── Helper functions ──────────────────────────────────────────────────────────

private fun windDirToAngle(dir: String?): Float = when (dir?.uppercase()?.trim()) {
    "N"   -> 0f;   "NNE" -> 22.5f; "NE"  -> 45f;   "ENE" -> 67.5f
    "E"   -> 90f;  "ESE" -> 112.5f;"SE"  -> 135f;  "SSE" -> 157.5f
    "S"   -> 180f; "SSW" -> 202.5f;"SW"  -> 225f;  "WSW" -> 247.5f
    "W"   -> 270f; "WNW" -> 292.5f;"NW"  -> 315f;  "NNW" -> 337.5f
    else  -> 0f
}

private fun windIntensityColor(speed: Float): Color = when {
    speed < 10 -> Color(0xFF5B9B78)
    speed < 20 -> Color(0xFF6B9BB0)
    speed < 40 -> Color(0xFFC8A86A)
    speed < 60 -> Color(0xFFCC7B40)
    else       -> Color(0xFFC05050)
}

private fun precipIntensityColor(prob: Float): Color = when {
    prob < 20 -> Color(0xFF5B9B78)
    prob < 40 -> Color(0xFF8AB870)
    prob < 60 -> Color(0xFFC8A86A)
    prob < 80 -> Color(0xFFCC7B40)
    else      -> Color(0xFFC05050)
}

// ── Default detail (bar chart) ────────────────────────────────────────────────

@Composable
private fun DefaultMetricContent(sheet: DetailSheet.Metric) {
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

// ── Wind: compass rose + intensity strip ──────────────────────────────────────

@Composable
private fun WindDetailContent(sheet: DetailSheet.Metric) {
    val accent = sheet.accent
    val textPrimary = TextPrimary
    val textSec = TextSecondary
    val ringColor = TextSecondary.copy(alpha = 0.18f)
    val tickColor = TextSecondary.copy(alpha = 0.55f)

    SectionLabel(sheet.icon, sheet.title, accent)
    Spacer(Modifier.height(12.dp))
    Text(sheet.description, color = textSec, fontSize = 15.sp, lineHeight = 22.sp)
    Spacer(Modifier.height(24.dp))

    // Speed + gust row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SPEED", color = textSec, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(4.dp))
            Text(sheet.value, color = accent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        if (!sheet.windGust.isNullOrBlank()) {
            Box(Modifier.width(1.dp).height(52.dp).background(textSec.copy(alpha = 0.18f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GUSTS", color = textSec, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(4.dp))
                Text(sheet.windGust, color = textPrimary, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(Modifier.height(28.dp))

    // Compass rose
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(164.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f - 2.dp.toPx()
                val innerR = outerR * 0.72f

                drawCircle(color = ringColor, radius = outerR, center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = ringColor.copy(alpha = 0.5f), radius = innerR, center = Offset(cx, cy),
                    style = Stroke(width = 0.75.dp.toPx()))

                for (i in 0 until 8) {
                    val rad = i * 45.0 * PI / 180.0
                    val s = sin(rad).toFloat(); val c = cos(rad).toFloat()
                    val isCardinal = i % 2 == 0
                    val tickIn = if (isCardinal) outerR * 0.82f else outerR * 0.89f
                    drawLine(
                        color = if (isCardinal) tickColor else tickColor.copy(alpha = 0.45f),
                        start = Offset(cx + tickIn * s, cy - tickIn * c),
                        end = Offset(cx + outerR * 0.96f * s, cy - outerR * 0.96f * c),
                        strokeWidth = if (isCardinal) 1.5.dp.toPx() else 0.75.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Needle — points toward the direction wind is coming FROM
                val angleDeg = windDirToAngle(sheet.windDir)
                val rad = angleDeg * PI / 180.0
                val nSin = sin(rad).toFloat(); val nCos = cos(rad).toFloat()
                val needleLen = innerR * 0.82f

                // Tail (opposite direction, faded)
                drawLine(
                    color = accent.copy(alpha = 0.30f),
                    start = Offset(cx, cy),
                    end = Offset(cx - nSin * needleLen * 0.32f, cy + nCos * needleLen * 0.32f),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                )
                // Main shaft
                drawLine(
                    color = accent,
                    start = Offset(cx, cy),
                    end = Offset(cx + nSin * needleLen, cy - nCos * needleLen),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                )
                // Center dot
                drawCircle(color = accent, radius = 5.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = ringColor, radius = 3.dp.toPx(), center = Offset(cx, cy))
            }
            // Cardinal labels
            Text("N", color = textSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp))
            Text("S", color = textSec, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
            Text("E", color = textSec, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
            Text("W", color = textSec, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp))
        }
    }

    // Hourly intensity strip
    sheet.chart?.let { chart ->
        Spacer(Modifier.height(24.dp))
        Text("THROUGHOUT THE DAY", color = textSec, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        val values = chart.values
        val mn = values.minOrNull() ?: 0f
        val mx = values.maxOrNull() ?: 1f
        val rng = (mx - mn).coerceAtLeast(1f)
        val dayChangeI = chart.dayChangeIndex
        val divColor = textSec.copy(alpha = 0.30f)
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val n = values.size
            val gap = 2.5.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            val r = CornerRadius(3.dp.toPx())
            values.forEachIndexed { i, v ->
                val barH = size.height * (0.18f + 0.80f * ((v - mn) / rng))
                drawRoundRect(
                    color = windIntensityColor(v),
                    topLeft = Offset(i * (barW + gap), size.height - barH),
                    size = Size(barW, barH), cornerRadius = r
                )
            }
            if (dayChangeI != null && dayChangeI in 1 until n) {
                val x = (dayChangeI * (barW + gap)) - gap / 2f
                drawLine(divColor, Offset(x, 0f), Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f)))
            }
        }
        val n = chart.labels.size
        val idxs = listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).distinct().filter { it in chart.labels.indices }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            idxs.forEach { i ->
                val isTmrw = dayChangeI != null && i == idxs.firstOrNull { it >= dayChangeI }
                Text(if (isTmrw) "tmrw" else chart.labels[i], color = textSec, fontSize = 11.sp,
                    fontWeight = if (isTmrw) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Spacer(Modifier.height(10.dp))
        // Speed legend
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Calm" to windIntensityColor(5f), "Breezy" to windIntensityColor(25f),
                   "Windy" to windIntensityColor(45f), "Strong" to windIntensityColor(65f))
                .forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(4.dp))
                        Text(label, color = textSec, fontSize = 10.sp)
                    }
                }
        }
    }
}

// ── Feels Like: dual-line apparent vs actual ──────────────────────────────────

@Composable
private fun FeelsLikeDetailContent(sheet: DetailSheet.Metric) {
    val accent = sheet.accent
    val textSec = TextSecondary
    val dashColor = TextSecondary.copy(alpha = 0.55f)
    val fillColor = accent.copy(alpha = 0.13f)

    SectionLabel(sheet.icon, sheet.title, accent)
    Spacer(Modifier.height(14.dp))
    Text(sheet.value, color = accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    Text(sheet.description, color = textSec, fontSize = 15.sp, lineHeight = 22.sp)

    val feelsVals = sheet.chart?.values
    val actualInts = sheet.hourlyActualTemps
    if (feelsVals != null && actualInts != null && feelsVals.size >= 2 && actualInts.size >= 2) {
        val n = minOf(feelsVals.size, actualInts.size)
        val actualVals = actualInts.map { it.toFloat() }

        // Comparison pill
        val diff = (feelsVals.firstOrNull()?.toInt() ?: 0) - (actualInts.firstOrNull() ?: 0)
        if (diff != 0) {
            Spacer(Modifier.height(12.dp))
            val msg = if (diff > 0) "Feels $diff° warmer than actual" else "Feels ${-diff}° cooler than actual"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) { Text(msg, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }

        Spacer(Modifier.height(22.dp))
        Text("APPARENT VS ACTUAL", color = textSec, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp, 2.dp).background(accent))
                Spacer(Modifier.width(6.dp))
                Text("Feels like", color = textSec, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(16.dp, 2.dp)) {
                    drawLine(
                        color = dashColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f))
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("Actual temp", color = textSec, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))

        val accentCapture = accent
        val dashCapture = dashColor
        val fillCapture = fillColor
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val allVals = feelsVals.take(n) + actualVals.take(n)
            val mn = allVals.min()
            val mx = allVals.max()
            val rng = (mx - mn).coerceAtLeast(1f)
            val pad = 10.dp.toPx()

            fun xAt(i: Int) = (i.toFloat() / (n - 1)) * size.width
            fun yAt(v: Float) = size.height - pad - ((v - mn) / rng) * (size.height - 2 * pad)

            // Gap fill between the two curves
            val gapPath = Path()
            feelsVals.take(n).forEachIndexed { i, v ->
                val x = xAt(i); val y = yAt(v)
                if (i == 0) gapPath.moveTo(x, y) else gapPath.lineTo(x, y)
            }
            for (i in (n - 1) downTo 0) {
                gapPath.lineTo(xAt(i), yAt(actualVals[i]))
            }
            gapPath.close()
            drawPath(gapPath, color = fillCapture)

            // Actual temp line (dashed)
            val actualPath = Path()
            actualVals.take(n).forEachIndexed { i, v ->
                val x = xAt(i); val y = yAt(v)
                if (i == 0) actualPath.moveTo(x, y) else actualPath.lineTo(x, y)
            }
            drawPath(actualPath, color = dashCapture,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))

            // Feels-like line (solid)
            val feelsPath = Path()
            feelsVals.take(n).forEachIndexed { i, v ->
                val x = xAt(i); val y = yAt(v)
                if (i == 0) feelsPath.moveTo(x, y) else feelsPath.lineTo(x, y)
            }
            drawPath(feelsPath, color = accentCapture,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Now dot
            drawCircle(color = accentCapture, radius = 4.dp.toPx(),
                center = Offset(xAt(0), yAt(feelsVals.first())))

            // Day-change marker
            val dayChangeI = sheet.chart?.dayChangeIndex
            if (dayChangeI != null && dayChangeI in 1 until n) {
                val x = xAt(dayChangeI)
                drawLine(dashCapture.copy(alpha = 0.40f), Offset(x, 0f), Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f)))
            }
        }

        // Time axis
        sheet.chart?.let { chart ->
            val labels = chart.labels
            val dayChangeI = chart.dayChangeIndex
            val ln = labels.size
            val idxs = listOf(0, ln / 4, ln / 2, 3 * ln / 4, ln - 1).distinct().filter { it in labels.indices }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                idxs.forEach { i ->
                    val isTmrw = dayChangeI != null && i == idxs.firstOrNull { it >= dayChangeI }
                    Text(if (isTmrw) "tmrw" else labels[i], color = textSec, fontSize = 11.sp,
                        fontWeight = if (isTmrw) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

// ── Precipitation: colored intensity blocks ───────────────────────────────────

@Composable
private fun PrecipDetailContent(sheet: DetailSheet.Metric) {
    val accent = sheet.accent
    val textSec = TextSecondary
    val annotCapture = TextSecondary.copy(alpha = 0.50f)

    SectionLabel(sheet.icon, sheet.title, accent)
    Spacer(Modifier.height(14.dp))
    Text(sheet.value, color = accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    Text(sheet.description, color = textSec, fontSize = 15.sp, lineHeight = 22.sp)

    sheet.chart?.let { chart ->
        Spacer(Modifier.height(22.dp))
        Text("HOURLY RAIN CHANCE", color = textSec, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(8.dp))

        // Intensity legend
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Low" to precipIntensityColor(10f), "Moderate" to precipIntensityColor(30f),
                   "High" to precipIntensityColor(55f), "Very high" to precipIntensityColor(75f))
                .forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(4.dp))
                        Text(label, color = textSec, fontSize = 10.sp)
                    }
                }
        }
        Spacer(Modifier.height(10.dp))

        val values = chart.values
        val dayChangeI = chart.dayChangeIndex
        Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
            val n = values.size
            val gap = 2.5.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            val r = CornerRadius(3.dp.toPx())

            listOf(0.30f, 0.70f).forEach { frac ->
                val y = size.height * (1f - frac)
                drawLine(annotCapture, Offset(0f, y), Offset(size.width, y),
                    strokeWidth = 0.75.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }

            values.forEachIndexed { i, prob ->
                val barH = (prob / 100f) * size.height
                if (barH > 1.dp.toPx()) {
                    drawRoundRect(precipIntensityColor(prob),
                        topLeft = Offset(i * (barW + gap), size.height - barH),
                        size = Size(barW, barH), cornerRadius = r)
                }
            }

            if (dayChangeI != null && dayChangeI in 1 until n) {
                val x = (dayChangeI * (barW + gap)) - gap / 2f
                drawLine(annotCapture.copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f)))
            }
        }

        // Time axis
        val n = chart.labels.size
        val idxs = listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).distinct().filter { it in chart.labels.indices }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            idxs.forEach { i ->
                val isTmrw = dayChangeI != null && i == idxs.firstOrNull { it >= dayChangeI }
                Text(if (isTmrw) "tmrw" else chart.labels[i], color = textSec, fontSize = 11.sp,
                    fontWeight = if (isTmrw) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("— — 30%: possible  ·  70%: likely", color = textSec.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}

@Composable
fun DetailSheetContent(sheet: DetailSheet) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
        when (sheet) {
            is DetailSheet.Metric -> when (sheet.title) {
                "Wind"          -> WindDetailContent(sheet)
                "Feels like"    -> FeelsLikeDetailContent(sheet)
                "Precipitation" -> PrecipDetailContent(sheet)
                else            -> DefaultMetricContent(sheet)
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

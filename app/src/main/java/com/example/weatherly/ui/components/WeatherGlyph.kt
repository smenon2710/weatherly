package com.example.weatherly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist, flat vector weather icons drawn with Canvas — no emoji, no image
 * assets. Soft muted palette so they sit calmly on warm off-white surfaces.
 */
@Composable
fun WeatherGlyph(
    code: Int,
    isDay: Boolean = true,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawWeather(glyphFor(code, isDay), center, this.size.minDimension)
    }
}

private enum class Glyph { SUN, MOON, CLOUD_SUN, CLOUD_MOON, CLOUD, FOG, RAIN, SNOW, THUNDER }

private fun glyphFor(code: Int, isDay: Boolean): Glyph = when (code) {
    0 -> if (isDay) Glyph.SUN else Glyph.MOON
    1, 2 -> if (isDay) Glyph.CLOUD_SUN else Glyph.CLOUD_MOON
    3 -> Glyph.CLOUD
    45, 48 -> Glyph.FOG
    51, 53, 55, 56, 57 -> Glyph.RAIN
    61, 63, 65, 66, 67 -> Glyph.RAIN
    71, 73, 75, 77 -> Glyph.SNOW
    80, 81, 82 -> Glyph.RAIN
    85, 86 -> Glyph.SNOW
    95, 96, 99 -> Glyph.THUNDER
    else -> Glyph.CLOUD
}

private val SunColor = Color(0xFFE0B15C)
private val MoonColor = Color(0xFF93A1B8)
private val CloudColor = Color(0xFFAAB5C0)
private val RainColor = Color(0xFF7F97AD)
private val SnowColor = Color(0xFFA9B6C2)
private val BoltColor = Color(0xFFE0B15C)
private val FogColor = Color(0xFFB3BCC6)

private fun DrawScope.drawWeather(glyph: Glyph, c: Offset, dim: Float) {
    when (glyph) {
        Glyph.SUN -> drawSun(c, dim * 0.19f, SunColor)
        Glyph.MOON -> drawMoon(c, dim * 0.30f, MoonColor)
        Glyph.CLOUD -> drawCloud(Offset(c.x, c.y + dim * 0.04f), dim * 0.82f, CloudColor)
        Glyph.CLOUD_SUN -> {
            drawSun(Offset(c.x - dim * 0.20f, c.y - dim * 0.22f), dim * 0.12f, SunColor)
            drawCloud(Offset(c.x + dim * 0.05f, c.y + dim * 0.08f), dim * 0.72f, CloudColor)
        }
        Glyph.CLOUD_MOON -> {
            drawMoon(Offset(c.x - dim * 0.20f, c.y - dim * 0.20f), dim * 0.15f, MoonColor)
            drawCloud(Offset(c.x + dim * 0.05f, c.y + dim * 0.08f), dim * 0.72f, CloudColor)
        }
        Glyph.FOG -> {
            drawCloud(Offset(c.x, c.y - dim * 0.10f), dim * 0.78f, FogColor)
            val w = dim * 0.30f
            val sw = dim * 0.05f
            listOf(0.20f to 0.0f, 0.30f to 0.08f, 0.40f to -0.04f).forEach { (yf, xf) ->
                drawLine(
                    FogColor,
                    Offset(c.x - w + xf * dim, c.y + dim * yf),
                    Offset(c.x + w + xf * dim, c.y + dim * yf),
                    strokeWidth = sw, cap = StrokeCap.Round
                )
            }
        }
        Glyph.RAIN -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, CloudColor)
            val sw = dim * 0.055f
            listOf(-0.22f, 0f, 0.22f).forEach { xf ->
                val x = c.x + xf * dim
                drawLine(
                    RainColor,
                    Offset(x + dim * 0.04f, c.y + dim * 0.20f),
                    Offset(x - dim * 0.02f, c.y + dim * 0.36f),
                    strokeWidth = sw, cap = StrokeCap.Round
                )
            }
        }
        Glyph.SNOW -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, CloudColor)
            listOf(-0.22f, 0f, 0.22f).forEach { xf ->
                drawCircle(SnowColor, dim * 0.045f, Offset(c.x + xf * dim, c.y + dim * 0.28f))
            }
        }
        Glyph.THUNDER -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, CloudColor)
            val bolt = Path().apply {
                moveTo(c.x + dim * 0.04f, c.y + dim * 0.12f)
                lineTo(c.x - dim * 0.12f, c.y + dim * 0.30f)
                lineTo(c.x - dim * 0.01f, c.y + dim * 0.30f)
                lineTo(c.x - dim * 0.06f, c.y + dim * 0.44f)
                lineTo(c.x + dim * 0.14f, c.y + dim * 0.22f)
                lineTo(c.x + dim * 0.02f, c.y + dim * 0.22f)
                close()
            }
            drawPath(bolt, BoltColor)
        }
    }
}

private fun DrawScope.drawSun(c: Offset, coreR: Float, color: Color) {
    val rayIn = coreR * 1.35f
    val rayOut = coreR * 1.95f
    val w = coreR * 0.30f
    for (i in 0 until 8) {
        val a = Math.PI / 4.0 * i
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(
            color,
            Offset(c.x + dx * rayIn, c.y + dy * rayIn),
            Offset(c.x + dx * rayOut, c.y + dy * rayOut),
            strokeWidth = w, cap = StrokeCap.Round
        )
    }
    drawCircle(color, coreR, c)
}

private fun DrawScope.drawMoon(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
        val o = r * 0.58f
        addOval(Rect(c.x - r + o, c.y - r - r * 0.18f, c.x + r + o, c.y + r - r * 0.18f))
        fillType = PathFillType.EvenOdd
    }
    drawPath(p, color)
}

private fun DrawScope.drawCloud(c: Offset, w: Float, color: Color) {
    val r = w * 0.5f
    val baseY = c.y + r * 0.20f
    drawCircle(color, r * 0.46f, Offset(c.x - r * 0.55f, baseY - r * 0.04f))
    drawCircle(color, r * 0.60f, Offset(c.x - r * 0.02f, baseY - r * 0.34f))
    drawCircle(color, r * 0.42f, Offset(c.x + r * 0.52f, baseY - r * 0.02f))
    drawRoundRect(
        color,
        topLeft = Offset(c.x - r * 0.96f, baseY - r * 0.04f),
        size = Size(r * 1.92f, r * 0.60f),
        cornerRadius = CornerRadius(r * 0.30f, r * 0.30f)
    )
}

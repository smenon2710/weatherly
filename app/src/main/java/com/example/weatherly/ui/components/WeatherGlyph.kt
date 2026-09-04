package com.example.weatherly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.weatherly.util.wmoText
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
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
    modifier: Modifier = Modifier,
    contentDescription: String? = wmoText(code)
) {
    val m = if (contentDescription != null)
        modifier.size(size).semantics { this.contentDescription = contentDescription }
    else
        modifier.size(size)
    Canvas(modifier = m) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawWeather(
            glyphFor(code, isDay), center, this.size.minDimension,
            intensity = intensityFor(code), hasHail = hasHail(code)
        )
    }
}

internal enum class Glyph {
    SUN, MOON, CLOUD_SUN, CLOUD_MOON, CLOUD, FOG,
    RAIN, SNOW, THUNDER,
    MOON_RAIN, MOON_SNOW, MOON_THUNDER
}

internal fun glyphFor(code: Int, isDay: Boolean): Glyph = when (code) {
    0          -> if (isDay) Glyph.SUN        else Glyph.MOON
    1, 2       -> if (isDay) Glyph.CLOUD_SUN  else Glyph.CLOUD_MOON
    3          -> Glyph.CLOUD
    45, 48     -> Glyph.FOG
    51, 53, 55,
    56, 57     -> if (isDay) Glyph.RAIN       else Glyph.MOON_RAIN
    61, 63, 65,
    66, 67     -> if (isDay) Glyph.RAIN       else Glyph.MOON_RAIN
    71, 73, 75,
    77         -> if (isDay) Glyph.SNOW       else Glyph.MOON_SNOW
    80, 81, 82 -> if (isDay) Glyph.RAIN       else Glyph.MOON_RAIN
    85, 86     -> if (isDay) Glyph.SNOW       else Glyph.MOON_SNOW
    95, 96, 99 -> if (isDay) Glyph.THUNDER    else Glyph.MOON_THUNDER
    else       -> Glyph.CLOUD
}

/**
 * How many raindrops/snowflakes a RAIN/SNOW-family glyph draws, derived directly from the WMO
 * code's own built-in intensity tier — Open-Meteo's code table already distinguishes
 * light/moderate/heavy (or light/heavy) severity for drizzle, rain, freezing drizzle/rain, snow,
 * and showers as genuinely distinct codes, matching the same distinction
 * `WeatherBackground.classify()`'s animated scene already makes (`RainIntensity`/`SnowIntensity`
 * tiers) for these exact code groups. Previously every code in a RAIN/SNOW glyph family rendered
 * as an identical fixed-count icon regardless of real severity — this was real information the
 * data already provided but the icon discarded, not a data gap needing new fields.
 */
internal fun intensityFor(code: Int): Int = when (code) {
    51, 56, 61, 66, 71, 77, 80, 85 -> 1 // slight/light
    53, 63, 73                     -> 2 // moderate
    55, 57, 65, 67, 75, 82, 86     -> 3 // dense/heavy/violent
    else                            -> 2
}

/** Whether a thunderstorm glyph should show hail accents — WMO 96/99 are specifically
 * "thunderstorm with slight/heavy hail," distinct from plain 95, the same distinction
 * `WeatherBackground.classify()` already makes (THUNDER vs THUNDER_HAIL scenes). */
internal fun hasHail(code: Int): Boolean = code == 96 || code == 99

/** Color set for [drawWeather] — lets a caller swap the whole palette without duplicating the
 * shape-composition logic per glyph. [MutedGlyphColors] is this app's own in-screen palette
 * (unchanged, still the default); [VividGlyphColors] is a more saturated set for the home-screen
 * widget only (user-requested, referencing another weather app's widget) — the in-app icon
 * palette is a deliberate, separate design choice (soft/muted "so they sit calmly on warm
 * off-white surfaces") and isn't being changed. */
internal data class GlyphColors(
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val rain: Color,
    val snow: Color,
    val bolt: Color,
    val fog: Color,
)

internal val MutedGlyphColors = GlyphColors(
    sun = Color(0xFFE0B15C),
    moon = Color(0xFF93A1B8),
    cloud = Color(0xFFAAB5C0),
    rain = Color(0xFF7F97AD),
    snow = Color(0xFFA9B6C2),
    bolt = Color(0xFFE0B15C),
    fog = Color(0xFFB3BCC6),
)

internal val VividGlyphColors = GlyphColors(
    sun = Color(0xFFFFA726),
    moon = Color(0xFF9575CD),
    cloud = Color(0xFFECEFF1),
    rain = Color(0xFF42A5F5),
    snow = Color(0xFF81D4FA),
    bolt = Color(0xFFFFC107),
    fog = Color(0xFFCFD8DC),
)

/** Picks which of a shape's fixed drop/flake offsets to draw for a given intensity tier (1-3),
 * preserving each glyph's own existing spacing/asymmetry rather than introducing new positions —
 * 1 keeps just the middle position, 2 keeps the two outer ones, 3 (or anything else) keeps all. */
private fun dropOffsetsFor(intensity: Int, full: List<Float>): List<Float> = when (intensity) {
    1 -> listOf(full[1])
    2 -> listOf(full[0], full[2])
    else -> full
}

internal fun DrawScope.drawWeather(
    glyph: Glyph,
    c: Offset,
    dim: Float,
    colors: GlyphColors = MutedGlyphColors,
    intensity: Int = 2,
    hasHail: Boolean = false,
) {
    when (glyph) {
        Glyph.SUN        -> drawSun(c, dim * 0.19f, colors.sun)
        Glyph.MOON       -> drawMoon(c, dim * 0.30f, colors.moon)
        Glyph.CLOUD      -> drawCloud(Offset(c.x, c.y + dim * 0.04f), dim * 0.82f, colors.cloud)
        Glyph.CLOUD_SUN  -> {
            drawSun(Offset(c.x - dim * 0.20f, c.y - dim * 0.22f), dim * 0.12f, colors.sun)
            drawCloud(Offset(c.x + dim * 0.05f, c.y + dim * 0.08f), dim * 0.72f, colors.cloud)
        }
        Glyph.CLOUD_MOON -> {
            drawMoon(Offset(c.x - dim * 0.22f, c.y - dim * 0.20f), dim * 0.15f, colors.moon)
            drawCloud(Offset(c.x + dim * 0.05f, c.y + dim * 0.08f), dim * 0.72f, colors.cloud)
        }
        Glyph.FOG -> {
            drawCloud(Offset(c.x, c.y - dim * 0.10f), dim * 0.78f, colors.fog)
            val w = dim * 0.30f
            val sw = dim * 0.05f
            listOf(0.20f to 0.0f, 0.30f to 0.08f, 0.40f to -0.04f).forEach { (yf, xf) ->
                drawLine(
                    colors.fog,
                    Offset(c.x - w + xf * dim, c.y + dim * yf),
                    Offset(c.x + w + xf * dim, c.y + dim * yf),
                    strokeWidth = sw, cap = StrokeCap.Round
                )
            }
        }
        Glyph.RAIN -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, colors.cloud)
            val sw = dim * 0.055f
            dropOffsetsFor(intensity, listOf(-0.22f, 0f, 0.22f)).forEach { xf ->
                val x = c.x + xf * dim
                drawLine(colors.rain, Offset(x + dim * 0.04f, c.y + dim * 0.20f),
                    Offset(x - dim * 0.02f, c.y + dim * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
            }
        }
        Glyph.SNOW -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, colors.cloud)
            dropOffsetsFor(intensity, listOf(-0.22f, 0f, 0.22f)).forEach { xf ->
                drawCircle(colors.snow, dim * 0.045f, Offset(c.x + xf * dim, c.y + dim * 0.28f))
            }
        }
        Glyph.THUNDER -> {
            drawCloud(Offset(c.x, c.y - dim * 0.12f), dim * 0.78f, colors.cloud)
            drawBolt(c, dim, colors.bolt)
            if (hasHail) drawHail(c, dim, colors.snow)
        }
        Glyph.MOON_RAIN -> {
            drawMoon(Offset(c.x - dim * 0.22f, c.y - dim * 0.24f), dim * 0.13f, colors.moon)
            drawCloud(Offset(c.x + dim * 0.04f, c.y - dim * 0.02f), dim * 0.70f, colors.cloud)
            val sw = dim * 0.055f
            dropOffsetsFor(intensity, listOf(-0.17f, 0.05f, 0.26f)).forEach { xf ->
                val x = c.x + xf * dim
                drawLine(colors.rain, Offset(x + dim * 0.04f, c.y + dim * 0.16f),
                    Offset(x - dim * 0.02f, c.y + dim * 0.32f), strokeWidth = sw, cap = StrokeCap.Round)
            }
        }
        Glyph.MOON_SNOW -> {
            drawMoon(Offset(c.x - dim * 0.22f, c.y - dim * 0.24f), dim * 0.13f, colors.moon)
            drawCloud(Offset(c.x + dim * 0.04f, c.y - dim * 0.02f), dim * 0.70f, colors.cloud)
            dropOffsetsFor(intensity, listOf(-0.17f, 0.05f, 0.26f)).forEach { xf ->
                drawCircle(colors.snow, dim * 0.045f, Offset(c.x + xf * dim, c.y + dim * 0.22f))
            }
        }
        Glyph.MOON_THUNDER -> {
            drawMoon(Offset(c.x - dim * 0.22f, c.y - dim * 0.24f), dim * 0.13f, colors.moon)
            drawCloud(Offset(c.x + dim * 0.04f, c.y - dim * 0.02f), dim * 0.70f, colors.cloud)
            drawBolt(c, dim, colors.bolt)
            if (hasHail) drawHail(c, dim, colors.snow)
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

// Crescent moon via PathOperation.Difference — subtracts the shadow circle from
// the outer circle cleanly, without the EvenOdd overflow issue.
private fun DrawScope.drawMoon(c: Offset, r: Float, col: Color) {
    val outer = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
    val cut   = Path().apply {
        val cx2 = c.x + r * 0.40f
        val cy2 = c.y - r * 0.10f
        addOval(Rect(cx2 - r, cy2 - r, cx2 + r, cy2 + r))
    }
    val crescent = Path().also { it.op(outer, cut, PathOperation.Difference) }
    drawPath(crescent, col)
}

private fun DrawScope.drawBolt(c: Offset, dim: Float, color: Color) {
    val bolt = Path().apply {
        moveTo(c.x + dim * 0.04f, c.y + dim * 0.12f)
        lineTo(c.x - dim * 0.12f, c.y + dim * 0.30f)
        lineTo(c.x - dim * 0.01f, c.y + dim * 0.30f)
        lineTo(c.x - dim * 0.06f, c.y + dim * 0.44f)
        lineTo(c.x + dim * 0.14f, c.y + dim * 0.22f)
        lineTo(c.x + dim * 0.02f, c.y + dim * 0.22f)
        close()
    }
    drawPath(bolt, color)
}

/** Small ice-toned dots flanking the bolt — the visual accent for WMO 96/99's "with hail",
 * distinct from a plain thunderstorm (95). Reuses [GlyphColors.snow] (already a light icy tone)
 * rather than adding a dedicated hail color field. */
private fun DrawScope.drawHail(c: Offset, dim: Float, color: Color) {
    listOf(-0.20f to 0.32f, 0.22f to 0.36f).forEach { (xf, yf) ->
        drawCircle(color, dim * 0.035f, Offset(c.x + xf * dim, c.y + dim * yf))
    }
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

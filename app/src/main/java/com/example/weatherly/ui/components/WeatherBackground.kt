package com.example.weatherly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.ui.theme.LocalIsDarkTheme
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen animated backdrop, in the spirit of Apple Weather's per-condition scenes. Sits
 * behind the entire scrolling [com.example.weatherly.ui.WeatherScreen] content — visible in the
 * hero and in the gaps/margins around cards. Cards themselves are fully opaque (`GlassCard`); a
 * translucent "frosted glass" card fill was tried and reverted after it produced visibly patchy
 * seams against this background, especially in light mode — see `GlassCard`'s doc comment.
 *
 * A single [Canvas] redraws every frame off one shared `timeMs` clock — reading that value only
 * inside the draw phase (not composition) means each tick re-draws just this Canvas, not the
 * whole screen. Particle positions are pure functions of `(timeMs, per-particle seed)`; the seed
 * lists themselves are generated once per [Scene] via `remember(scene)`, not regenerated per
 * frame, so particles don't jitter.
 *
 * [Scene] selection is driven entirely by real data, not decoration: the WMO `code`, `cloudCoverPct`,
 * `visibility` (mist vs. fog), `aqi` (haze, and a smoky tint on fog), current wind speed (the
 * severe-wind streak overlay, and blizzard whiteout when paired with heavy snow), and — for
 * Tornado/Hurricane, which have no forecast weather code at all — real active NWS `alerts` text.
 * [Scene.DUST_STORM] and [Scene.VOLCANIC_ASH] are a deliberate exception: Open-Meteo has no
 * signal for either (no code, no alert type), so [classify] never returns them — they exist only
 * as implemented-but-unreachable scenes, per an explicit product decision to build the visuals
 * ahead of any data source that could drive them, rather than leave them unbuilt.
 */
@Composable
fun WeatherBackground(
    code: Int,
    isDay: Boolean,
    aqi: Int?,
    cloudCoverPct: Int?,
    visibility: Int?,
    visibilityUnit: String,
    windKmh: Int?,
    windGustKmh: Int?,
    alerts: List<WeatherAlert>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val alertEvents = remember(alerts) { alerts.map { it.event } }
    val scene = remember(code, isDay, cloudCoverPct, visibility, visibilityUnit, aqi, alertEvents) {
        classify(code, isDay, cloudCoverPct, visibility, visibilityUnit, aqi, alertEvents)
    }
    // A real hazard signal (windKmh/windGustKmh already fetched for the metrics grid), layered on
    // top of whatever base scene is active — not a separate WMO-driven scene, since gusty wind can
    // occur alongside almost any condition. Paired with SNOW_HEAVY this is what produces a
    // blizzard-style whiteout; on its own it's the "Severe Squall/Wind" treatment.
    val gustKmh = windGustKmh ?: windKmh ?: 0
    val severeWind = gustKmh >= 45

    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { timeMs = it }
        }
    }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isGoldenHour = hour in setOf(5, 6, 17, 18, 19, 20)

    val rainSeeds = remember(scene) { if (scene.rainIntensity != null) seeds(scene.rainIntensity.count) else emptyList() }
    val snowSeeds = remember(scene) { if (scene.snowIntensity != null) seeds(scene.snowIntensity.count) else emptyList() }
    val sleetSeeds = remember(scene) { if (scene == Scene.SLEET) seeds(90) else emptyList() }
    val hailSeeds = remember(scene) { if (scene == Scene.HAIL || scene == Scene.THUNDER_HAIL) seeds(35) else emptyList() }
    val fogSeeds = remember(scene) { if (scene == Scene.FOG || scene == Scene.MIST) seeds(if (scene == Scene.FOG) 5 else 3) else emptyList() }
    val dustSeeds = remember(scene) { if (scene == Scene.DUST_STORM) seeds(6) else emptyList() }
    val ashSeeds = remember(scene) { if (scene == Scene.VOLCANIC_ASH) seeds(70) else emptyList() }
    val cloudSeeds = remember(scene) {
        when (scene) {
            Scene.PARTLY_CLOUDY -> seeds(3)
            Scene.MOSTLY_CLOUDY -> seeds(5)
            else -> emptyList()
        }
    }
    val starSeeds = remember(scene) { if (scene == Scene.CLEAR_NIGHT || scene == Scene.FAIR_NIGHT) seeds(50) else emptyList() }
    val moteSeeds = remember(scene, isGoldenHour) {
        if (isGoldenHour && isDay && scene in setOf(Scene.CLEAR_DAY, Scene.FAIR_DAY, Scene.PARTLY_CLOUDY)) seeds(12) else emptyList()
    }
    val thunderSeeds = remember(scene) { if (scene == Scene.THUNDER || scene == Scene.THUNDER_HAIL || scene == Scene.HURRICANE) seeds(3) else emptyList() }
    val windSeeds = remember(severeWind) { if (severeWind) seeds(24) else emptyList() }
    val debrisSeeds = remember(scene) { if (scene == Scene.TORNADO) seeds(10) else emptyList() }

    val fogTint = remember(isDark, aqi) {
        val base = if (isDark) Color(0xFF3A424A) else Color(0xFFC7CFD6)
        val smoky = if (isDark) Color(0xFF4A3A2E) else Color(0xFFB8A388)
        val smokyFrac = (((aqi ?: 0) - 100).coerceIn(0, 100)) / 100f
        lerp(base, smoky, smokyFrac).copy(alpha = if (isDark) 0.16f else 0.20f)
    }
    val cloudTint = if (isDark) Color(0xFF4A5560).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.35f)

    // Rain/snow/sleet/hail/wind particles: dark mode keeps near-white, which pops against the
    // dark gradient stops. The same white is nearly invisible against light mode's pale pastel
    // sky stops — alpha alone can't fix a hue that's fundamentally wrong for a light background,
    // which is why the light-mode weather background looked "unchanging" (the base gradient was
    // shifting per condition the whole time; the particle layer on top just wasn't visible enough
    // to register). Light-mode inks reuse this app's existing rain/snow "slate blue" tip-tone
    // family (see tipColors() in WeatherComponents.kt) so particles read as this app's own
    // palette, not a generic gray overlay bolted on to fix a bug.
    val rainInk = if (isDark) Color.White else Color(0xFF3F5670)
    val snowInk = if (isDark) Color.White else Color(0xFF5D7A94)
    val sleetInk = if (isDark) Color(0xFFD8E4EA) else Color(0xFF56707F)
    val hailInk = if (isDark) Color(0xFFE8EDF0) else Color(0xFF6B7B85)
    val windInk = if (isDark) Color.White else Color(0xFF5D6B78)
    val iceSheenInk = if (isDark) Color.White else Color(0xFF6E88A0)
    val snowWashInk = if (isDark) Color.White else Color(0xFF9DB2C2)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(conditionGradient(code, isDay)))
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (scene) {
                Scene.CLEAR_DAY, Scene.FAIR_DAY -> drawSunGlow(timeMs, isGoldenHour)
                Scene.CLEAR_NIGHT, Scene.FAIR_NIGHT -> drawStars(starSeeds, timeMs)
                Scene.PARTLY_CLOUDY -> drawClouds(cloudSeeds, timeMs, cloudTint)
                Scene.MOSTLY_CLOUDY -> drawClouds(cloudSeeds, timeMs, cloudTint.copy(alpha = cloudTint.alpha * 1.4f))
                Scene.OVERCAST ->
                    // A flat, texture-blocked sheet rather than more discrete cloud blobs — per the
                    // brief, overcast should read as one uniform gray layer with no sun glow or
                    // directional shadows, not just "denser clouds."
                    drawRect(
                        (if (isDark) Color(0xFF3A4048) else Color(0xFFA8ADB2)).copy(alpha = if (isDark) 0.30f else 0.28f),
                        size = size
                    )
                Scene.HAZE -> drawHaze(timeMs, isDark)
                Scene.MIST -> drawFogBands(fogSeeds, timeMs, fogTint.copy(alpha = fogTint.alpha * 0.55f))
                Scene.FOG -> drawFogBands(fogSeeds, timeMs, fogTint)
                Scene.DRIZZLE -> drawRain(rainSeeds, timeMs, RainIntensity.DRIZZLE, isDark, rainInk)
                Scene.RAIN_LIGHT -> drawRain(rainSeeds, timeMs, RainIntensity.LIGHT, isDark, rainInk)
                Scene.RAIN_MODERATE -> drawRain(rainSeeds, timeMs, RainIntensity.MODERATE, isDark, rainInk)
                Scene.RAIN_HEAVY -> {
                    drawRain(rainSeeds, timeMs, RainIntensity.HEAVY, isDark, rainInk)
                    drawGroundWash(Color(0xFF16222C), 0.30f)
                }
                Scene.FREEZING_RAIN -> {
                    drawRain(rainSeeds, timeMs, RainIntensity.LIGHT, isDark, rainInk)
                    drawIceSheen(timeMs, iceSheenInk)
                }
                Scene.SNOW_LIGHT -> drawSnow(snowSeeds, timeMs, SnowIntensity.LIGHT, isDark, snowInk)
                Scene.SNOW_MODERATE -> {
                    drawSnow(snowSeeds, timeMs, SnowIntensity.MODERATE, isDark, snowInk)
                    drawGroundWash(snowWashInk, 0.16f)
                }
                Scene.SNOW_HEAVY -> {
                    drawSnow(snowSeeds, timeMs, SnowIntensity.HEAVY, isDark, snowInk)
                    drawGroundWash(snowWashInk, 0.26f)
                }
                Scene.SLEET -> {
                    drawSleet(sleetSeeds, timeMs, sleetInk)
                    drawGroundWash(Color(0xFF8FA3AC), 0.18f)
                }
                Scene.HAIL -> drawHail(hailSeeds, timeMs, hailInk)
                Scene.THUNDER -> {
                    drawRain(rainSeeds, timeMs, RainIntensity.HEAVY, isDark, rainInk)
                    drawThunderFlash(thunderSeeds, timeMs)
                }
                Scene.THUNDER_HAIL -> {
                    drawRain(rainSeeds, timeMs, RainIntensity.HEAVY, isDark, rainInk)
                    drawHail(hailSeeds, timeMs, hailInk)
                    drawThunderFlash(thunderSeeds, timeMs)
                }
                Scene.TORNADO -> {
                    drawRect(Color(0xFF1A1A20).copy(alpha = 0.35f), size = size)
                    drawTornado(timeMs, debrisSeeds)
                }
                Scene.HURRICANE -> {
                    drawRect(Color(0xFF10151C).copy(alpha = 0.30f), size = size)
                    drawHurricaneRain(rainSeeds, timeMs, rainInk)
                    drawThunderFlash(thunderSeeds, timeMs)
                }
                Scene.DUST_STORM -> drawDustStorm(dustSeeds, timeMs)
                Scene.VOLCANIC_ASH -> {
                    drawRect(Color(0xFF232323).copy(alpha = 0.35f), size = size)
                    drawAsh(ashSeeds, timeMs)
                }
            }
            if (moteSeeds.isNotEmpty()) drawMotes(moteSeeds, timeMs)
            if (severeWind && scene != Scene.TORNADO && scene != Scene.HURRICANE) drawWindStreaks(windSeeds, timeMs, windInk)
        }
    }
}

// ── Condition → scene mapping ───────────────────────────────────────────────────

private enum class Scene(val rainIntensity: RainIntensity? = null, val snowIntensity: SnowIntensity? = null) {
    CLEAR_DAY, FAIR_DAY, CLEAR_NIGHT, FAIR_NIGHT,
    PARTLY_CLOUDY, MOSTLY_CLOUDY, OVERCAST,
    MIST, FOG, HAZE,
    DRIZZLE(rainIntensity = RainIntensity.DRIZZLE),
    RAIN_LIGHT(rainIntensity = RainIntensity.LIGHT),
    RAIN_MODERATE(rainIntensity = RainIntensity.MODERATE),
    RAIN_HEAVY(rainIntensity = RainIntensity.HEAVY),
    FREEZING_RAIN(rainIntensity = RainIntensity.LIGHT),
    SNOW_LIGHT(snowIntensity = SnowIntensity.LIGHT),
    SNOW_MODERATE(snowIntensity = SnowIntensity.MODERATE),
    SNOW_HEAVY(snowIntensity = SnowIntensity.HEAVY),
    SLEET, HAIL,
    THUNDER(rainIntensity = RainIntensity.HEAVY),
    THUNDER_HAIL(rainIntensity = RainIntensity.HEAVY),
    TORNADO,
    HURRICANE(rainIntensity = RainIntensity.HEAVY),
    // Unreachable from classify() — see the WeatherBackground doc comment. Kept as real scenes
    // (own enum values, own drawing functions) rather than removed, per explicit product decision
    // to have the visuals ready ahead of any future data source, rather than fake a trigger today.
    DUST_STORM, VOLCANIC_ASH
}

/**
 * WMO weather codes — same ranges used by WeatherRepository.buildUpcomingHeadline/buildTips and
 * WeatherAdvisor, kept consistent rather than re-derived, extended here with the finer-grained
 * codes (freezing rain/drizzle, snow grains, thunderstorm+hail) those simpler call sites don't
 * need to distinguish. Priority order: an active Tornado/Hurricane alert overrides everything
 * (most severe, and the only signal available at all for those two — no forecast code exists for
 * either); then precipitation codes; then sky-cover, using real `cloudCoverPct`/`visibility`/`aqi`
 * data to split WMO's coarse 4-value sky bucket into the finer categories those fields can
 * actually support.
 */
private fun classify(
    code: Int,
    isDay: Boolean,
    cloudCoverPct: Int?,
    visibility: Int?,
    visibilityUnit: String,
    aqi: Int?,
    alertEvents: List<String>
): Scene {
    if (alertEvents.any { it.contains("Tornado", ignoreCase = true) }) return Scene.TORNADO
    if (alertEvents.any { it.contains("Hurricane", ignoreCase = true) || it.contains("Tropical Storm", ignoreCase = true) }) {
        return Scene.HURRICANE
    }
    return when {
        code == 96 || code == 99 -> Scene.THUNDER_HAIL
        code == 95 -> Scene.THUNDER
        code == 77 -> Scene.SLEET
        code == 71 || code == 85 -> Scene.SNOW_LIGHT
        code == 73 -> Scene.SNOW_MODERATE
        code == 75 || code == 86 -> Scene.SNOW_HEAVY
        code == 66 || code == 67 || code == 56 || code == 57 -> Scene.FREEZING_RAIN
        code == 65 || code == 82 -> Scene.RAIN_HEAVY
        code == 63 || code == 81 -> Scene.RAIN_MODERATE
        code == 61 || code == 80 -> Scene.RAIN_LIGHT
        code in 51..55 -> Scene.DRIZZLE
        code == 45 || code == 48 -> {
            // Standard meteorological mist/fog split (~1 km visibility), unit-aware since
            // `visibility` travels in whatever WeatherData.visibilityUnit is (km or mi) — using a
            // km threshold against a mile reading would silently mislabel every imperial user's
            // fog as mist. Missing visibility data defaults to the denser reading (Fog): the code
            // itself already asserts foggy-enough conditions were reported, so understating it is
            // the wrong direction to guess.
            val km = if (visibilityUnit.contains("mi")) (visibility ?: 0) * 1.60934f else (visibility ?: 0).toFloat()
            if (visibility != null && km >= 1f) Scene.MIST else Scene.FOG
        }
        code in 2..3 -> when {
            cloudCoverPct == null -> if (code == 3) Scene.OVERCAST else Scene.PARTLY_CLOUDY
            cloudCoverPct >= 90 -> Scene.OVERCAST
            cloudCoverPct >= 65 -> Scene.MOSTLY_CLOUDY
            else -> Scene.PARTLY_CLOUDY
        }
        (aqi ?: 0) > 100 -> Scene.HAZE
        code == 1 -> if (isDay) Scene.FAIR_DAY else Scene.FAIR_NIGHT
        else -> if (isDay) Scene.CLEAR_DAY else Scene.CLEAR_NIGHT
    }
}

private enum class RainIntensity(
    val count: Int,
    val basePxPerSec: Float,
    val streakPx: Float,
    val strokeWidthPx: Float,
    val alphaLight: Float,
    val alphaDark: Float
) {
    // alphaLight bumped up from the original white-based tuning: a colored ink (see rainInk in
    // WeatherBackground) has much better contrast against the light gradient than white ever did
    // at the same alpha, but these still need to be strong enough to read clearly on their own.
    DRIZZLE(45, 260f, 14f, 1.5f, 0.30f, 0.28f),
    LIGHT(80, 420f, 20f, 1.8f, 0.38f, 0.36f),
    MODERATE(120, 600f, 26f, 2.0f, 0.46f, 0.46f),
    HEAVY(160, 820f, 34f, 2.4f, 0.56f, 0.58f)
}

private enum class SnowIntensity(val count: Int, val basePxPerSec: Float, val sizeMin: Float, val sizeSpread: Float) {
    LIGHT(45, 16f, 1.2f, 2.2f),
    MODERATE(80, 24f, 1.4f, 2.8f),
    HEAVY(130, 40f, 1.6f, 3.4f)
}

// ── Particle seeds ──────────────────────────────────────────────────────────────

/** Four independent random floats per particle, generated once per scene and reused every frame
 * — animated position is a deterministic function of (timeMs, seed), never re-randomized. */
private data class Seed(val a: Float, val b: Float, val c: Float, val d: Float)

private fun seeds(n: Int): List<Seed> {
    val r = Random.Default
    return List(n) { Seed(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat()) }
}

private fun wrap01(timeMs: Long, phaseSeed: Float, cycleMs: Float): Float =
    (((timeMs + phaseSeed * cycleMs) % cycleMs) + cycleMs) % cycleMs / cycleMs

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

// ── Particle renderers ────────────────────────────────────────────────────────

private fun DrawScope.drawRain(seeds: List<Seed>, timeMs: Long, intensity: RainIntensity, isDark: Boolean, ink: Color) {
    val w = size.width
    val h = size.height
    val streak = intensity.streakPx
    val slant = w * 0.05f
    val color = ink.copy(alpha = if (isDark) intensity.alphaDark else intensity.alphaLight)
    seeds.forEach { s ->
        val speed = intensity.basePxPerSec * (0.75f + s.b * 0.5f)
        val cycleMs = (h + streak) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = t * (h + streak) - streak
        val xBase = s.c * w
        val x = (((xBase + t * slant) % w) + w) % w
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x - slant * 0.3f, y + streak),
            strokeWidth = intensity.strokeWidthPx,
            cap = StrokeCap.Round
        )
    }
}

/** Near-horizontal driving rain sheets — the "extreme horizontal rain" a hurricane needs, distinct
 * from [drawRain]'s gentle angled streaks rather than a parameter tweak to it, so normal rain
 * scenes can't accidentally end up with hurricane-grade slant. */
private fun DrawScope.drawHurricaneRain(seeds: List<Seed>, timeMs: Long, ink: Color) {
    val w = size.width
    val h = size.height
    val streak = 60f
    val color = ink.copy(alpha = 0.5f)
    seeds.forEach { s ->
        val speed = 1100f * (0.8f + s.b * 0.4f)
        val travel = w + streak * 3f
        val cycleMs = travel / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val x = t * travel - streak * 3f
        val y = s.c * h + sin(timeMs / 1000f + s.d * 6.283f) * 6f
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x - streak * 3f, y + streak * 0.4f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnow(seeds: List<Seed>, timeMs: Long, intensity: SnowIntensity, isDark: Boolean, ink: Color) {
    val w = size.width
    val h = size.height
    // Light mode gets a touch more alpha than dark, same reasoning as RainIntensity.alphaLight —
    // a colored ink contrasts far better than white ever did here, but still needs to read clearly.
    val baseAlpha = if (isDark) 0.55f else 0.65f
    seeds.forEach { s ->
        val speed = intensity.basePxPerSec * (0.75f + s.b * 0.5f)
        val cycleMs = (h + 24f) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = t * (h + 24f) - 12f
        val swayAmp = 10f + s.c * 26f
        val swayFreq = 0.3f + s.d * 0.7f
        val xBase = s.b * w
        val x = xBase + sin(timeMs / 1000f * swayFreq * 6.283f + s.d * 6.283f) * swayAmp
        val radius = intensity.sizeMin + s.c * intensity.sizeSpread
        drawCircle(ink.copy(alpha = baseAlpha + s.c * 0.3f), radius = radius, center = Offset(x, y))
    }
}

/** Slanted, hard-edged, fast streaks with small bounce ticks at the baseline — sleet reads as
 * "harder than rain, smaller than hail" rather than a snow or rain variant. */
private fun DrawScope.drawSleet(seeds: List<Seed>, timeMs: Long, ink: Color) {
    val w = size.width
    val h = size.height
    val streak = 10f
    val slant = w * 0.09f
    seeds.forEach { s ->
        val speed = 950f * (0.8f + s.b * 0.4f)
        val cycleMs = (h + streak) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = t * (h + streak) - streak
        val x = (((s.c * w) + t * slant) % w + w) % w
        drawLine(
            color = ink.copy(alpha = 0.65f),
            start = Offset(x, y), end = Offset(x - slant * 0.4f, y + streak),
            strokeWidth = 1.6f, cap = StrokeCap.Round
        )
        // Bounce tick at this particle's resting baseline — a fixed per-seed ground position, not
        // synced to the exact landing frame (cheap approximation of "bouncing off surfaces").
        val groundY = h - 6f - s.d * (h * 0.25f)
        drawLine(
            color = ink.copy(alpha = 0.35f),
            start = Offset(x - 3f, groundY), end = Offset(x + 3f, groundY),
            strokeWidth = 1.2f, cap = StrokeCap.Round
        )
    }
}

/** Falling hard spheres with static impact marks — impact positions are per-seed fixed points
 * (not synced to the exact frame a given particle lands), a deliberate simplification that still
 * reads as "hail hitting the ground" without simulating real per-particle collision timing. */
private fun DrawScope.drawHail(seeds: List<Seed>, timeMs: Long, ink: Color) {
    val w = size.width
    val h = size.height
    seeds.forEach { s ->
        val speed = 700f * (0.8f + s.b * 0.4f)
        val cycleMs = (h + 10f) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = t * (h + 10f) - 5f
        val x = s.c * w
        val radius = 2.5f + s.d * 2.5f
        drawCircle(ink.copy(alpha = 0.85f), radius = radius, center = Offset(x, y))
        if (t > 0.9f) {
            val impactAlpha = (1f - t) / 0.1f * 0.4f
            val iy = h - 4f
            drawLine(ink.copy(alpha = impactAlpha), Offset(x - 5f, iy), Offset(x + 5f, iy), strokeWidth = 1.5f)
        }
    }
}

private fun DrawScope.drawFogBands(seeds: List<Seed>, timeMs: Long, tint: Color) {
    val w = size.width
    val h = size.height
    seeds.forEachIndexed { i, s ->
        val bandH = h * 0.13f
        val y = h * (0.08f + i * (0.8f / seeds.size.coerceAtLeast(1)))
        val bandW = w * (0.85f + s.c * 0.45f)
        val cycleMs = 26000f + s.d * 16000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val x = -bandW * 0.5f + t * (w + bandW)
        drawRoundRect(
            color = tint.copy(alpha = tint.alpha * (0.6f + s.c * 0.4f)),
            topLeft = Offset(x, y),
            size = Size(bandW, bandH),
            cornerRadius = CornerRadius(bandH / 2, bandH / 2)
        )
    }
}

/** Warm, mostly-static horizon wash — haze doesn't drift like fog bands, it's a stagnant-air tint,
 * so this only pulses gently rather than sliding across like [drawFogBands]. */
private fun DrawScope.drawHaze(timeMs: Long, isDark: Boolean) {
    val pulse = 0.85f + 0.15f * ((sin(timeMs / 1000f * 0.25f) + 1f) / 2f)
    val color = if (isDark) Color(0xFF4A3F2A) else Color(0xFFE8D9A8)
    val baseAlpha = if (isDark) 0.20f else 0.24f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0f), color.copy(alpha = baseAlpha * pulse)),
            startY = size.height * 0.25f,
            endY = size.height
        ),
        size = size
    )
}

private fun DrawScope.drawClouds(seeds: List<Seed>, timeMs: Long, tint: Color) {
    val w = size.width
    seeds.forEachIndexed { i, s ->
        val scale = 60f + s.c * 70f
        val y = size.height * (0.08f + s.d * 0.28f)
        val cycleMs = 40000f + s.a * 30000f
        val t = wrap01(timeMs, i * 5000f / cycleMs, cycleMs)
        val cx = -scale * 2f + t * (w + scale * 4f)
        val col = tint.copy(alpha = tint.alpha * (0.7f + s.c * 0.3f))
        drawCircle(col, radius = scale * 0.6f, center = Offset(cx - scale * 0.6f, y))
        drawCircle(col, radius = scale * 0.8f, center = Offset(cx, y - scale * 0.15f))
        drawCircle(col, radius = scale * 0.55f, center = Offset(cx + scale * 0.65f, y + scale * 0.05f))
    }
}

private fun DrawScope.drawStars(seeds: List<Seed>, timeMs: Long) {
    val w = size.width
    val h = size.height * 0.75f
    seeds.forEach { s ->
        val x = s.a * w
        val y = s.b * h
        val radius = 1f + s.c * 1.8f
        val twinkleSpeed = 0.5f + s.d * 1.0f
        val phase = s.c * 6.283f
        val alpha = 0.35f + 0.45f * ((sin(timeMs / 1000f * twinkleSpeed * 6.283f + phase) + 1f) / 2f)
        drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(x, y))
    }
}

private fun DrawScope.drawSunGlow(timeMs: Long, warm: Boolean) {
    val cx = size.width * 0.78f
    val cy = size.height * 0.12f
    val pulse = 0.92f + 0.08f * ((sin(timeMs / 1000f * 0.5f) + 1f) / 2f)
    val radius = size.width * 0.35f * pulse
    val color = if (warm) Color(0xFFFFD9A0) else Color(0xFFFFF6DD)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}

/** Soft warm light motes drifting slowly upward — the extra touch that distinguishes a golden-hour
 * "beautiful evening" from a plain clear day, layered on top of the base scene. */
private fun DrawScope.drawMotes(seeds: List<Seed>, timeMs: Long) {
    val w = size.width
    val h = size.height
    seeds.forEach { s ->
        val speed = 6f + s.b * 10f
        val travel = h * 0.5f
        val cycleMs = travel / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = h * 0.6f - t * travel
        val x = s.c * w + sin(timeMs / 1000f * 0.4f + s.d * 6.283f) * 14f
        val alpha = sin(t * 3.14159f).coerceIn(0f, 1f) * 0.5f
        drawCircle(Color(0xFFFFD9A0).copy(alpha = alpha), radius = 2f + s.c * 2f, center = Offset(x, y))
    }
}

private fun DrawScope.drawThunderFlash(seeds: List<Seed>, timeMs: Long) {
    val cycleMs = 9000f
    val t = timeMs % cycleMs
    seeds.forEachIndexed { i, s ->
        val triggerT = (i * cycleMs / 3f) + s.a * (cycleMs / 3.5f)
        val dt = t - triggerT
        if (dt in 0f..220f) {
            val progress = dt / 220f
            val rampIn = if (progress < 0.15f) progress / 0.15f else 1f
            val alpha = (1f - progress) * 0.5f * rampIn
            drawRect(Color.White.copy(alpha = alpha.coerceIn(0f, 0.5f)), size = size)
        }
    }
}

/** A flat translucent strip along the bottom of the screen — cheap stand-in for "damp reflective
 * ground" (rain), "visible accumulation" (snow), or "glossy ice sheen" (freezing rain, via
 * [drawIceSheen] instead). Not literal ground geometry, just a readable color cue. */
private fun DrawScope.drawGroundWash(color: Color, alpha: Float) {
    val bandH = size.height * 0.14f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0f), color.copy(alpha = alpha)),
            startY = size.height - bandH,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - bandH),
        size = Size(size.width, bandH)
    )
}

/** Slow-moving specular highlight sweeping across the lower half of the screen — the "high-gloss
 * reflective ice sheen" freezing rain needs, distinct from the flat opacity wash every other scene
 * uses, since ice is specifically about catching and moving light. */
private fun DrawScope.drawIceSheen(timeMs: Long, ink: Color) {
    val w = size.width
    val h = size.height
    val t = wrap01(timeMs, 0f, 14000f)
    val cx = -w * 0.3f + t * w * 1.6f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                ink.copy(alpha = 0f),
                ink.copy(alpha = 0.28f),
                ink.copy(alpha = 0f)
            ),
            startX = cx - w * 0.25f,
            endX = cx + w * 0.25f
        ),
        topLeft = Offset(0f, h * 0.55f),
        size = Size(w, h * 0.45f)
    )
    drawGroundWash(ink, 0.24f)
}

/** Drifting ochre/gold particulate bands (reusing the fog-band drift mechanic, since a dust wall
 * moves the same way a fog bank does) plus scattered grit specks for texture. */
private fun DrawScope.drawDustStorm(seeds: List<Seed>, timeMs: Long) {
    val tint = Color(0xFFB5854A).copy(alpha = 0.30f)
    drawFogBands(seeds, timeMs, tint)
    val w = size.width
    val h = size.height
    seeds.forEachIndexed { i, s ->
        repeat(6) { j ->
            val seedMix = (s.a + j * 0.13f) % 1f
            val cycleMs = 9000f + seedMix * 6000f
            val t = wrap01(timeMs, seedMix, cycleMs)
            val x = t * w
            val y = h * (0.2f + ((i * 7 + j) % 11) / 11f * 0.7f)
            drawCircle(Color(0xFF8A6435).copy(alpha = 0.3f), radius = 1.5f, center = Offset(x, y))
        }
    }
}

/** Dark, gritty falling flecks — visually a darker, faster, smaller cousin of [drawSnow] rather
 * than a wholly separate mechanic, since "descending particle flakes" is the same underlying shape
 * as snowfall, just with volcanic ash's palette and motion. */
private fun DrawScope.drawAsh(seeds: List<Seed>, timeMs: Long) {
    val w = size.width
    val h = size.height
    seeds.forEach { s ->
        val speed = 55f + s.b * 40f
        val cycleMs = (h + 16f) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val y = t * (h + 16f) - 8f
        val x = s.c * w + sin(timeMs / 1000f * 0.5f + s.d * 6.283f) * 8f
        drawCircle(Color(0xFF1C1C1C).copy(alpha = 0.5f + s.c * 0.3f), radius = 1f + s.c * 1.6f, center = Offset(x, y))
    }
}

/** Rotating tapered funnel (stacked, wobbling ellipses narrowing to a point) with orbiting debris
 * near the base — the one genuinely bespoke illustration in this set; everything else reuses a
 * shared falling/drifting/wash primitive, but there's no other scene this shape applies to. */
private fun DrawScope.drawTornado(timeMs: Long, debris: List<Seed>) {
    val cx = size.width * 0.5f
    val topY = size.height * 0.12f
    val bottomY = size.height * 0.88f
    val topW = size.width * 0.32f
    val bottomW = size.width * 0.035f
    val rotationDeg = (timeMs / 1000f * 220f) % 360f

    drawOval(
        Color(0xFF2A2A32).copy(alpha = 0.55f),
        topLeft = Offset(cx - topW, topY - topW * 0.22f),
        size = Size(topW * 2f, topW * 0.5f)
    )

    val steps = 16
    for (i in 0 until steps) {
        val f = i / (steps - 1f)
        val y = lerpF(topY, bottomY, f)
        val segW = lerpF(topW, bottomW, f)
        val wobbleAngle = (rotationDeg + f * 360f) * 3.14159f / 180f
        val wobble = sin(wobbleAngle) * segW * 0.18f
        drawOval(
            Color(0xFF3A3A44).copy(alpha = 0.30f + 0.28f * (1f - f)),
            topLeft = Offset(cx - segW / 2f + wobble, y),
            size = Size(segW, segW * 0.32f)
        )
    }

    val n = debris.size.coerceAtLeast(1)
    debris.forEachIndexed { i, s ->
        val angleDeg = (rotationDeg * 2.2f + i * (360f / n) + s.a * 40f)
        val angle = angleDeg * 3.14159f / 180f
        val r = bottomW * 1.6f + (s.b * 22f)
        val dx = cx + cos(angle) * r
        val dy = bottomY - 4f + sin(angle) * r * 0.28f
        drawCircle(Color(0xFF6B6255).copy(alpha = 0.55f), radius = 1.5f + s.c * 2f, center = Offset(dx, dy))
    }
}

/** Fast horizontal motion-distortion lines, layered over any scene once gusts cross the severe
 * threshold — the "Severe Squall/Wind" treatment. Deliberately not drawn for Tornado/Hurricane,
 * which already imply extreme wind through their own dedicated scenes. */
private fun DrawScope.drawWindStreaks(seeds: List<Seed>, timeMs: Long, ink: Color) {
    val w = size.width
    val h = size.height
    seeds.forEach { s ->
        val streakLen = w * (0.08f + s.c * 0.10f)
        val speed = w * (1.1f + s.b * 0.6f)
        val cycleMs = (w + streakLen) / speed * 1000f
        val t = wrap01(timeMs, s.a, cycleMs)
        val x = t * (w + streakLen) - streakLen
        val y = s.d * h
        drawLine(
            color = ink.copy(alpha = 0.14f + s.c * 0.10f),
            start = Offset(x, y), end = Offset(x + streakLen, y + streakLen * 0.06f),
            strokeWidth = 1.2f, cap = StrokeCap.Round
        )
    }
}

package com.example.weatherly.data.advice

import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import kotlin.math.roundToInt

/** The everyday questions Weatherly can answer locally, with no network call. */
enum class AdviceIntent { UMBRELLA, JACKET, WALKING, DRIVING, HIKING, CLOTHING }

/**
 * Turns the current forecast into short, practical advice using simple rules.
 * All thresholds reason in Celsius / km-h internally, but answers are phrased
 * in whatever units the user is viewing.
 */
object WeatherAdvisor {

    fun advise(intent: AdviceIntent, w: WeatherData, units: UnitSystem): String {
        val metric = units == UnitSystem.METRIC
        val feelsC = toC(w.realFeelC ?: w.currentTempC, metric)
        val windK = toKmh(w.windKmh ?: 0, metric)
        val gustK = toKmh(w.windGustKmh ?: w.windKmh ?: 0, metric)
        val rain = rainChance(w)
        val code = w.currentIcon
        // Real signal, not just a WMO-code guess: `rain` above is a type-agnostic precipitation
        // probability (Open-Meteo doesn't distinguish rain from snow in it), so a high reading
        // used to tell people to grab an umbrella even when it was actually snowing. Checked first
        // in umbrella() so that case gets the right advice instead.
        val isSnowy = (w.currentSnowfall ?: 0.0) > 0.0 ||
            w.hourlySnowfall.take(3).sum() > 0.0 ||
            isSnowOrIce(code)
        val ctx = Ctx(w, units, metric, feelsC, windK, gustK, rain, code, isSnowy)

        return when (intent) {
            AdviceIntent.UMBRELLA -> umbrella(ctx)
            AdviceIntent.JACKET -> jacket(ctx)
            AdviceIntent.WALKING -> walking(ctx)
            AdviceIntent.DRIVING -> driving(ctx)
            AdviceIntent.HIKING -> hiking(ctx)
            AdviceIntent.CLOTHING -> clothing(ctx)
        }
    }

    private val UMBRELLA_RE = Regex("\\bumbrella\\b")
    private val JACKET_RE = Regex("\\b(jacket|coat)\\b")
    private val WALKING_RE = Regex("\\b(walk|walking|jog|jogging|run|running)\\b")
    private val DRIVING_RE = Regex("\\b(driv(e|ing)|road|commute|commuting)\\b")
    private val HIKING_RE = Regex("\\b(hik(e|ing)|trail)\\b")
    private val CLOTHING_RE = Regex("\\b(wear|outfit|clothes|clothing)\\b")

    /**
     * Matches free-form chat text to the same six local intents the quick-suggestion chips
     * already answer for free — lets a *typed* "should I bring an umbrella today?" get the
     * identical zero-latency, zero-cost local answer the chip gives, instead of always paying
     * for an LLM call. Reserves the LLM for genuinely open-ended questions (multi-day planning,
     * comparisons, anything combining more than one topic) per the direction logged in
     * IMPROVEMENTS.md's "AI Chat — Keep, but Tighten LLM Usage" section.
     *
     * Deliberately conservative on two axes, both aimed at not misrouting a question that
     * actually needs LLM synthesis: (1) only a short message (≤12 words) is considered at all —
     * a longer message is far more likely to be a compound or nuanced question ("should I bring
     * an umbrella or just reschedule the hike, and what about Thursday?") that a single-intent
     * rule-based answer would only half-address; (2) intents are checked in a fixed priority
     * order and the first match wins, so a message mentioning two keywords still gets exactly one
     * (predictable, if not necessarily the "best") local answer rather than an ambiguous one.
     * Not tuned against a large real corpus of user phrasing — a reasonable first cut, expected
     * to need revisiting once real usage shows what gets misrouted either direction.
     */
    fun matchIntent(text: String): AdviceIntent? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.split(Regex("\\s+")).size > 12) return null
        val t = trimmed.lowercase()
        return when {
            UMBRELLA_RE.containsMatchIn(t) -> AdviceIntent.UMBRELLA
            JACKET_RE.containsMatchIn(t) -> AdviceIntent.JACKET
            WALKING_RE.containsMatchIn(t) -> AdviceIntent.WALKING
            DRIVING_RE.containsMatchIn(t) -> AdviceIntent.DRIVING
            HIKING_RE.containsMatchIn(t) -> AdviceIntent.HIKING
            CLOTHING_RE.containsMatchIn(t) -> AdviceIntent.CLOTHING
            else -> null
        }
    }

    private data class Ctx(
        val w: WeatherData,
        val units: UnitSystem,
        val metric: Boolean,
        val feelsC: Int,
        val windK: Int,
        val gustK: Int,
        val rain: Int,
        val code: Int,
        val isSnowy: Boolean
    ) {
        val feelsLabel = "${w.realFeelC ?: w.currentTempC}${units.tempLabel}"
        val tempLabel = "${w.currentTempC}${units.tempLabel}"
        val windLabel = w.windKmh?.let { "$it ${w.windUnit}" } ?: "calm"
    }

    private fun umbrella(c: Ctx): String {
        if (isThunder(c.code)) return "Thunderstorms around, so rain is likely — take a waterproof layer, " +
            "and note an umbrella won't help much if it's gusty (winds ${c.windLabel})."
        // Checked before the rain-percentage logic below: that percentage is a type-agnostic
        // "chance of precipitation" and would otherwise tell someone to grab an umbrella for snow.
        if (c.isSnowy) return "It's snow, not rain, so an umbrella won't do much good — wear waterproof " +
            "boots and watch your footing instead."
        return when {
            c.rain >= 60 || isRainCode(c.code) ->
                "Yes — there's a high chance of rain (${c.rain}%). Take an umbrella" +
                    if (c.windK >= 35) ", though with winds around ${c.windLabel} a rain jacket may hold up better." else "."
            c.rain in 30..59 ->
                "Maybe — about a ${c.rain}% chance of rain, so a compact umbrella is worth keeping with you."
            else ->
                "Probably not needed — only a ${c.rain}% chance of rain. You can likely leave the umbrella at home."
        }
    }

    private fun jacket(c: Ctx): String {
        val base = when {
            c.feelsC < 0 -> "It feels like ${c.feelsLabel} — a heavy coat, plus a hat and gloves."
            c.feelsC < 8 -> "It feels like ${c.feelsLabel} — a warm jacket is a good idea."
            c.feelsC < 15 -> "It feels like ${c.feelsLabel} — a light jacket or a couple of layers should be comfortable."
            c.feelsC < 22 -> "It feels like ${c.feelsLabel} — pleasant; maybe a light layer for the evening."
            else -> "It feels like ${c.feelsLabel} — no jacket needed, light clothing is fine."
        }
        val extra = when {
            c.isSnowy -> " It's snow, not rain — go for something waterproof and insulated."
            c.rain >= 50 -> " Go for something water-resistant given the ${c.rain}% precipitation chance."
            c.windK >= 35 -> " A windproof layer would help with the breeze (${c.windLabel})."
            else -> ""
        }
        return base + extra
    }

    private fun walking(c: Ctx): String {
        if (isThunder(c.code)) return "Best to hold off — there are thunderstorms about. Wait until they pass before heading out."
        val cautions = buildList {
            if (c.isSnowy) add("snow underfoot, so watch your footing")
            else if (c.rain >= 50) add("a ${c.rain}% chance of rain")
            if (c.feelsC >= 30) add("it's hot at ${c.feelsLabel}, so hydrate and take it easy")
            if (c.feelsC <= 2) add("it's cold at ${c.feelsLabel}, so wrap up well")
            if (c.gustK >= 45) add("gusty winds")
            if ((c.w.aqi ?: 0) > 150) add("poor air quality (AQI ${c.w.aqi})")
            if ((c.w.uvIndex ?: 0) >= 8 && c.w.isDay) add("very high UV — wear sunscreen")
        }
        return if (cautions.isEmpty()) {
            "Good conditions for a walk or jog — feels like ${c.feelsLabel}, low precipitation chance (${c.rain}%), and winds are ${c.windLabel}."
        } else {
            "Doable, but keep in mind " + cautions.joinToString(", and ") +
                ". Otherwise it feels like ${c.feelsLabel} with winds ${c.windLabel}."
        }
    }

    private fun driving(c: Ctx): String {
        val hazards = buildList {
            if (isFog(c.code) || (c.w.visibility ?: 99) <= 2) add("reduced visibility — use low-beam headlights and slow down")
            // c.isSnowy also catches snow arriving in the next few hours, not just the current
            // icon, unlike a bare isSnowOrIce(c.code) check.
            if (c.isSnowy) add("snow or ice is possible, so roads may be slippery — leave extra braking distance")
            if (c.rain >= 60 || c.code in 63..65 || c.code == 82) add("wet roads from heavy rain — ease off the speed")
            if (isThunder(c.code)) add("thunderstorms can bring sudden downpours and poor visibility")
            if (c.gustK >= 55) add("strong gusts (${c.gustK} km/h) — watch for crosswinds on open stretches")
        }
        return if (hazards.isEmpty()) {
            "Conditions look fine for driving — visibility is good and roads should be clear. Usual care is enough."
        } else {
            "Drive with care: " + hazards.joinToString("; ") + "."
        }
    }

    private fun hiking(c: Ctx): String {
        if (isThunder(c.code)) return "Not a good day for a hike — thunderstorms make exposed trails risky. Save it for clearer weather."
        val notes = buildList {
            if (c.isSnowy) add("snow could make trails icy or hard to follow — waterproof boots and poles help")
            else if (c.rain >= 50) add("a ${c.rain}% rain chance could mean muddy, slippery trails")
            if (c.feelsC >= 30) add("it's hot (${c.feelsLabel}) — start early, carry plenty of water")
            if (c.feelsC <= 2) add("it's cold (${c.feelsLabel}) — layer up and watch for ice")
            if ((c.w.uvIndex ?: 0) >= 8) add("UV is very high — hat, sunglasses and sunscreen")
            if (c.gustK >= 45) add("gusty winds on exposed ridges")
        }
        val daylight = c.w.sunset?.let { " Daylight lasts until about $it." } ?: ""
        return if (notes.isEmpty()) {
            "Great day for a hike — feels like ${c.feelsLabel}, low precipitation chance (${c.rain}%), and manageable winds.$daylight"
        } else {
            "You can hike, just plan for " + notes.joinToString(", and ") + ".$daylight"
        }
    }

    private fun clothing(c: Ctx): String {
        val core = when {
            c.feelsC < 0 -> "Bundle up: heavy coat, hat, gloves and a scarf."
            c.feelsC < 8 -> "Dress warm: a proper jacket over a sweater."
            c.feelsC < 15 -> "Go for layers — a light jacket you can take off as it warms."
            c.feelsC < 22 -> "Comfortable in a t-shirt with a light layer for later."
            c.feelsC < 28 -> "Light, breathable clothing is ideal."
            else -> "Keep it light and airy, and stay hydrated — it's warm out."
        }
        val adds = buildList {
            if (c.isSnowy) add("it's snow rather than rain — waterproof, insulated boots are worth it")
            else if (c.rain >= 50) add("bring a waterproof layer (${c.rain}% precipitation chance)")
            if ((c.w.uvIndex ?: 0) >= 8 && c.w.isDay) add("add sunglasses and sunscreen for the strong sun")
            if (c.windK >= 35) add("a windproof layer for the breeze")
        }
        val tail = if (adds.isEmpty()) "" else " Also " + adds.joinToString(", and ") + "."
        return "It feels like ${c.feelsLabel} right now. $core$tail"
    }

    // --- helpers ----------------------------------------------------------
    private fun rainChance(w: WeatherData): Int {
        val next = w.hourly.take(6).mapNotNull { it.precipChance }
        val hourlyMax = next.maxOrNull() ?: 0
        val dailyMax = w.daily.firstOrNull()?.precipProbMax ?: 0
        return maxOf(hourlyMax, dailyMax)
    }

    private fun toC(v: Int, metric: Boolean): Int =
        if (metric) v else ((v - 32) * 5.0 / 9.0).roundToInt()

    private fun toKmh(v: Int, metric: Boolean): Int =
        if (metric) v else (v * 1.609).roundToInt()

    private fun isRainCode(code: Int) =
        code in 51..67 || code in 80..82
    private fun isSnowOrIce(code: Int) =
        code in 71..77 || code in 85..86 || code in 66..67
    private fun isThunder(code: Int) = code in 95..99
    private fun isFog(code: Int) = code == 45 || code == 48
}

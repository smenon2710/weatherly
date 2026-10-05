package com.example.weatherly.data.advice

import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.WeatherData
import java.util.Locale

/** One titled paragraph of the Forecast Insight sheet. */
data class BriefingSection(val title: String, val text: String)

/**
 * The body of the Forecast Insight sheet (the one the hero pill opens): a short, plain-language
 * read of the forecast, one paragraph each for temperature, sky, precipitation, active alerts and
 * the days ahead. Exists because the sheet used to show little beyond the pill's own sentence —
 * user-reported as adding nothing.
 *
 * Deterministic and local, like [WeatherAdvisor] — no LLM. Written as sentences that interpret
 * the forecast ("it should ease off around 4 PM", "most likely it stays dry") rather than a
 * table of readings: an earlier version of this sheet listed raw metric values and was rejected
 * for duplicating the metrics grid. Built purely from [WeatherData], so it also works on a
 * forecast restored from cache.
 */
object ForecastBriefing {

    private const val HOURS = 12

    fun build(w: WeatherData): List<BriefingSection> = buildList {
        add(BriefingSection("Temperature", temperature(w)))
        sky(w)?.let { add(BriefingSection("Sky", it)) }
        precipitation(w)?.let { add(BriefingSection("Precipitation", it)) }
        alerts(w)?.let { add(BriefingSection("Alerts", it)) }
        ahead(w)?.let { add(BriefingSection("Looking ahead", it)) }
    }

    private fun temperature(w: WeatherData): String {
        val parts = mutableListOf<String>()
        val now = w.currentTempC
        val feels = w.realFeelC
        // A feels-like gap only worth calling out once it's big enough to notice.
        val gap = if (w.windUnit == "km/h") 2 else 4
        parts += when {
            feels != null && feels - now >= gap -> "It's $now° right now, but it feels warmer — more like $feels°."
            feels != null && now - feels >= gap -> "It's $now° right now, but it feels cooler — more like $feels°."
            else -> "It's $now° right now, and feels about like that."
        }
        val next = w.hourly.take(HOURS)
        if (next.size >= 2) {
            val hi = next.maxBy { it.tempC }
            val lo = next.minBy { it.tempC }
            parts += if (hi.tempC == lo.tempC) {
                "It should hold steady around ${hi.tempC}° over the next ${next.size} hours."
            } else {
                "Over the next ${next.size} hours, expect it to peak near ${hi.tempC}° ${at(hi)} " +
                    "and dip to about ${lo.tempC}° ${at(lo)}."
            }
        }
        w.comparedToYesterday?.let {
            parts += if (it.startsWith("About")) "Today's high is about the same as yesterday's."
            else "Today's high is ${it.removeSuffix(" than yesterday")} than yesterday's."
        }
        return parts.joinToString(" ")
    }

    private fun sky(w: WeatherData): String? {
        val now = Sky.of(w.currentIcon) ?: return null
        val next = w.hourly.take(HOURS)
        val change = next.drop(1).firstOrNull { Sky.of(it.icon).let { s -> s != null && s != now } }
        val then = if (change != null) {
            "Expect it to ${Sky.of(change.icon)!!.then} around ${change.hourLabel}."
        } else {
            "That should hold for the next ${next.size.coerceAtLeast(1)} hours."
        }
        return "${now.now} right now. $then"
    }

    private fun precipitation(w: WeatherData): String? {
        val next = w.hourly.take(HOURS)
        if (next.isEmpty()) return null
        val chances = next.map { it.precipChance ?: 0 }
        val rainSum = w.hourlyPrecipAmount.take(HOURS).sum()
        val snowSum = w.hourlySnowfall.take(HOURS).sum()
        val peakIdx = chances.indices.maxBy { chances[it] }
        val peak = chances[peakIdx]
        // precipChance doesn't say rain or snow (see CLAUDE.md's Key invariants) — the type comes
        // from the real per-hour amounts, falling back to the peak hour's weather code.
        val kind = when {
            snowSum > 0 && rainSum > 0 -> "rain and snow"
            snowSum > 0 || isSnow(next[peakIdx].icon) -> "snow"
            else -> "rain"
        }
        val parts = mutableListOf<String>()
        val snowingNow = (w.currentSnowfall ?: 0.0) > 0 || isSnow(w.currentIcon)
        val fallingNow = snowingNow || (w.currentRainMm ?: 0.0) > 0 || isPrecip(w.currentIcon)
        if (fallingNow) {
            val verb = if (snowingNow) "snowing" else "raining"
            val stop = next.drop(1).firstOrNull { !isPrecip(it.icon) && (it.precipChance ?: 0) < 30 }
            parts += if (stop != null) "It's $verb now and should ease off around ${stop.hourLabel}."
            else "It's $verb now and looks set to continue for most of the next ${next.size} hours."
        } else {
            parts += when {
                peak == 0 -> "It looks dry for the next ${next.size} hours, with no $kind expected."
                peak < 20 -> "It looks dry for the next ${next.size} hours — the chance of $kind never gets above $peak%."
                peak < 50 -> "There's a slight chance of $kind, highest ${at(next[peakIdx])} at $peak%. Most likely it stays dry."
                else -> {
                    val startIdx = chances.indexOfFirst { it >= 50 }
                    val lead = "${kind.replaceFirstChar { it.uppercase() }} is likely, starting ${at(next[startIdx])}"
                    if (startIdx == peakIdx) "$lead ($peak%)."
                    else "$lead and most likely ${at(next[peakIdx])} ($peak%)."
                }
            }
        }
        val amounts = listOfNotNull(
            amount(rainSum, w.precipUnit)?.let { "$it of rain" },
            amount(snowSum, w.snowUnit)?.let { "$it of snow" }
        )
        if (amounts.isNotEmpty()) {
            parts += "About ${amounts.joinToString(" and ")} is expected over that time."
        }
        return parts.joinToString(" ")
    }

    private fun alerts(w: WeatherData): String? {
        if (w.alerts.isEmpty()) return null
        val shown = w.alerts.take(3)
        val parts = shown.map { a ->
            val article = if (a.event.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "An" else "A"
            "$article ${a.event} is in effect" + (a.expiresLabel?.let { " until $it" } ?: "") + "."
        }.toMutableList()
        val extra = w.alerts.size - shown.size
        if (extra > 0) parts += "$extra more ${if (extra == 1) "is" else "are"} also active."
        parts += "Tap the alert strip on the main screen for the official details."
        return parts.joinToString(" ")
    }

    /**
     * Tomorrow, read against today rather than as a standalone set of figures — warmer or
     * cooler, wetter or drier, windier or calmer, and whether it will feel different from what
     * the thermometer says — followed by the day's own practical tip. User-requested: the
     * "3° warmer than yesterday" line in the Temperature section was the part that landed, and
     * the same kind of comparison was wanted here. Each comparison only appears when the
     * difference is big enough to notice, so an unremarkable tomorrow stays short.
     */
    private fun ahead(w: WeatherData): String? {
        val today = w.daily.firstOrNull() ?: return null
        val tomorrow = w.daily.getOrNull(1) ?: return null
        val look = Sky.of(tomorrow.icon)?.day ?: return null
        val metric = w.windUnit == "km/h"
        val pop = tomorrow.precipProbMax ?: 0
        val kind = if ((tomorrow.snowfallSum ?: 0.0) > 0 || isSnow(tomorrow.icon)) "snow" else "rain"
        val parts = mutableListOf(
            "Tomorrow looks $look, ranging from ${tomorrow.lowC}° to ${tomorrow.highC}°" +
                (if (pop >= 20) ", with a $pop% chance of $kind." else ".")
        )

        // Temperature against today — by daytime high first, falling back to the overnight low
        // when the days match but the nights don't.
        val tempGap = if (metric) 2 else 3
        val highDiff = tomorrow.highC - today.highC
        val lowDiff = tomorrow.lowC - today.lowC
        parts += when {
            highDiff >= tempGap -> "That's about $highDiff° warmer than today."
            highDiff <= -tempGap -> "That's about ${-highDiff}° cooler than today."
            lowDiff >= tempGap -> "Daytime will be much like today, but the night stays about $lowDiff° milder."
            lowDiff <= -tempGap -> "Daytime will be much like today, but the night turns about ${-lowDiff}° colder."
            else -> "Temperatures will be much the same as today."
        }

        // Whether it will feel different from the thermometer (humidity or wind chill), from
        // tomorrow's own hourly feels-like — there's no per-day humidity figure to compare.
        val feelsGap = if (metric) 3 else 5
        val feelsMax = tomorrow.dayHourly.maxOfOrNull { it.feelsLikeC }
        val feelsMin = tomorrow.dayHourly.minOfOrNull { it.feelsLikeC }
        if (feelsMax != null && feelsMax - tomorrow.highC >= feelsGap) {
            parts += "It will feel muggier than the numbers suggest — closer to $feelsMax° at the peak."
        } else if (feelsMin != null && tomorrow.lowC - feelsMin >= feelsGap) {
            parts += "It will feel colder than the numbers suggest — closer to $feelsMin° at its coldest."
        }

        val popToday = today.precipProbMax ?: 0
        if (pop >= 30 && pop - popToday >= 20) {
            parts += "${kind.replaceFirstChar { it.uppercase() }} is more likely than today."
        } else if (popToday >= 30 && popToday - pop >= 20) {
            parts += "It should be drier than today."
        }

        val windTomorrow = tomorrow.windMaxKmh
        val windToday = today.windMaxKmh
        if (windTomorrow != null && windToday != null) {
            val windGap = if (metric) 15 else 10
            val breezy = if (metric) 30 else 20
            if (windTomorrow - windToday >= windGap && windTomorrow >= breezy) {
                parts += "It will be noticeably windier than today, with winds up to $windTomorrow ${w.windUnit}."
            } else if (windToday - windTomorrow >= windGap && windToday >= breezy) {
                parts += "Winds ease off compared with today."
            }
        }

        // The day's own practical advice (WeatherRepository.buildDayOutlookTips), skipping the
        // "nothing to plan around" fillers.
        tomorrow.tips.firstOrNull { it.tone != TipTone.NEUTRAL && it.tone != TipTone.NICE }
            ?.let { parts += it.text }

        val wettest = w.daily.drop(2).maxByOrNull { it.precipProbMax ?: 0 }
        val wettestPop = wettest?.precipProbMax ?: 0
        if (wettest != null && wettestPop >= 50) {
            parts += "After that, ${wettest.dayLabel} looks like the wettest day ($wettestPop%)."
        } else if (w.daily.size > 2) {
            parts += "No day after that stands out as especially wet."
        }
        return parts.joinToString(" ")
    }

    // --- helpers ----------------------------------------------------------

    /** "around 3 PM", or "right now" for the first hourly entry (labelled "Now"). */
    private fun at(h: HourEntry): String = if (h.hourLabel == "Now") "right now" else "around ${h.hourLabel}"

    /** A total worth mentioning, formatted with its unit — null if it rounds to zero. */
    private fun amount(total: Double, unit: String): String? {
        val text = String.format(Locale.US, if (unit == "in") "%.2f" else "%.1f", total)
        return if (text.toDouble() > 0.0) "$text $unit" else null
    }

    private fun isSnow(code: Int) = code in 71..77 || code in 85..86
    private fun isPrecip(code: Int) = code in 51..67 || code in 71..77 || code in 80..86 || code in 95..99

    /** WMO code → three phrasings: as it is now, as a change to expect, and as a whole day. */
    private enum class Sky(val now: String, val then: String, val day: String) {
        CLEAR("Skies are clear", "clear up", "clear"),
        PARTLY("It's partly cloudy", "turn partly cloudy", "partly cloudy"),
        OVERCAST("It's overcast", "cloud over", "overcast"),
        FOG("It's foggy", "turn foggy", "foggy"),
        DRIZZLE("It's drizzling", "start drizzling", "drizzly"),
        RAIN("It's raining", "start raining", "rainy"),
        SNOW("It's snowing", "start snowing", "snowy"),
        THUNDER("There are thunderstorms around", "turn stormy", "stormy");

        companion object {
            fun of(code: Int): Sky? = when (code) {
                0, 1 -> CLEAR
                2 -> PARTLY
                3 -> OVERCAST
                45, 48 -> FOG
                in 51..57 -> DRIZZLE
                in 61..67, in 80..82 -> RAIN
                in 71..77, in 85..86 -> SNOW
                in 95..99 -> THUNDER
                else -> null
            }
        }
    }
}

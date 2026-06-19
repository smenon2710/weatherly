package com.example.weatherly.data.repository

import com.example.weatherly.data.model.ChatApiMessage
import com.example.weatherly.data.model.ChatCompletionRequest
import com.example.weatherly.data.model.ChatCompletionResponse
import com.example.weatherly.data.model.ChatMessage
import com.example.weatherly.data.model.ChatRole
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Talks to OpenRouter for the AI assistant. Builds a compact, current-conditions
 * weather brief from [WeatherData] and injects it as a system prompt so the model
 * can answer practical questions ("can I jog this evening?", "umbrella?", etc.)
 * grounded in the user's actual forecast.
 */
class ChatRepository {

    private val api = NetworkModule.openRouterApi

    suspend fun ask(
        history: List<ChatMessage>,
        weather: WeatherData?,
        units: UnitSystem,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "The AI assistant isn't set up yet, but the quick suggestions above still work."
                )
            )
        }

        val messages = buildList {
            add(ChatApiMessage("system", systemPrompt(weather, units)))
            // Cap context sent to the API to avoid token-limit errors on long sessions.
            history.filterNot { it.isError }.takeLast(10).forEach {
                val role = if (it.role == ChatRole.USER) "user" else "assistant"
                add(ChatApiMessage(role, it.text))
            }
        }

        try {
            val response = apiCallWithRetry(
                auth = "Bearer ${apiKey.trim()}",
                body = ChatCompletionRequest(model = model, messages = messages)
            )
            val text = response.choices?.firstOrNull()?.message?.content?.trim()
            when {
                !text.isNullOrBlank() -> Result.success(text)
                response.error?.message != null ->
                    Result.failure(IllegalStateException(response.error.message))
                else -> Result.failure(IllegalStateException("The assistant didn't return a reply."))
            }
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(httpMessage(e.code())))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Couldn't reach the assistant."))
        }
    }

    private suspend fun apiCallWithRetry(auth: String, body: ChatCompletionRequest): ChatCompletionResponse {
        repeat(2) { attempt ->
            if (attempt > 0) delay(2_000L)
            try {
                return api.chat(authorization = auth, body = body)
            } catch (e: HttpException) {
                if (e.code() != 429 || attempt == 1) throw e
            }
        }
        error("unreachable")
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> "Invalid API key. Check it in the key field above."
        402 -> "This model needs credits on your OpenRouter account, or pick a free model."
        429 -> "Rate limited by OpenRouter — wait a moment and try again."
        in 500..599 -> "OpenRouter is having trouble right now. Try again shortly."
        else -> "Request failed (HTTP $code)."
    }

    private fun systemPrompt(w: WeatherData?, units: UnitSystem): String {
        val now = SimpleDateFormat("EEEE d MMM, h:mm a", Locale.getDefault()).format(Date())
        val rules = """
            You are Weatherly's friendly weather assistant. Today is $now.
            Answer the user's question in 1-3 short sentences, in a warm, practical tone.
            Base every answer ONLY on the weather data below — do not invent numbers.
            When asked about an activity (jogging, driving, hiking, cycling, what to wear,
            carrying an umbrella), weigh temperature, feels-like, precipitation chance,
            wind, UV and air quality, then give a clear recommendation.
            Lead with the relevant conditions, then the suggestion. Keep units as shown.
            If the data doesn't cover something (e.g. days far ahead), say so briefly.
        """.trimIndent()

        val brief = w?.let { weatherBrief(it, units) }
            ?: "No live weather is loaded right now. Ask the user to open the weather screen first."

        return "$rules\n\nWEATHER DATA:\n$brief"
    }

    private fun weatherBrief(w: WeatherData, units: UnitSystem): String {
        val t = units.tempLabel // "°C" / "°F"
        val sb = StringBuilder()
        sb.appendLine("Location: ${w.locationName}")
        sb.appendLine("Now: ${w.currentTempC}$t, ${w.condition}" +
            (w.realFeelC?.let { ", feels like $it$t" } ?: "") +
            (if (w.isDay) " (daytime)" else " (night)"))
        sb.appendLine("Today: high ${w.highTodayC}$t, low ${w.lowTodayC}$t" +
            (w.comparedToYesterday?.let { ". $it" } ?: ""))

        val line = buildList {
            w.humidity?.let { add("humidity $it%") }
            w.windKmh?.let { add("wind $it ${w.windUnit}" + (w.windDir?.let { d -> " $d" } ?: "")) }
            w.windGustKmh?.let { add("gusts $it ${w.windUnit}") }
            w.cloudCoverPct?.let { add("cloud $it%") }
            w.precipMm?.let { add("precip ${it} ${w.precipUnit}") }
            w.uvIndex?.let { add("UV $it" + (w.uvLabel?.let { l -> " ($l)" } ?: "")) }
            w.aqi?.let { add("AQI $it" + (w.aqiLabel?.let { l -> " ($l)" } ?: "")) }
            w.visibility?.let { add("visibility $it ${w.visibilityUnit}") }
        }
        if (line.isNotEmpty()) sb.appendLine(line.joinToString(", "))
        if (w.sunrise != null || w.sunset != null) {
            sb.appendLine("Sunrise ${w.sunrise ?: "?"}, sunset ${w.sunset ?: "?"}")
        }

        val hours = w.hourly.take(8)
        if (hours.isNotEmpty()) {
            sb.appendLine("Next hours: " + hours.joinToString("; ") { h ->
                "${h.hourLabel} ${h.tempC}$t" + (h.precipChance?.let { " ${it}% rain" } ?: "")
            })
        }

        val days = w.daily.take(6)
        if (days.isNotEmpty()) {
            sb.appendLine("Coming days: " + days.joinToString("; ") { d ->
                "${d.dayLabel} ${d.highC}/${d.lowC}$t ${d.phrase ?: ""}" +
                    (d.precipProbMax?.let { " ${it}% rain" } ?: "")
            })
        }
        return sb.toString().trim()
    }
}

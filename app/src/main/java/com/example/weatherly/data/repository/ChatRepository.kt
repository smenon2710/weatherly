package com.example.weatherly.data.repository

import com.example.weatherly.data.model.ChatApiMessage
import com.example.weatherly.data.model.ChatCompletionRequest
import com.example.weatherly.data.model.ChatMessage
import com.example.weatherly.data.model.ChatRole
import com.example.weatherly.data.model.ChatStreamChunk
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.remote.NetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Talks to OpenRouter for the AI assistant. Exposes [askStreaming] for real-time
 * token-by-token delivery via SSE, and [simulateStreaming] for the rule-based
 * advisor so both paths feel identical to the user.
 */
class ChatRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val requestAdapter = moshi.adapter(ChatCompletionRequest::class.java)
    private val chunkAdapter = moshi.adapter(ChatStreamChunk::class.java)

    /**
     * Calls OpenRouter with [stream = true] and emits content deltas as they
     * arrive. Retries once after 2 s on HTTP 429. Throws [IllegalStateException]
     * on all other failures so the caller can surface them as an error bubble.
     */
    fun askStreaming(
        history: List<ChatMessage>,
        weather: WeatherData?,
        units: UnitSystem,
        apiKey: String,
        model: String
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "The AI assistant isn't set up yet, but the quick suggestions above still work."
            )
        }

        val messages = buildList {
            add(ChatApiMessage("system", systemPrompt(weather, units)))
            history.filterNot { it.isError }.takeLast(10).forEach {
                add(ChatApiMessage(if (it.role == ChatRole.USER) "user" else "assistant", it.text))
            }
        }

        val body = ChatCompletionRequest(model = model, messages = messages, stream = true)
        val requestBody = requestAdapter.toJson(body).toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("HTTP-Referer", "https://github.com/smenon2710/weatherly")
            .addHeader("X-Title", "SkySpeak")
            .post(requestBody)
            .build()

        var attempt = 0
        while (attempt < 2) {
            if (attempt > 0) delay(2_000L)
            val response = try {
                NetworkModule.makeStreamingCall(httpRequest).execute()
            } catch (e: Exception) {
                throw IllegalStateException(e.message ?: "Couldn't reach the assistant.")
            }

            if (response.code == 429 && attempt == 0) {
                response.close()
                attempt++
                continue
            }

            if (!response.isSuccessful) {
                response.close()
                throw IllegalStateException(httpMessage(response.code))
            }

            val source = response.body?.source()
                ?: throw IllegalStateException("Empty response from assistant.")

            try {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = chunkAdapter.fromJson(data) ?: continue
                        chunk.error?.message?.let { throw IllegalStateException(it) }
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) emit(content)
                    } catch (e: IllegalStateException) {
                        throw e
                    } catch (_: Exception) { /* skip malformed SSE chunk */ }
                }
            } finally {
                source.close()
            }
            break
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Emits [text] in small character bursts with short random delays, giving
     * rule-based advisor answers the same streaming feel as LLM responses.
     */
    fun simulateStreaming(text: String): Flow<String> = flow {
        var i = 0
        while (i < text.length) {
            val end = minOf(i + Random.nextInt(1, 4), text.length)
            emit(text.substring(i, end))
            i = end
            delay(15L + Random.nextLong(0, 20))
        }
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
            You are SkySpeak's friendly weather assistant. Today is $now.
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
        val t = units.tempLabel
        val sb = StringBuilder()
        sb.appendLine("Location: ${w.locationName}")
        sb.appendLine(
            "Now: ${w.currentTempC}$t, ${w.condition}" +
                (w.realFeelC?.let { ", feels like $it$t" } ?: "") +
                (if (w.isDay) " (daytime)" else " (night)")
        )
        sb.appendLine(
            "Today: high ${w.highTodayC}$t, low ${w.lowTodayC}$t" +
                (w.comparedToYesterday?.let { ". $it" } ?: "")
        )
        val line = buildList {
            w.humidity?.let { add("humidity $it%") }
            w.windKmh?.let { add("wind $it ${w.windUnit}" + (w.windDir?.let { d -> " $d" } ?: "")) }
            w.windGustKmh?.let { add("gusts $it ${w.windUnit}") }
            w.cloudCoverPct?.let { add("cloud $it%") }
            w.precipMm?.let { add("precip $it ${w.precipUnit}") }
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

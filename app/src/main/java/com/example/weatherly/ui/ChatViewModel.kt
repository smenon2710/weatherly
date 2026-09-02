package com.example.weatherly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.BuildConfig
import com.example.weatherly.data.advice.AdviceIntent
import com.example.weatherly.data.advice.WeatherAdvisor
import com.example.weatherly.data.model.ChatMessage
import com.example.weatherly.data.model.ChatRole
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        // A generous default, not a strict cost control — this app's own usage pattern is short,
        // infrequent sessions (see IMPROVEMENTS.md's AI Assistant strategic note), and the LLM's
        // real job is occasional multi-day/synthesis questions, not constant chatting. Chosen as
        // a real safety net against runaway cost on the shared build-time key without ever
        // realistically bothering a normal user's actual usage. Adjust if real usage shows
        // otherwise.
        private const val LLM_DAILY_CAP = 20
        private const val CAP_REACHED_MESSAGE =
            "You've reached today's limit for AI-powered answers — it resets tomorrow. I can " +
                "still help right now with quick questions like umbrella, jacket, driving, " +
                "hiking, walking, or what to wear, either typed or from the suggestions below."
        private const val OFF_TOPIC_MESSAGE =
            "I'm SkySpeak's weather assistant, so I can't help with that — but ask me anything " +
                "about the forecast, or try umbrella, jacket, driving, hiking, walking, or what " +
                "to wear below."
    }

    private val repository = ChatRepository()
    private val prefs = PreferencesStore(app)

    private val apiKey: String get() = prefs.getOpenRouterKey(BuildConfig.OPENROUTER_API_KEY)
    private val model: String get() = prefs.getEffectiveOpenRouterModel(BuildConfig.OPENROUTER_MODEL)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    /** Text being streamed into the current assistant bubble. Empty when idle. */
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private var activeStreamJob: Job? = null

    val hasKey: Boolean get() = apiKey.isNotBlank()

    fun send(text: String, weather: WeatherData?, units: UnitSystem) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _sending.value) return

        // Local-first routing: a typed question that means the same thing as one of the six
        // quick-suggestion chips (e.g. "should I bring an umbrella?") gets the identical free,
        // zero-latency local answer the chip gives, instead of always paying for an LLM call —
        // see WeatherAdvisor.matchIntent's doc comment for the matching heuristic. Falls through
        // to the real LLM call below only when nothing matches.
        val intent = WeatherAdvisor.matchIntent(prompt)
        if (intent != null) {
            sendLocal(intent, prompt, weather, units)
            return
        }

        // Obviously off-topic messages (general trivia, coding help, translation, etc.) never
        // reach the LLM at all — the system prompt's topic-scope rule would decline these anyway,
        // but only after spending a real, billed OpenRouter call to do it. See
        // WeatherAdvisor.isObviouslyOffTopic's doc comment for why this is a denylist, not an
        // allowlist (a genuinely ambiguous message still reaches the LLM as before).
        if (WeatherAdvisor.isObviouslyOffTopic(prompt)) {
            addLocalExchange(prompt, OFF_TOPIC_MESSAGE)
            return
        }

        // Daily usage cap — protects the developer's own shared build-time key from runaway
        // cost; never applies once the user has entered their own key in Settings, since there's
        // no shared-cost risk there. Redirects to the local rule engine instead of erroring out —
        // the user can still get a real answer for the six everyday questions, just not open-ended
        // LLM synthesis, until the cap resets tomorrow.
        if (!prefs.hasOwnOpenRouterKey() && prefs.getLlmUsageCountToday() >= LLM_DAILY_CAP) {
            addLocalExchange(prompt, CAP_REACHED_MESSAGE)
            return
        }

        _messages.value = _messages.value + ChatMessage(ChatRole.USER, prompt)
        _sending.value = true
        _streamingText.value = ""

        activeStreamJob = viewModelScope.launch {
            try {
                repository.askStreaming(
                    history = _messages.value,
                    weather = weather,
                    units = units,
                    apiKey = apiKey,
                    model = model
                ).collect { chunk -> _streamingText.value += chunk }

                val full = _streamingText.value
                if (full.isNotBlank()) {
                    _messages.value = _messages.value + ChatMessage(ChatRole.ASSISTANT, full)
                    // Only a completed exchange counts against the cap — a failed/errored call
                    // (network issue, invalid key, rate limit) shouldn't cost the user anything.
                    if (!prefs.hasOwnOpenRouterKey()) prefs.incrementLlmUsageToday()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.value = _messages.value +
                    ChatMessage(ChatRole.ASSISTANT, e.message ?: "Something went wrong.", isError = true)
            } finally {
                _streamingText.value = ""
                _sending.value = false
            }
        }
    }

    /**
     * Answers [question] via [WeatherAdvisor] instead of the LLM — shared by the quick-suggestion
     * chips (which already know their [AdviceIntent]) and [send]'s local-first routing (which
     * infers it from typed text). Centralizing the "no weather loaded yet" fallback here means
     * both entry points give the identical message instead of the chip's own copy in
     * `ChatScreen.kt` risking drift from this one.
     */
    fun sendLocal(intent: AdviceIntent, question: String, weather: WeatherData?, units: UnitSystem) {
        val reply = weather?.let { WeatherAdvisor.advise(intent, it, units) }
            ?: "Open the weather screen first so I can read your local conditions, then ask again."
        addLocalExchange(question, reply)
    }

    /** Streams a locally computed answer word-by-word so it feels like LLM output. */
    fun addLocalExchange(question: String, answer: String) {
        _messages.value = _messages.value + ChatMessage(ChatRole.USER, question)
        _sending.value = true
        _streamingText.value = ""

        activeStreamJob = viewModelScope.launch {
            try {
                repository.simulateStreaming(answer)
                    .collect { chunk -> _streamingText.value += chunk }

                val full = _streamingText.value
                if (full.isNotBlank()) {
                    _messages.value = _messages.value + ChatMessage(ChatRole.ASSISTANT, full)
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _streamingText.value = ""
                _sending.value = false
            }
        }
    }

    fun clear() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        _messages.value = emptyList()
        _streamingText.value = ""
        _sending.value = false
    }
}

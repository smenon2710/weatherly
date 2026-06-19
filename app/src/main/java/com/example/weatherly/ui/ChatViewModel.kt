package com.example.weatherly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.BuildConfig
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

    private val repository = ChatRepository()
    private val prefs = PreferencesStore(app)

    private val apiKey: String get() = prefs.getOpenRouterKey(BuildConfig.OPENROUTER_API_KEY)
    private val model: String get() = prefs.getOpenRouterModel(BuildConfig.OPENROUTER_MODEL)

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

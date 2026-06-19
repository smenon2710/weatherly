package com.example.weatherly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.BuildConfig
import com.example.weatherly.data.model.ChatMessage
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.model.ChatRole
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.repository.ChatRepository
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

    /** Whether the AI assistant is configured (the dev added a key before building). */
    val hasKey: Boolean get() = apiKey.isNotBlank()

    fun send(text: String, weather: WeatherData?, units: UnitSystem) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _sending.value) return

        _messages.value = _messages.value + ChatMessage(ChatRole.USER, prompt)
        _sending.value = true

        viewModelScope.launch {
            repository.ask(
                history = _messages.value,
                weather = weather,
                units = units,
                apiKey = apiKey,
                model = model
            ).onSuccess { reply ->
                _messages.value = _messages.value + ChatMessage(ChatRole.ASSISTANT, reply)
            }.onFailure { e ->
                _messages.value = _messages.value +
                    ChatMessage(ChatRole.ASSISTANT, e.message ?: "Something went wrong.", isError = true)
            }
            _sending.value = false
        }
    }

    /** Append a question and an instant, locally-computed answer (no network). */
    fun addLocalExchange(question: String, answer: String) {
        _messages.value = _messages.value +
            ChatMessage(ChatRole.USER, question) +
            ChatMessage(ChatRole.ASSISTANT, answer)
    }

    fun clear() {
        _messages.value = emptyList()
    }
}

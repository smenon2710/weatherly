package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** Who authored a chat turn. */
enum class ChatRole { USER, ASSISTANT }

/** A single message shown in the AI chat UI. */
data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val isError: Boolean = false
)

// --- OpenRouter (OpenAI-compatible) request/response models -----------------

/** One message in the wire format expected by the chat-completions endpoint. */
data class ChatApiMessage(
    val role: String,   // "system" | "user" | "assistant"
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatApiMessage>,
    @Json(name = "max_tokens") val maxTokens: Int = 500,
    val temperature: Double = 0.4,
    val stream: Boolean = false
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice>?,
    val error: ChatApiError?
)

data class ChatChoice(
    val message: ChatApiMessage?
)

data class ChatApiError(
    val message: String?,
    val code: Int? = null
)

// --- Streaming (SSE) response models ----------------------------------------

data class ChatStreamChunk(
    val choices: List<StreamChoice>?,
    val error: ChatApiError? = null
)

data class StreamChoice(
    val delta: StreamDelta?,
    @Json(name = "finish_reason") val finishReason: String? = null
)

data class StreamDelta(
    val content: String? = null
)

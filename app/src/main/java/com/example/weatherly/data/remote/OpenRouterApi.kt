package com.example.weatherly.data.remote

import com.example.weatherly.data.model.ChatCompletionRequest
import com.example.weatherly.data.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * OpenRouter chat-completions (OpenAI-compatible). Base: https://openrouter.ai/
 * The Authorization header carries the user's Bearer key and is supplied per
 * call so a runtime-entered key works without rebuilding.
 */
interface OpenRouterApi {
    @Headers("X-Title: SkySpeak")
    @POST("api/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}

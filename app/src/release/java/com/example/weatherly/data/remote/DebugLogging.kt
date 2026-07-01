package com.example.weatherly.data.remote

import okhttp3.OkHttpClient

/** No-op in release — logging-interceptor isn't on the release classpath, by design. */
internal fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder = this

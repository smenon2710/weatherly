package com.example.weatherly.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/** Compiled into debug builds only — logging-interceptor is a debugImplementation dependency. */
internal fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder = apply {
    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
}

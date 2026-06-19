package com.example.weatherly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.ui.ChatScreen
import com.example.weatherly.ui.WeatherScreen
import com.example.weatherly.ui.WeatherViewModel
import com.example.weatherly.ui.theme.WeatherlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherlyTheme {
                // Activity-scoped so the weather data + selection are shared with chat.
                val weatherViewModel: WeatherViewModel = viewModel()
                var showChat by rememberSaveable { mutableStateOf(false) }

                AnimatedContent(
                    targetState = showChat,
                    label = "screen",
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it }) togetherWith (slideOutHorizontally { -it / 4 })
                        } else {
                            (slideInHorizontally { -it / 4 }) togetherWith (slideOutHorizontally { it })
                        }
                    }
                ) { chatOpen ->
                    if (chatOpen) {
                        ChatScreen(
                            weatherViewModel = weatherViewModel,
                            onBack = { showChat = false }
                        )
                    } else {
                        WeatherScreen(
                            viewModel = weatherViewModel,
                            onOpenChat = { showChat = true }
                        )
                    }
                }
            }
        }
    }
}

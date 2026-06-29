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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.ui.ChatScreen
import com.example.weatherly.ui.RadarScreen
import com.example.weatherly.ui.WeatherScreen
import com.example.weatherly.ui.WeatherUiState
import com.example.weatherly.ui.WeatherViewModel
import com.example.weatherly.ui.theme.WeatherlyTheme

private enum class Screen { WEATHER, CHAT, RADAR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherlyTheme {
                val weatherViewModel: WeatherViewModel = viewModel()
                var screen by rememberSaveable { mutableStateOf(Screen.WEATHER) }

                val latLon by weatherViewModel.lastLatLon.collectAsStateWithLifecycle()
                val state by weatherViewModel.state.collectAsStateWithLifecycle()
                val locationName = (state as? WeatherUiState.Success)?.data?.locationName ?: ""

                AnimatedContent(
                    targetState = screen,
                    label = "screen",
                    transitionSpec = {
                        if (targetState != Screen.WEATHER) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
                        } else {
                            slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                        }
                    }
                ) { current ->
                    when (current) {
                        Screen.WEATHER -> WeatherScreen(
                            viewModel = weatherViewModel,
                            onOpenChat = { screen = Screen.CHAT },
                            onOpenRadar = { screen = Screen.RADAR }
                        )
                        Screen.CHAT -> ChatScreen(
                            weatherViewModel = weatherViewModel,
                            onBack = { screen = Screen.WEATHER }
                        )
                        Screen.RADAR -> RadarScreen(
                            lat = latLon?.first ?: 0.0,
                            lon = latLon?.second ?: 0.0,
                            locationName = locationName,
                            onBack = { screen = Screen.WEATHER }
                        )
                    }
                }
            }
        }
    }
}

package com.example.weatherly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.ui.ChatScreen
import com.example.weatherly.ui.SettingsScreen
import com.example.weatherly.ui.WeatherScreen
import com.example.weatherly.ui.WeatherViewModel
import com.example.weatherly.ui.theme.WeatherlyTheme

private enum class Screen { WEATHER, CHAT, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val weatherViewModel: WeatherViewModel = viewModel()
            val themePreference by weatherViewModel.themePreference.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> systemDark
            }

            WeatherlyTheme(darkTheme = darkTheme) {
                var screen by rememberSaveable { mutableStateOf(Screen.WEATHER) }

                val units by weatherViewModel.units.collectAsStateWithLifecycle()

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
                            onOpenSettings = { screen = Screen.SETTINGS }
                        )
                        Screen.CHAT -> ChatScreen(
                            weatherViewModel = weatherViewModel,
                            onBack = { screen = Screen.WEATHER }
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            themePreference = themePreference,
                            onThemePreferenceChange = weatherViewModel::setThemePreference,
                            units = units,
                            onUnitsChange = weatherViewModel::setUnits,
                            onBack = { screen = Screen.WEATHER }
                        )
                    }
                }
            }
        }
    }
}

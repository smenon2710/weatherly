package com.example.weatherly.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.BuildConfig
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.widget.WeatherWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesStore(app)

    /**
     * Whether an OpenRouter key is currently configured. The key's actual value is
     * never exposed here — it belongs to the developer/user who entered it, and a
     * settings field that echoed it back in plaintext (even masked-with-reveal)
     * would let anyone with the phone unlocked read and copy it out.
     */
    private val _hasOpenRouterKey =
        MutableStateFlow(prefs.hasOpenRouterKey(BuildConfig.OPENROUTER_API_KEY))
    val hasOpenRouterKey: StateFlow<Boolean> = _hasOpenRouterKey.asStateFlow()

    private val _openRouterModel =
        MutableStateFlow(prefs.getOpenRouterModel(BuildConfig.OPENROUTER_MODEL))
    val openRouterModel: StateFlow<String> = _openRouterModel.asStateFlow()

    fun saveOpenRouterKey(key: String) {
        prefs.setOpenRouterKey(key)
        _hasOpenRouterKey.value = prefs.hasOpenRouterKey(BuildConfig.OPENROUTER_API_KEY)
    }

    fun removeOpenRouterKey() {
        prefs.setOpenRouterKey("")
        _hasOpenRouterKey.value = prefs.hasOpenRouterKey(BuildConfig.OPENROUTER_API_KEY)
    }

    fun saveOpenRouterModel(model: String) {
        prefs.setOpenRouterModel(model)
        _openRouterModel.value = prefs.getOpenRouterModel(BuildConfig.OPENROUTER_MODEL)
    }

    private val _widgetTransparent = MutableStateFlow(prefs.getWidgetTransparent())
    val widgetTransparent: StateFlow<Boolean> = _widgetTransparent.asStateFlow()

    fun setWidgetTransparent(transparent: Boolean) {
        prefs.setWidgetTransparent(transparent)
        _widgetTransparent.value = transparent
        // Refresh immediately so the change is visible without waiting for the next
        // system-scheduled widget update.
        viewModelScope.launch { WeatherWidget().updateAll(getApplication()) }
    }
}

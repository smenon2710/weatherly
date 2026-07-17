package com.example.weatherly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.prefs.ForecastCache
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.WeatherRepository
import com.example.weatherly.location.LocationProvider
import com.example.weatherly.widget.WeatherWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val data: WeatherData, val cachedAt: Long? = null) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
    data object NeedsPermission : WeatherUiState
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WeatherRepository(app)
    private val locationProvider = LocationProvider(app)
    private val prefs = PreferencesStore(app)
    private val forecastCache = ForecastCache(app)

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    /** True while a pull-to-refresh (or silent background refresh) is in flight. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _units = MutableStateFlow(prefs.getUnitSystem())
    val units: StateFlow<UnitSystem> = _units.asStateFlow()

    private val _themePreference = MutableStateFlow(prefs.getThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _places = MutableStateFlow(prefs.getPlaces())
    val places: StateFlow<List<SavedPlace>> = _places.asStateFlow()

    /** null = use current device location. */
    private val _selected = MutableStateFlow(prefs.getSelected())
    val selected: StateFlow<SavedPlace?> = _selected.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SavedPlace>>(emptyList())
    val searchResults: StateFlow<List<SavedPlace>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    /** Alerts that were previously active and are no longer, awaiting user dismissal. Populated
     * by comparing each fetch's alerts against PreferencesStore's last-tracked set — see
     * trackAlertChanges(). */
    private val _resolvedAlerts = MutableStateFlow<List<TrackedAlert>>(emptyList())
    val resolvedAlerts: StateFlow<List<TrackedAlert>> = _resolvedAlerts.asStateFlow()

    init {
        // Show the last known forecast immediately so the app never opens to a blank screen.
        forecastCache.load()?.let { (data, ts) ->
            _state.value = WeatherUiState.Success(data, cachedAt = ts)
        }
        viewModelScope.launch {
            while (true) {
                delay(30 * 60 * 1000L)
                if (_state.value is WeatherUiState.Success) load(forceRefresh = true, background = true)
            }
        }
    }

    /**
     * Loads weather. When [background] is true (pull-to-refresh, periodic, or
     * on-resume while already showing data) the existing content stays on screen
     * and only the refresh indicator spins; errors are kept quiet so a transient
     * blip doesn't wipe a good screen.
     */
    fun load(forceRefresh: Boolean = false, background: Boolean = false) {
        viewModelScope.launch {
            if (background) _refreshing.value = true else _state.value = WeatherUiState.Loading
            try {
                val sel = _selected.value
                val units = _units.value
                val lat: Double
                val lon: Double
                val name: String?
                if (sel != null) {
                    lat = sel.lat; lon = sel.lon; name = sel.name
                } else {
                    val ll = locationProvider.currentLatLon()
                    if (ll == null) {
                        if (!background) {
                            _state.value = WeatherUiState.Error(
                                "Couldn't get your location. Check that location is enabled, or add a city."
                            )
                        }
                        return@launch
                    }
                    lat = ll.first; lon = ll.second; name = null
                }
                repository.getWeather(lat, lon, units, placeName = name, forceRefresh = forceRefresh)
                    .onSuccess {
                    forecastCache.save(it)
                    _state.value = WeatherUiState.Success(it)
                    trackAlertChanges(it.alerts)
                    // Keep home-screen widget in sync whenever the app fetches fresh data.
                    viewModelScope.launch { WeatherWidget().updateAll(getApplication()) }
                }
                    .onFailure {
                        if (!background || _state.value !is WeatherUiState.Success) {
                            _state.value = WeatherUiState.Error(it.message ?: "Something went wrong.")
                        }
                    }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Pull-to-refresh entry point: refresh data while keeping the screen visible. */
    fun refresh() = load(forceRefresh = true, background = true)

    /** Diffs this fetch's alerts against the last-tracked set to detect resolution (an alert
     * that was tracked but is no longer active) — covers alerts that clear silently during a
     * background refresh, not just ones the user is staring at when they expire. */
    private fun trackAlertChanges(current: List<WeatherAlert>) {
        val currentIds = current.map { it.id }.toSet()
        val previous = prefs.getTrackedAlerts()
        val newlyResolved = previous.filter { it.id !in currentIds }
        if (newlyResolved.isNotEmpty()) {
            val alreadyShown = _resolvedAlerts.value.map { it.id }.toSet()
            _resolvedAlerts.value = _resolvedAlerts.value + newlyResolved.filterNot { it.id in alreadyShown }
        }
        prefs.setTrackedAlerts(current.map { TrackedAlert(it.id, it.event) })
    }

    fun dismissResolvedAlert(id: String) {
        _resolvedAlerts.value = _resolvedAlerts.value.filterNot { it.id == id }
    }

    fun onPermissionDenied() {
        if (_selected.value != null) load() else _state.value = WeatherUiState.NeedsPermission
    }

    fun selectCurrentLocation() {
        _selected.value = null
        prefs.setSelected(null)
        resetAlertTracking()
        load(forceRefresh = true)
    }

    fun selectPlace(place: SavedPlace) {
        _selected.value = place
        prefs.setSelected(place)
        resetAlertTracking()
        load(forceRefresh = true)
    }

    /** A tracked alert from the previous location isn't a real "resolution" for the new one. */
    private fun resetAlertTracking() {
        prefs.setTrackedAlerts(emptyList())
        _resolvedAlerts.value = emptyList()
    }

    fun addPlace(place: SavedPlace) {
        val list = (_places.value + place).distinctBy { "${it.lat},${it.lon}" }
        _places.value = list
        prefs.setPlaces(list)
        _searchResults.value = emptyList()
        selectPlace(place)
    }

    fun removePlace(place: SavedPlace) {
        val list = _places.value.filterNot { it.lat == place.lat && it.lon == place.lon }
        _places.value = list
        prefs.setPlaces(list)
        if (_selected.value?.let { it.lat == place.lat && it.lon == place.lon } == true) {
            selectCurrentLocation()
        }
    }

    fun setUnits(units: UnitSystem) {
        _units.value = units
        prefs.setUnitSystem(units)
        load(forceRefresh = true)
    }

    fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
        prefs.setThemePreference(preference)
    }

    fun search(query: String) {
        viewModelScope.launch {
            _searching.value = true
            _searchResults.value = repository.searchCity(query)
            _searching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}

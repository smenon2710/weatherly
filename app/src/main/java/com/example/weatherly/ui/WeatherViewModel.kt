package com.example.weatherly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.ThemePreference
import com.example.weatherly.data.model.UnitSystem
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

    /** Last lat/lon used for a weather fetch — for centering the radar map. */
    private val _lastLatLon = MutableStateFlow<Pair<Double, Double>?>(null)
    val lastLatLon: StateFlow<Pair<Double, Double>?> = _lastLatLon.asStateFlow()

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
                _lastLatLon.value = lat to lon
                repository.getWeather(lat, lon, units, placeName = name, forceRefresh = forceRefresh)
                    .onSuccess {
                    forecastCache.save(it)
                    _state.value = WeatherUiState.Success(it)
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

    fun onPermissionDenied() {
        if (_selected.value != null) load() else _state.value = WeatherUiState.NeedsPermission
    }

    fun selectCurrentLocation() {
        _selected.value = null
        prefs.setSelected(null)
        load(forceRefresh = true)
    }

    fun selectPlace(place: SavedPlace) {
        _selected.value = place
        prefs.setSelected(place)
        load(forceRefresh = true)
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

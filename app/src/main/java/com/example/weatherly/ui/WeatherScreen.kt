package com.example.weatherly.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.ui.components.AppBackground
import com.example.weatherly.ui.components.AttributionFooter
import com.example.weatherly.ui.components.CurrentHeader
import com.example.weatherly.ui.components.Cyan
import com.example.weatherly.ui.components.DailyCard
import com.example.weatherly.ui.components.DetailSheet
import com.example.weatherly.ui.components.DetailSheetContent
import com.example.weatherly.ui.components.HourlyCard
import com.example.weatherly.ui.components.MetricsGrid
import com.example.weatherly.ui.components.TextPrimary
import com.example.weatherly.ui.components.TextSecondary
import com.example.weatherly.ui.components.TipBanner
import com.example.weatherly.ui.components.conditionGradient
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    onOpenChat: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()

    var showLocations by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.load() else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state is WeatherUiState.Success) viewModel.load(background = true)
    }

    when (val s = state) {
        is WeatherUiState.Success ->
            WeatherContent(
                data = s.data,
                cachedAt = s.cachedAt,
                isRefreshing = refreshing,
                onRefresh = { viewModel.refresh() },
                onOpenLocations = { showLocations = true },
                onOpenChat = onOpenChat
            )

        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            when (s) {
                is WeatherUiState.NeedsPermission -> CenterMessage(
                    "Weatherly needs your location to show local weather.",
                    "Grant location access",
                    onAction = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    onSecondary = { showLocations = true }
                )

                is WeatherUiState.Error -> CenterMessage(
                    s.message, "Retry",
                    onAction = { viewModel.load(forceRefresh = true) },
                    onSecondary = { showLocations = true }
                )

                else -> CircularProgressIndicator(color = TextPrimary)
            }
        }
    }

    if (showLocations) {
        ModalBottomSheet(
            onDismissRequest = {
                showLocations = false
                viewModel.clearSearch()
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LocationsSheet(
                units = units,
                selected = selected,
                places = places,
                searchResults = searchResults,
                searching = searching,
                onUnits = viewModel::setUnits,
                onSearch = viewModel::search,
                onAdd = { viewModel.addPlace(it); showLocations = false },
                onUseCurrent = { viewModel.selectCurrentLocation(); showLocations = false },
                onSelect = { viewModel.selectPlace(it); showLocations = false },
                onRemove = viewModel::removePlace
            )
        }
    }
}

@Composable
private fun LocationsSheet(
    units: UnitSystem,
    selected: SavedPlace?,
    places: List<SavedPlace>,
    searchResults: List<SavedPlace>,
    searching: Boolean,
    onUnits: (UnitSystem) -> Unit,
    onSearch: (String) -> Unit,
    onAdd: (SavedPlace) -> Unit,
    onUseCurrent: () -> Unit,
    onSelect: (SavedPlace) -> Unit,
    onRemove: (SavedPlace) -> Unit
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(400)
            onSearch(query)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text("Units", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = units == UnitSystem.METRIC,
                onClick = { onUnits(UnitSystem.METRIC) },
                label = { Text("°C · km/h") }
            )
            FilterChip(
                selected = units == UnitSystem.IMPERIAL,
                onClick = { onUnits(UnitSystem.IMPERIAL) },
                label = { Text("°F · mph") }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Add a city", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search city name") },
            trailingIcon = {
                IconButton(onClick = { onSearch(query) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        )
        if (searching) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.height(24.dp))
        }
        searchResults.forEach { place ->
            PlaceRow(
                title = place.name,
                subtitle = place.subtitle,
                onClick = { onAdd(place) }
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        PlaceRow(
            title = "Current location",
            subtitle = if (selected == null) "Selected" else null,
            leading = Icons.Filled.MyLocation,
            onClick = onUseCurrent
        )
        places.forEach { place ->
            PlaceRow(
                title = place.name,
                subtitle = if (selected?.lat == place.lat && selected?.lon == place.lon) "Selected" else place.subtitle,
                leading = Icons.Filled.Place,
                onClick = { onSelect(place) },
                onDelete = { onRemove(place) }
            )
        }
    }
}

@Composable
private fun PlaceRow(
    title: String,
    subtitle: String?,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = TextSecondary, modifier = Modifier.height(20.dp))
            Spacer(Modifier.padding(end = 12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = TextSecondary, fontSize = 13.sp)
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = TextSecondary)
            }
        }
    }
}

/** Stateless content — used by the screen and by @Preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(
    data: WeatherData,
    cachedAt: Long? = null,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onOpenLocations: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    var sheet by remember { mutableStateOf<DetailSheet?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    color = Cyan
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Condition-responsive gradient hero — sky tone at top, fades to AppBackground
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(conditionGradient(data.currentIcon, data.isDay)))
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // App bar row: locations · wordmark · chat
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .padding(top = 4.dp)
                        ) {
                            IconButton(
                                onClick = onOpenLocations,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.Filled.Place, contentDescription = "Locations", tint = Cyan)
                            }
                            // Text wordmark — adapts to theme, no PNG bleeding required
                            Text(
                                "weatherly",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 5.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(40.dp)
                                    .shadow(3.dp, CircleShape, clip = false)
                                    .clip(CircleShape)
                                    .background(Cyan)
                                    .clickable { onOpenChat() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = "Ask the weather assistant",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        CurrentHeader(data, textColor = TextPrimary, subColor = TextSecondary)
                        if (data.tips.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            data.tips.forEach { tip ->
                                TipBanner(tip)
                                Spacer(Modifier.height(10.dp))
                            }
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Cards section on the plain app background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    HourlyCard(data)
                    Spacer(Modifier.height(12.dp))
                    DailyCard(
                        data,
                        onDayClick = { sheet = DetailSheet.Day(it, data.windUnit, data.precipUnit) }
                    )
                    Spacer(Modifier.height(12.dp))
                    MetricsGrid(data, onMetricClick = { sheet = it })
                    if (cachedAt != null) {
                        val agoText = remember(cachedAt) {
                            val agoMin = (System.currentTimeMillis() - cachedAt) / 60_000
                            if (agoMin < 1) "just now" else "${agoMin}m ago"
                        }
                        Text(
                            "Showing data from $agoText · pull to refresh",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                    AttributionFooter(textColor = TextSecondary)
                }
            }
        }
    }

    sheet?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DetailSheetContent(current)
        }
    }
}

@Composable
private fun CenterMessage(
    message: String,
    action: String,
    onAction: () -> Unit,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text(message, color = TextPrimary, textAlign = TextAlign.Center)
        Button(onClick = onAction) { Text(action) }
        if (onSecondary != null) {
            TextButton(onClick = onSecondary) { Text("Or choose a city") }
        }
    }
}

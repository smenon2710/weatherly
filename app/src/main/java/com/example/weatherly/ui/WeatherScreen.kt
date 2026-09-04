package com.example.weatherly.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.togetherWith
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.ui.components.AlertBannerList
import com.example.weatherly.ui.components.ResolvedAlertCard
import com.example.weatherly.ui.components.AppBackground
import com.example.weatherly.ui.components.AttributionFooter
import com.example.weatherly.ui.components.CurrentHeader
import com.example.weatherly.ui.components.DayDetailBody
import com.example.weatherly.ui.components.Cyan
import com.example.weatherly.ui.components.DailyCard
import com.example.weatherly.ui.components.DetailSheet
import com.example.weatherly.ui.components.DetailSheetContent
import com.example.weatherly.ui.components.HourlyCard
import com.example.weatherly.ui.components.MetricsGrid
import com.example.weatherly.ui.components.TextPrimary
import com.example.weatherly.ui.components.TextSecondary
import com.example.weatherly.ui.components.WeatherBackground
import com.example.weatherly.ui.components.heroBackdropIsDark
import com.example.weatherly.ui.components.heroTextColors
import com.example.weatherly.ui.components.heroWeight
import com.example.weatherly.util.rememberLocalTimeText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    onOpenChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val resolvedAlerts by viewModel.resolvedAlerts.collectAsStateWithLifecycle()

    var showLocations by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Already-granted permission resolves near-instantly here, including on every
            // return from Chat/Settings (WeatherScreen re-enters composition each time). Use a
            // background load when content is already on screen so it isn't wiped by a full
            // Loading state, mirroring the ON_RESUME effect below.
            viewModel.load(background = state is WeatherUiState.Success)
        } else {
            viewModel.onPermissionDenied()
        }
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
                onOpenChat = onOpenChat,
                onOpenSettings = onOpenSettings,
                resolvedAlerts = resolvedAlerts,
                onDismissResolved = { viewModel.dismissResolvedAlert(it) }
            )

        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            when (s) {
                is WeatherUiState.NeedsPermission -> CenterMessage(
                    "SkySpeak needs your location to show local weather.",
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
                selected = selected,
                places = places,
                searchResults = searchResults,
                searching = searching,
                currentLocationTimezone = (state as? WeatherUiState.Success)?.data?.timezone,
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
    selected: SavedPlace?,
    places: List<SavedPlace>,
    searchResults: List<SavedPlace>,
    searching: Boolean,
    // The currently-loaded weather data's own timezone, only meaningful for the "Current
    // location" row: it's only known once that location has actually been fetched (there's no
    // geocoding result for "wherever the device is" ahead of time), and only actually describes
    // current location when no saved place is selected — see the call site for the guard.
    currentLocationTimezone: String?,
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
                timezone = place.timezone,
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
            timezone = if (selected == null) currentLocationTimezone else null,
            onClick = onUseCurrent
        )
        places.forEach { place ->
            PlaceRow(
                title = place.name,
                subtitle = if (selected?.lat == place.lat && selected?.lon == place.lon) "Selected" else place.subtitle,
                leading = Icons.Filled.Place,
                timezone = place.timezone,
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
    timezone: String? = null,
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
        rememberLocalTimeText(timezone)?.let {
            Text(it, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = TextSecondary)
            }
        }
    }
}

/** Stateless content — used by the screen and by @Preview. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WeatherContent(
    data: WeatherData,
    cachedAt: Long? = null,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onOpenLocations: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    resolvedAlerts: List<TrackedAlert> = emptyList(),
    onDismissResolved: (String) -> Unit = {}
) {
    var sheet by remember { mutableStateOf<DetailSheet?>(null) }
    // Tapping a day in the 7-day forecast expands that row into a full detail view via a shared-
    // element transition, instead of opening DetailSheet.Day as a ModalBottomSheet the way every
    // other detail view still does — see DailyCard/ExpandedDayDetail below.
    var expandedDay by remember { mutableStateOf<DayEntry?>(null) }
    BackHandler(enabled = expandedDay != null) { expandedDay = null }

    // Hero text sits directly on WeatherBackground's animated scene with no card/scrim behind it,
    // so its color has to be calibrated against that scene rather than the fixed app-wide
    // TextPrimary/TextSecondary tokens — see heroTextColors' doc comment for the on-device
    // legibility bug (Franklin Park, NJ, light mode, Overcast) this fixes.
    val heroIsDark = remember(data.currentIcon, data.isDay, data.alerts) {
        heroBackdropIsDark(data.currentIcon, data.isDay, data.alerts)
    }
    val (heroPrimary, heroSecondary) = heroTextColors(heroIsDark)

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = expandedDay,
            label = "dayDetailTransition",
            // AnimatedContent's default behavior cross-fades the ENTIRE outgoing screen with the
            // ENTIRE incoming screen — visible as both screens' text blended/overlapping mid-
            // transition (confirmed as a real on-device issue, not a screenshot-timing artifact
            // as first assumed). Disabling both here means the only thing that visibly animates
            // is the shared-bounds element itself (the tapped row growing into the detail
            // header); everything else in each screen appears/disappears immediately rather than
            // fading through the other screen.
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            modifier = Modifier.fillMaxSize()
        ) { day ->
            if (day == null) {
                with(this@SharedTransitionLayout) {
                    WeatherContentBody(
                        data = data, cachedAt = cachedAt, isRefreshing = isRefreshing,
                        onRefresh = onRefresh, resolvedAlerts = resolvedAlerts,
                        onDismissResolved = onDismissResolved,
                        onOpenLocations = onOpenLocations, onOpenChat = onOpenChat,
                        onOpenSettings = onOpenSettings,
                        heroPrimary = heroPrimary, heroSecondary = heroSecondary,
                        sheet = sheet, onSheetChange = { sheet = it },
                        onDayClick = { expandedDay = it },
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            } else {
                with(this@SharedTransitionLayout) {
                    ExpandedDayDetail(
                        day = day,
                        windUnit = data.windUnit,
                        precipUnit = data.precipUnit,
                        animatedVisibilityScope = this@AnimatedContent,
                        onBack = { expandedDay = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.WeatherContentBody(
    data: WeatherData,
    cachedAt: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    resolvedAlerts: List<TrackedAlert>,
    onDismissResolved: (String) -> Unit,
    onOpenLocations: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    heroPrimary: Color,
    heroSecondary: Color,
    sheet: DetailSheet?,
    onSheetChange: (DetailSheet?) -> Unit,
    onDayClick: (DayEntry) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen animated scene (rain/snow/fog/clouds/etc., condition + time-of-day driven) —
        // sits behind the entire scrolling content, not just the hero. Cards below are fully
        // opaque (see GlassCard's doc comment for why a translucent card fill was reverted); the
        // background shows through in the hero and in the gaps/margins around cards instead.
        WeatherBackground(
            code = data.currentIcon,
            isDay = data.isDay,
            aqi = data.aqi,
            cloudCoverPct = data.cloudCoverPct,
            visibility = data.visibility,
            visibilityUnit = data.visibilityUnit,
            windKmh = data.windKmh,
            windGustKmh = data.windGustKmh,
            alerts = data.alerts,
            modifier = Modifier.fillMaxSize()
        )
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
                // Hero content sits directly over WeatherBackground's full-screen animated scene —
                // no gradient background of its own anymore (see the Box above this Column).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // App bar row: standard toolbar idiom — leading/trailing icon groups size to
                        // their own content, and only the wordmark gets weight(1f), so it always gets
                        // whatever width is left over rather than being capped to a fixed fraction of
                        // the screen (an earlier equal-weight-on-all-three-slots version capped the
                        // wordmark to exactly 1/3 of the screen width, which was narrow enough to wrap
                        // "skyspeak" onto a second line on some phones).
                        // Icons are grouped by function, not by which side has room: Locations + Settings
                        // are both "configuration" actions (plain icon, low visual weight); Chat is the
                        // app's single "feature" action (tinted/filled chip, higher weight). Putting
                        // Settings there would give a once-in-a-while action the same visual prominence
                        // as the app's primary CTA.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onOpenLocations) {
                                    Icon(Icons.Filled.Place, contentDescription = "Locations", tint = Cyan)
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Cyan)
                                }
                            }
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Two-tone wordmark: "sky" recedes, "speak" steps forward. Uses the
                                // same hero-calibrated colors as CurrentHeader below, not the raw
                                // app-wide tokens — it sits on the identical animated backdrop.
                                val skyColor = heroSecondary
                                val speakColor = heroPrimary
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = skyColor)) { append("sky") }
                                        withStyle(SpanStyle(color = speakColor)) { append("speak") }
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = heroWeight(FontWeight.Light),
                                    letterSpacing = 5.sp,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
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
                        CurrentHeader(
                            data, textColor = heroPrimary, subColor = heroSecondary,
                            onForecastClick = {
                                onSheetChange(DetailSheet.Forecast(
                                    headline = data.headline ?: data.comparedToYesterday ?: "No notable changes expected.",
                                    pressureDropAlert = data.pressureDropAlert,
                                    currentPressureHpa = data.pressureHpa,
                                    pressureTrend6h = data.pressureTrend6h
                                ))
                            }
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Cards section — fully opaque (see GlassCard's doc comment on why a translucent
                // fill was tried and reverted); WeatherBackground still shows through in the
                // hero above and in the gaps/margins around these cards.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Official NWS advisories and resolved-alert acknowledgments render first here
                    // — after two reverted attempts at making the alert visually exceptional
                    // (full-bleed, or placed above the hero), consistency with the rest of the
                    // app's card flow turned out to matter more than standing out. They're no
                    // longer full GlassCards themselves (see AlertBannerList's doc comment for
                    // why — a compact single-line severity strip now, not a fourth axis of "make
                    // it different"), but they still live in this same ordinary position in the
                    // flow rather than anywhere exceptional. Active alerts render above any
                    // resolved notices, since an ongoing hazard is more important than an
                    // acknowledgment.
                    if (data.alerts.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        AlertBannerList(
                            alerts = data.alerts,
                            onAlertClick = { onSheetChange(DetailSheet.Alert(it)) },
                            onMoreClick = { onSheetChange(DetailSheet.AlertList(it)) }
                        )
                    }
                    resolvedAlerts.forEach { resolved ->
                        Spacer(Modifier.height(12.dp))
                        ResolvedAlertCard(resolved = resolved, onDismiss = { onDismissResolved(resolved.id) })
                    }
                    Spacer(Modifier.height(12.dp))
                    HourlyCard(data)
                    Spacer(Modifier.height(12.dp))
                    DailyCard(
                        data,
                        onDayClick = onDayClick,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    Spacer(Modifier.height(12.dp))
                    MetricsGrid(data, onMetricClick = { onSheetChange(it) })
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
        // skipPartiallyExpanded: this sheet's content is always vertical-scrollable
        // (DetailSheetContent), and the intermediate "partially expanded" stop fights with
        // that inner scroll's own nested-scroll handling — dragging past it visibly jumps/
        // bounces once the content is at (or near) its own scroll bounds, worst on a metric
        // like Humidity whose content height sits right at that boundary. Going straight to
        // fully expanded removes the stop that causes the tug-of-war.
        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onSheetChange(null) },
            sheetState = detailSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DetailSheetContent(current, onAlertSelected = { onSheetChange(DetailSheet.Alert(it)) })
        }
    }
}

/**
 * The shared-element destination for tapping a day in the 7-day forecast — grows from that row
 * (see DailyCard's matching `Modifier.sharedBounds` with the same "day-${day.dayLabel}" key)
 * into this full-screen view rather than a sheet sliding up. Content itself is `DayDetailBody`,
 * the same body `DetailSheet.Day`'s bottom sheet used to render before this feature replaced it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ExpandedDayDetail(
    day: DayEntry,
    windUnit: String,
    precipUnit: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .sharedBounds(
                rememberSharedContentState(key = "day-${day.dayLabel}"),
                animatedVisibilityScope = animatedVisibilityScope,
                // The row's compact Row layout and this full-screen Column have nothing in
                // common structurally, so the default ResizeMode (RemeasureToBounds) — which
                // relays out the child at every intermediate size during the animation — looks
                // janky. ScaleToBounds renders each end's own layout once and scales/crossfades
                // between them, which suits structurally-different content like this.
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
            .background(AppBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        DayDetailBody(day, windUnit, precipUnit)
        Spacer(Modifier.height(28.dp))
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

# Weatherly — Improvements

Fixes and features ordered by effort. Items within each tier are independent.

## Status

### Completed

| # | Title | Tier |
|---|---|---|
| 1 | Clear stale search results on sheet dismiss | 1 |
| 2 | Cap chat history sent to OpenRouter | 1 |
| 3 | Wire on-device OpenRouter key into `ChatViewModel` | 1 |
| 4 | Fix deprecated `Geocoder.getFromLocation()` | 1 |
| 5 | Move auto-refresh timer into `WeatherViewModel` | 1 |
| 6 | Dark theme | 2 |
| 6a | Dark theme — `TipBanner` colours | polish |
| 6b | Dark theme — logo per-theme assets with edge fade | polish |
| 6c | Dark theme — detail sheet hardcoded `Color.White` | polish |
| 7 | Debounced city search | 2 |
| 8 | Unit tests for `WeatherAdvisor` | 2 |
| 9 | Auto-retry on 429 in `ChatRepository` | 2 |
| 12 | Text wordmark replaces PNG logo | design |
| 13 | Condition-responsive hero gradient | design |
| 14 | `CurrentHeader` hierarchy fix — temp as hero | design |
| 15 | `HourlyCard` "Now" anchor + H/L summary | design |
| 16 | Remove redundant `TemperatureChartCard` | design |

### Completed — Priority 1

| # | Title |
|---|---|
| 17 | Wrap `Previews.kt` composables in `WeatherlyTheme` |
| 18 | Widget background colour synced to design system (`#6B86A3`) |
| 19 | Pull-to-refresh indicator colour explicitly set to `Cyan` |
| 20 | `forecastDays` reduced from 10 → 7 |
| 21 | Dead `TemperatureChartCard` + `TemperatureChart` composables removed |
| — | Streaming chat: LLM answers stream via SSE; rule-based answers simulate streaming |

### Completed — Priority 2

| # | Title |
|---|---|
| 10 | Offline last-known forecast (`ForecastCache` + stale label) |
| 23 | ChatScreen: compact weather context strip |
| 26 | `WeatherGlyph` accessibility content descriptions |
| 27 | `TipBanner`: left-border annotation style |

### Completed — Priority 3

| # | Title |
|---|---|
| 11 | Adaptive launcher icon (warm-gold sun on deep navy, XML only — minSdk 26) |

### Pending — Priority 2 (medium effort, meaningful UX improvement)

| # | Title | Effort |
|---|---|---|
| 22 | Chat suggestion chips: semantic colour tinting | 1 h |
| 24 | MetricsGrid: primary strip + grouped secondary | 2 h |
| 25 | In-app OpenRouter key + model settings UI | 2–3 h (security trade-off — deferred) |

### Pending — Priority 3 (larger scope)

| # | Title | Effort |
|---|---|---|
| 28 | Localization: replace hardcoded strings with `strings.xml` | 1–2 days |
| 29 | Weather-change push notification | 1–2 days |
| 30 | Share current weather | half-day |

### Deferred

| # | Title | Note |
|---|---|---|
| — | Field naming cleanup (`currentTempC` → `currentTemp` etc.) | Rename across ~15 files; safe but tedious |
| — | `WeatherAdvisor` additional intents | Cycling, gardening, outdoor events |

---

## Tier 1 — Drop-in fixes ✅ All implemented

### 1. ✅ Clear stale search results when the location sheet closes

**Problem**  
`viewModel.clearSearch()` was never called when the bottom sheet was dismissed, so old city-search results reappeared the next time the user opened the sheet.

**File:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**Implemented change:**
```kotlin
ModalBottomSheet(
    onDismissRequest = {
        showLocations = false
        viewModel.clearSearch()   // ← added
    },
    containerColor = MaterialTheme.colorScheme.surface
) { ... }
```

---

### 2. ✅ Cap chat history sent to OpenRouter

**Problem**  
`ChatRepository.ask()` sent the entire conversation history on every request, risking silent token-limit failures on long sessions.

**File:** `app/src/main/java/com/example/weatherly/data/repository/ChatRepository.kt`

**Implemented change:**
```kotlin
// Cap context sent to the API to avoid token-limit errors on long sessions.
history.filterNot { it.isError }.takeLast(10).forEach { ... }
```

The full message list still displays in the UI — only the API payload is capped.

---

### 3. ✅ Wire the on-device OpenRouter key into `ChatViewModel`

**Problem**  
`PreferencesStore` had `getOpenRouterKey(buildDefault)` implemented but `ChatViewModel` read only from `BuildConfig`, ignoring on-device key storage entirely.

**File:** `app/src/main/java/com/example/weatherly/ui/ChatViewModel.kt`

**Implemented change:**
```kotlin
private val prefs = PreferencesStore(app)
// get() properties ensure the live value is read on every call, not snapshotted at init.
private val apiKey: String get() = prefs.getOpenRouterKey(BuildConfig.OPENROUTER_API_KEY)
private val model:  String get() = prefs.getOpenRouterModel(BuildConfig.OPENROUTER_MODEL)
```

`BuildConfig` values remain the fallback when no on-device key has been set. To expose key editing in the chat UI, call `prefs.setOpenRouterKey(key)` from a future settings screen.

---

### 4. ✅ Fix the deprecated `Geocoder.getFromLocation()` call

**Problem**  
The synchronous `Geocoder.getFromLocation()` overload is deprecated on API 33+. The `@Suppress("DEPRECATION")` annotation hid the warning without fixing it.

**File:** `app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt`

**Implemented change:** `reverseGeocode` is now `suspend` and branches on SDK version. The API 33+ path uses `suspendCoroutine` with a full `GeocodeListener` object (both `onGeocode` and `onError` callbacks) to guarantee the coroutine always resumes:

```kotlin
private suspend fun reverseGeocode(lat: Double, lon: Double): String {
    return try {
        if (!Geocoder.isPresent()) return "Current location"
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(results: List<android.location.Address>) {
                        cont.resume(results.firstOrNull())
                    }
                    override fun onError(errorMessage: String?) { cont.resume(null) }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        }
        address ?: return "Current location"
        listOfNotNull(address.locality ?: address.subAdminArea, address.adminArea)
            .distinct().joinToString(", ").ifBlank { "Current location" }
    } catch (e: Exception) { "Current location" }
}
```

---

### 5. ✅ Move the auto-refresh timer into `WeatherViewModel`

**Problem**  
The 30-minute auto-refresh lived in a `LaunchedEffect(Unit)` inside `WeatherScreen`. It was cancelled every time the user navigated to the chat screen and reset to 30 minutes on return.

**Files:**  
- `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt` — timer removed  
- `app/src/main/java/com/example/weatherly/ui/WeatherViewModel.kt` — timer added to `init`

**Implemented change — removed from `WeatherScreen`:**
```kotlin
// Deleted entirely:
LaunchedEffect(Unit) {
    while (true) {
        delay(30 * 60 * 1000L)
        if (state is WeatherUiState.Success) viewModel.load(forceRefresh = true, background = true)
    }
}
```

**Added to `WeatherViewModel.init`:**
```kotlin
init {
    viewModelScope.launch {
        while (true) {
            delay(30 * 60 * 1000L)
            if (_state.value is WeatherUiState.Success) load(forceRefresh = true, background = true)
        }
    }
}
```

The timer now survives screen transitions because `viewModelScope` is tied to the ViewModel lifecycle, not the composable.

---

## Tier 2 — Medium effort ✅ All implemented

### 6. ✅ Dark theme

**Problem**  
The app used a single hardcoded light colour scheme. System dark-mode was ignored.

**Files changed:**
- `app/src/main/java/com/example/weatherly/ui/theme/Theme.kt`
- `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`
- `app/src/main/java/com/example/weatherly/ui/ChatScreen.kt`
- `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**`Theme.kt`** — added `DarkColors` and wired `isSystemInDarkTheme()`:
```kotlin
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF7FA3C2),
    onPrimary        = Color(0xFF0F1923),
    background       = Color(0xFF0F1923),   // deep navy
    surface          = Color(0xFF1A2530),   // card surfaces
    onBackground     = Color(0xFFE0E6ED),   // primary text
    onSurface        = Color(0xFFE0E6ED),
    onSurfaceVariant = Color(0xFF8A9BAD)    // secondary text
)

@Composable
fun WeatherlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
```

**`WeatherComponents.kt`** — colour constants converted to `@Composable` getters:
```kotlin
val AppBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val TextPrimary:   Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
```

`GlassCard` now reads its fill and border from the scheme:
```kotlin
val actualFill = if (fill == Color.Unspecified) MaterialTheme.colorScheme.surface else fill
val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
```

`tipColors` received a `primaryText: Color` parameter to avoid calling the `@Composable` getter from a non-composable context. `TempRangeBar` captures `dotColor = TextPrimary` before the Canvas block for the same reason.

**`ChatScreen.kt`** — removed top-level `ChatBg`/`HeaderSurface` vals; all `Color.White` card/bubble surfaces replaced with `MaterialTheme.colorScheme.surface`.

**`WeatherScreen.kt`** — both `ModalBottomSheet` container colours updated to `MaterialTheme.colorScheme.surface`.

**Note:** `WeatherWidget.kt` uses Jetpack Glance and has its own colour system — unchanged intentionally.

To verify: toggle system dark mode or use the existing `WeatherNightPreview` in `Previews.kt` (it uses `uiMode = UI_MODE_NIGHT_YES` and now reflects the real dark scheme).

---

### 6a. ✅ Dark theme — `TipBanner` colours

**Problem**  
`tipColors()` returned hardcoded light pastel backgrounds for all tones (e.g. `#EFE4D0`). In dark mode these light pills appeared jarring against the deep navy background, and text was unreadable.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

**Implemented change:** Promoted `tipColors()` to a `@Composable` function using `isSystemInDarkTheme()`. Each tone now has a distinct dark-mode pair — a deeply tinted background and a lighter, saturated foreground text:

```kotlin
@Composable
private fun tipColors(tone: TipTone): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return when (tone) {
        TipTone.HOT ->
            if (isDark) Color(0xFF2C1A06) to Color(0xFFE8BE7A)
            else Color(0xFFEFE4D0) to Color(0xFF6E5C3C)
        TipTone.RAIN ->
            if (isDark) Color(0xFF0D1E2E) to Color(0xFF7FA8C9)
            else Color(0xFFDCE6EF) to Color(0xFF3F5670)
        // … all 7 tones
    }
}
```

The `primaryText: Color` parameter was removed; `TextPrimary` is now read directly inside the composable.

---

### 6b. ✅ Dark theme — logo per-theme assets with edge fade

**Problem**  
The original `weatherly_logo.png` had a white background baked in. In dark mode the logo appeared as a white rectangle. Multiple blend-mode approaches (ColorMatrix, BlendMode.Multiply) either hid the logo entirely or left visible artefacts.

**Files changed:**
- `app/src/main/res/drawable/weatherly_logo.png` — **deleted**
- `app/src/main/res/drawable/weatherly_logo_light.png` — new asset for light mode
- `app/src/main/res/drawable/weatherly_logo_dark.png` — new asset for dark mode
- `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**Asset preparation:** Both PNGs were processed with a Python/Pillow script that applies a 10% cosine-eased alpha fade on all four edges. This makes the rectangular boundary invisible regardless of the background colour — the logo content stays fully opaque in the centre and dissolves to transparent at the edges.

**`WeatherScreen.kt`** — `Image` selects the asset based on `isSystemInDarkTheme()`:
```kotlin
val isDark = isSystemInDarkTheme()
// …
Image(
    painter = painterResource(
        if (isDark) R.drawable.weatherly_logo_dark
        else R.drawable.weatherly_logo_light
    ),
    contentDescription = "Weatherly",
    contentScale = ContentScale.Fit,
    modifier = Modifier.align(Alignment.Center).height(60.dp)
)
```

---

### 6c. ✅ Dark theme — detail sheet hardcoded `Color.White`

**Problem**  
The metrics/day detail `ModalBottomSheet` used `containerColor = Color.White`, so it showed a white popup in dark mode.

**File:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**Implemented change:**
```kotlin
// Before
ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Color.White)
// After
ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surface)
```

---

### 7. ✅ Debounced city search

**Problem**  
City search required manually pressing a search icon button, adding unnecessary friction.

**File:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt` (`LocationsSheet`)

**Implemented change:** Added `LaunchedEffect(query)` immediately after the `query` state declaration:
```kotlin
var query by remember { mutableStateOf("") }
LaunchedEffect(query) {
    if (query.length >= 2) {
        delay(400)
        onSearch(query)
    }
}
```

The existing search icon button is kept as a manual trigger. The `LaunchedEffect` is automatically cancelled and restarted on each keystroke, so the API is called only after the user pauses for 400 ms.

---

### 8. ✅ Unit tests for `WeatherAdvisor`

**Problem**  
`WeatherAdvisor` contained branching threshold logic with no automated coverage. Changing a threshold could silently break another scenario.

**New file:** `app/src/test/java/com/example/weatherly/data/advice/WeatherAdvisorTest.kt`

25 tests covering all 6 intents (UMBRELLA, JACKET, WALKING, DRIVING, HIKING, CLOTHING) plus imperial unit thresholds. The file uses a `weather()` helper that constructs a minimal `WeatherData` with sensible defaults, keeping each test concise.

**`app/build.gradle.kts`** — added test dependency:
```kotlin
testImplementation("junit:junit:4.13.2")
```

Run with: `./gradlew test`

---

### 9. ✅ Auto-retry on 429 (rate limit) in `ChatRepository`

**Problem**  
A transient HTTP 429 from OpenRouter immediately surfaced an error to the user, even though a short wait usually resolves it.

**File:** `app/src/main/java/com/example/weatherly/data/repository/ChatRepository.kt`

**Implemented change:** Extracted `apiCallWithRetry()` — retries once after 2 seconds on 429; any other HTTP error is rethrown immediately:
```kotlin
private suspend fun apiCallWithRetry(auth: String, body: ChatCompletionRequest): ChatCompletionResponse {
    repeat(2) { attempt ->
        if (attempt > 0) delay(2_000L)
        try {
            return api.chat(authorization = auth, body = body)
        } catch (e: HttpException) {
            if (e.code() != 429 || attempt == 1) throw e
        }
    }
    error("unreachable")
}
```

`ask()` calls `apiCallWithRetry()` instead of `api.chat()` directly.

---

## Design polish ✅ All implemented

### 12. ✅ Text wordmark replaces PNG logo

**Problem**
The `original_weatherly_logo_upgraded.png` asset had its own dark-teal background baked in, which bled visibly in light mode and fought the new gradient hero in dark mode. Multiple blending workarounds (per-theme assets, edge-fade processing) added asset-maintenance burden without fixing the root cause.

**Files changed:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**Implemented change:** The `Image` composable is replaced with a `Text` wordmark:
```kotlin
Text(
    "weatherly",
    color = TextPrimary,
    fontSize = 20.sp,
    fontWeight = FontWeight.Light,
    letterSpacing = 5.sp,
    modifier = Modifier.align(Alignment.Center)
)
```
Resolves colour from `TextPrimary` (`MaterialTheme.colorScheme.onBackground`), so it is fully theme-aware with no per-mode assets needed.

---

### 13. ✅ Condition-responsive hero gradient

**Problem**
The weather hero section looked identical regardless of conditions — clear sunny days and thunderstorms showed the same cream/navy background. The app communicated *what* the weather was but not *what it felt like*.

**Files changed:**
- `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt` — new `conditionGradient()` function
- `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt` — hero Box uses the gradient

**Implemented change:** `conditionGradient(code: Int, isDay: Boolean): List<Color>` maps WMO condition codes to a sky-toned two-stop gradient (sky colour at top → `MaterialTheme.colorScheme.background` at bottom). The gradient is applied to the hero Box wrapping the header row, `CurrentHeader`, and `TipBanner`s. Condition-to-colour mapping:

| Condition | Light sky | Dark sky |
|---|---|---|
| Clear day | `#B8D8F0` (pale blue) | `#102030` |
| Clear night | `#1A2448` (deep indigo) | `#04091A` |
| Rain / drizzle | `#BFD4E6` (slate blue) | `#0E1C2A` |
| Snow | `#D0E0EE` (cold white-blue) | `#1A2230` |
| Thunder | `#3A2F50` (dark violet) | `#1C1230` |
| Fog | `#CDD3D8` (flat grey) | `#18202A` |
| Overcast | `#C8D2DC` (grey-blue) | `#141C24` |

---

### 14. ✅ `CurrentHeader` hierarchy fix — temperature as hero

**Problem**
The large weather glyph (76 dp) sat between the location name and the temperature, breaking the most important visual relationship on the screen. Multiple elements competed at SemiBold / similar sizes with no clear winner.

**Files changed:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

**Implemented change:** Element order and weights rebuilt from scratch:
1. Location — 12 sp, Medium, 2 sp letter-spacing, uppercase (`TextSecondary`)
2. `[glyph 20dp]  Condition text` — small glyph inline with condition, 15 sp Normal (`TextSecondary`)
3. **Temperature — 96 sp Thin (`TextPrimary`) — the undisputed hero**
4. `H:xx°  ·  L:xx°` — 15 sp Normal (`TextSecondary`)
5. `Feels like xx°` — 13 sp Normal (`TextSecondary`)
6. Comparison pill — 12 sp Normal, 10% alpha background

The 76 dp standalone glyph is removed from the hero entirely.

---

### 15. ✅ `HourlyCard` "Now" anchor + H/L summary

**Problem**
The current hour (`"Now"`) rendered identically to future hours — users had to read the label to find the present moment. The section label gave no temperature range context.

**Files changed:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

**Implemented change:**
- Section label row shows `"Next N hours"` on the left and `"H:xx°  L:xx°"` on the right.
- `"Now"` column renders at `TextPrimary + Bold`; all future hours at `TextSecondary + Normal`.

---

### 16. ✅ Remove redundant `TemperatureChartCard`

**Problem**
`TemperatureChartCard` plotted the same 24-hour temperatures already shown in `HourlyCard`, with the same time labels duplicated on the x-axis. It added ~120 dp of scroll depth with no new information.

**Files changed:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

**Implemented change:** `TemperatureChartCard` is no longer called from `WeatherContent`. The H/L temperature range that was the chart's only unique value is now shown inline in the `HourlyCard` section label (see item 15). The `TemperatureChartCard` composable is retained in `WeatherComponents.kt` for potential future use (e.g., standalone chart in a day detail sheet).

---

## Tier 3 — Larger features ⬜ Pending

### 10. Offline last-known forecast (survive cold start with no network)

**Problem**  
A cold start with no network connection shows an error screen. Persisting the last successful forecast lets the app open instantly with cached data and refresh silently in the background.

**Approach:** Use `SharedPreferences` + Moshi (already available) to store the last `WeatherData` as JSON. No new dependency needed.

**New file:** `app/src/main/java/com/example/weatherly/data/prefs/ForecastCache.kt`

```kotlin
class ForecastCache(context: Context) {
    private val prefs = context.getSharedPreferences("forecast_cache", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WeatherData::class.java)

    fun save(data: WeatherData) {
        prefs.edit()
            .putString("data", adapter.toJson(data))
            .putLong("ts", System.currentTimeMillis())
            .apply()
    }

    fun load(): Pair<WeatherData, Long>? {
        val json = prefs.getString("data", null) ?: return null
        val ts   = prefs.getLong("ts", 0L)
        return runCatching { adapter.fromJson(json)!! to ts }.getOrNull()
    }
}
```

**In `WeatherViewModel`:**
1. On `init`, load the cache and emit `WeatherUiState.Success` immediately if a cached value exists.
2. Then kick off a background network refresh regardless.
3. Show a "Last updated X minutes ago" label when data is stale (> 30 min).

**In `WeatherContent`:** Add a small timestamp line below the headline when showing cached data.

---

### 11. Adaptive launcher icon ⬜ Pending

**Problem**  
The manifest has no `android:icon`, so the app uses the AOSP default icon on the home screen.

**Approach**

1. Export `weatherly_logo.png` as two drawables:
   - `res/drawable/ic_launcher_foreground.xml` — logo mark, centred within the 66% safe zone
   - `res/drawable/ic_launcher_background.xml` — solid brand colour `#12A5C9` or a gradient

2. Create `res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

3. Add standard PNG densities (`mdpi` → `xxxhdpi`) for pre-API 26 devices.

4. Update `AndroidManifest.xml`:
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>
```

Android Studio's **Image Asset Studio** (right-click `res` → New → Image Asset) automates steps 1–3 from the source PNG.

---

---

## Priority 1 — Quick fixes

### 17. Wrap `Previews.kt` composables in `WeatherlyTheme`

**Problem**
`WeatherDayPreview` and `WeatherNightPreview` call `WeatherContent` directly without a `WeatherlyTheme` wrapper. `conditionGradient`, `AppBackground`, `TextPrimary`, and `TextSecondary` all read from `MaterialTheme.colorScheme`, so previews render with Material3 defaults (white background, default purple primary) instead of Weatherly's cream/navy palette. The night preview's `uiMode = UI_MODE_NIGHT_YES` toggles `isSystemInDarkTheme()` but again hits Material3 defaults, not `DarkColors`.

**File:** `app/src/main/java/com/example/weatherly/ui/Previews.kt`

**Change:** Wrap both preview composables in `WeatherlyTheme { ... }`.

---

### 18. Widget background colour out of sync with design system

**Problem**
`WeatherWidget.kt` uses a hardcoded `Color(0xFF12A5C9)` background — the old brand teal — which no longer matches the app's primary `#6B86A3` (dusty blue). The widget looks like it belongs to a different app.

**File:** `app/src/main/java/com/example/weatherly/widget/WeatherWidget.kt`

**Change:** Replace the hardcoded hex with the primary colour from the design system (`Color(0xFF6B86A3)`). For a more premium widget, apply a simplified condition-aware two-colour gradient (extend `conditionGradient` to be usable from Glance, or maintain a parallel lightweight map).

---

### 19. Pull-to-refresh indicator colour

**Problem**
`PullToRefreshBox` uses Material3's default spinner colour (the scheme's `primary`), which in Weatherly's theme maps to `#6B86A3`. This is actually correct by default, but it's worth verifying explicitly — if the indicator ever appears in the wrong colour after a theme change, it's because `PullToRefreshBox` uses `indicatorColor` which defaults to `MaterialTheme.colorScheme.primary`. Adding an explicit `indicatorColor = Cyan` parameter makes the intent clear and immune to future theme tweaks.

**File:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

---

### 20. `forecastDays=10` fetches data the UI never shows

**Problem**
`OpenMeteoApi.getForecast` requests `forecast_days=10` but `WeatherContent` only ever displays the days the API returns starting from today. The repository builds `daily` from `todayIndex` onward, which could theoretically show up to 10 days, but the `DailyCard` label says "7-day forecast" in the README. In practice the Open-Meteo free tier returns up to 16 days; unnecessary data increases response size and parse time.

**File:** `app/src/main/java/com/example/weatherly/data/remote/OpenMeteoApi.kt`

**Change:** Either set `forecastDays = 7` (to match what's shown) or bump the UI to show 10 days and update the label.

---

### 21. Remove or repurpose dead `TemperatureChartCard` composable

**Problem**
`TemperatureChartCard` was removed from `WeatherContent` (item 16) but the composable still lives in `WeatherComponents.kt`. Dead Compose code adds maintenance surface and confuses future contributors.

**Two options:**
1. **Delete** it — the H/L summary in `HourlyCard` covers its former role.
2. **Repurpose** it — wire it into `DetailSheet.Day` to show a mini temperature trend for the tapped day's hourly data.

Option 2 requires passing the selected day's hourly data into the sheet, which is not currently available at the `DetailSheet.Day` level.

---

## Priority 2 — Medium effort

### 22. Chat suggestion chips: semantic colour tinting

**Problem**
All six suggestion chips (`AssistChip`) in `ChatScreen` use `containerColor = MaterialTheme.colorScheme.surface` equally, giving them identical visual weight. The chips answer conceptually distinct questions (rain → umbrella, cold → jacket, etc.) and should communicate their domain at a glance.

**File:** `app/src/main/java/com/example/weatherly/ui/ChatScreen.kt`

**Change:** Map each chip to a tinted background from the existing accent palette:

| Intent | Chip tint |
|---|---|
| UMBRELLA | `Cyan.copy(alpha = 0.12f)` (blue/rain) |
| JACKET | `Indigo.copy(alpha = 0.12f)` (cold) |
| WALKING | `Green.copy(alpha = 0.12f)` |
| DRIVING | `Amber.copy(alpha = 0.12f)` |
| HIKING | `Teal.copy(alpha = 0.12f)` |
| CLOTHING | `Coral.copy(alpha = 0.12f)` |

---

### 23. ChatScreen: compact weather context strip

**Problem**
The chat header subtitle reads "Grounded in West Lafayette" but the user has no sense of *what* conditions the AI is reading once they're in the chat view. The AI's answers reference specific numbers (temperature, rain chance) with no way for the user to verify them without leaving the screen.

**File:** `app/src/main/java/com/example/weatherly/ui/ChatScreen.kt`

**Change:** Add a compact, non-scrollable one-line strip above the `LazyColumn` when `weather != null`:

```
┌─────────────────────────────────────────────────────┐
│  West Lafayette  ·  ☀  31°  ·  40% rain  ·  UV 8   │
└─────────────────────────────────────────────────────┘
```

A single `GlassCard` with `TextSecondary` 13sp text. Use `WeatherGlyph` at 16dp for the condition icon.

---

### 24. MetricsGrid: primary strip + grouped secondary

**Problem**
Nine tiles at identical visual weight (118dp `GlassCard`) presents everything as equally important. Humidity, wind, and UV index are universally relevant; barometric pressure and visibility are specialist data most users rarely need.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

**Change:** Split `MetricsGrid` into two visual tiers:
1. **Primary row** — three stats (Humidity, Wind, UV) in a single compact `GlassCard`, 64dp tall, 3-column, no icons, value at 22sp Bold + unit at 12sp.
2. **Secondary grid** — remaining six tiles in the existing 2-col `GlassCard` pattern, grouped under `TextSecondary` eyebrows: "ATMOSPHERE" (Pressure, Visibility), "SUN & SAFETY" (UV detail, AQI), "DAILY" (Sunrise, Precipitation).

---

### 25. In-app OpenRouter key + model settings UI

**Problem**
`PreferencesStore.setOpenRouterKey()` and `setOpenRouterModel()` are fully implemented but there is no UI to call them. Users who want to use the AI assistant must know to edit `local.properties` before building — not viable for distributed builds or TestFlight-style installs.

**Approach:** Add a settings icon or "Set up AI" prompt in `ChatScreen` when `chatViewModel.hasKey` is false. A `ModalBottomSheet` with two `OutlinedTextField`s (API key + optional model override) and a "Save" button is sufficient. The key input should use `KeyboardType.Password` so it's masked.

**Files to touch:** `ChatScreen.kt`, `ChatViewModel.kt` (expose a `saveKey(key, model)` method).

---

### 26. `WeatherGlyph` accessibility content descriptions

**Problem**
`WeatherGlyph` draws weather icons on `Canvas` with zero semantic information. Screen readers (TalkBack) receive nothing from these elements — a user with visual impairment hears the temperature but gets no weather condition cue from the icon.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherGlyph.kt`

**Change:** Add a `contentDescription` parameter defaulting to the WMO text label:

```kotlin
@Composable
fun WeatherGlyph(
    code: Int,
    isDay: Boolean = true,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = wmoText(code)
) {
    Canvas(modifier = modifier.size(size).semantics { this.contentDescription = contentDescription ?: "" }) { ... }
}
```

Also add `import androidx.compose.ui.semantics.semantics` and `import androidx.compose.ui.semantics.contentDescription`.

---

### 27. `TipBanner`: left-border annotation style

**Problem**
The full-bleed `TipBanner` background (e.g. `Color(0xFF0D1E2E)` in dark mode) reads as an alert or notification rather than editorial context. The visual weight competes with the cards below.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

**Change:** Replace the filled background with a left accent border + very light tint:

```kotlin
Row(
    modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
        .background(fg.copy(alpha = if (isDark) 0.08f else 0.10f))
        .drawBehind {
            drawRect(color = fg, size = Size(4.dp.toPx(), size.height))
        }
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
)
```

This makes tips feel like margin annotations rather than push banners.

---

## Priority 3 — Larger scope

### 28. Localization: replace hardcoded strings with `string` resources

**Problem**
All user-visible text is hardcoded in Kotlin (`"Feels like"`, `"No major weather to plan around today."`, section labels, error messages, etc.). There is a `strings.xml` in the project (`res/values/strings.xml`) with only `app_name`. This prevents translation and makes copy changes require code edits.

**Scope:** ~60 distinct user-facing strings across `WeatherComponents.kt`, `WeatherScreen.kt`, `ChatScreen.kt`, `WeatherAdvisor.kt`, and `ChatRepository.kt` (system prompt excluded — it should stay in code). Extract to `strings.xml` using Android Studio's "Extract string resource" refactor. Parameterized strings (e.g. `"$diff° warmer than yesterday"`) use `getString(R.string.warmer_than_yesterday, diff)`.

---

### 29. Weather-change push notification

**Problem**
The app has no background awareness. A user who doesn't open the app won't know a thunderstorm is rolling in this afternoon.

**Approach:**
- Use `WorkManager` to schedule a daily check (e.g. 7 AM).
- Compare tomorrow's forecast against simple thresholds (rain > 60%, UV > 8, thunderstorm code, temperature delta > 5°).
- Fire a `NotificationCompat` with the condition and headline.
- Requires `POST_NOTIFICATIONS` permission (Android 13+) and a notification channel.
- The widget receiver could double as the trigger to avoid a separate background process.

**New files:** `worker/WeatherCheckWorker.kt`, `notification/WeatherNotificationService.kt`

---

### 30. Share current weather

**Problem**
There is no way to share conditions with another person (e.g. "It's 31° and partly cloudy in West Lafayette — bring sunscreen").

**Approach:** Add a share `IconButton` in the `WeatherContent` header row (alongside the existing locations and chat buttons). On tap, build a plain-text summary from `WeatherData` and fire `Intent.ACTION_SEND`. No new dependency needed.

**File:** `app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt`

---

## Field naming cleanup ⬜ Deferred

`WeatherData` fields like `currentTempC`, `windKmh`, `weekMinC`, and `DayEntry.windMaxKmh` imply fixed units in their names, but they hold values in whatever unit system the user selected (metric or imperial). This is a correctness trap for future contributors.

**Recommended rename:**

| Current name | Rename to |
|---|---|
| `currentTempC` | `currentTemp` |
| `highTodayC` | `highToday` |
| `lowTodayC` | `lowToday` |
| `realFeelC` | `feelsLike` |
| `windKmh` | `windSpeed` |
| `windGustKmh` | `windGust` |
| `weekMinC` | `weekMin` |
| `weekMaxC` | `weekMax` |
| `DayEntry.windMaxKmh` | `DayEntry.windMax` |
| `DayEntry.precipSumMm` | `DayEntry.precipSum` |

Touches ~15 files. Use IDE rename refactoring (not find-replace) to catch all usages. Review `WeatherAdvisor`'s internal `toC()` / `toKmh()` conversions after the rename.

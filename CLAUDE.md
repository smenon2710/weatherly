# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SkySpeak: Premium Weather Chat is an Android weather app (Kotlin + Jetpack Compose) that pulls forecast data from Open-Meteo (no API key) and optionally uses OpenRouter for an AI chat assistant. The app was formerly named Weatherly; the package name (`com.example.weatherly`) and all Kotlin class names retain the `Weatherly` prefix until the pre-launch application ID migration.

## Build & run

Open the project root in Android Studio (Quail or newer) and let Gradle sync, then run on a device or emulator. There is no CLI build script — use Android Studio's run configuration or:

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # install on connected device
./gradlew test                   # unit tests (none yet; placeholder)
./gradlew lint                   # lint
```

## API key setup

Copy `local.properties.template` to `local.properties` (already git-ignored) and fill in:

```
sdk.dir=/path/to/your/Android/sdk
OPENROUTER_API_KEY=sk-or-...   # optional — quick-suggest chips work without it
OPENROUTER_MODEL=openrouter/free   # optional override; any OpenRouter model ID works
```

Both values are injected at build time into `BuildConfig.OPENROUTER_API_KEY` and `BuildConfig.OPENROUTER_MODEL`. If no key is present at build time, users can enter one directly in the chat screen; it is stored in `SharedPreferences` via `PreferencesStore`.

## Architecture

**Single-activity, no Navigation component.** `MainActivity` holds a `WeatherViewModel` (activity-scoped so both screens share it) and toggles between `WeatherScreen` and `ChatScreen` via `AnimatedContent` + a `showChat` boolean.

**Data layer:**

| Class | Role |
|---|---|
| `NetworkModule` | Singleton object wiring four Retrofit clients: Open-Meteo forecast, Open-Meteo geocoding, Open-Meteo air quality, OpenRouter. Exposes `makeStreamingCall(Request)` for raw SSE calls. No DI framework. |
| `WeatherRepository` | Single source of truth. Fetches forecast + air quality in parallel (`coroutineScope`/`async`). 30-minute in-memory cache keyed by `"lat,lon,units"`. |
| `ChatRepository` | Streams OpenRouter responses token-by-token via SSE (`askStreaming`) and simulates the same feel for rule-based answers (`simulateStreaming`). Both return `Flow<String>`. Retries once on HTTP 429. All requests include `HTTP-Referer` and `X-Title` headers for OpenRouter log attribution. |
| `ForecastCache` | Persists the last successful `WeatherData` as JSON in `SharedPreferences` (`forecast_cache`). `WeatherViewModel.init` loads it synchronously so the app opens instantly offline. Replaced by fresh data on every successful network fetch. |
| `PreferencesStore` | `SharedPreferences` wrapper for unit system, saved places, selected place, and on-device OpenRouter key/model. |
| `WeatherAdvisor` | Pure object (no network). Answers six hard-coded intents (UMBRELLA, JACKET, WALKING, DRIVING, HIKING, CLOTHING) locally from the current `WeatherData`. |

**UI layer:**

- `WeatherViewModel` — `AndroidViewModel` exposing `StateFlow<WeatherUiState>`. On `init`, loads `ForecastCache` synchronously so the screen is never blank on cold start. Handles location resolution (falls back to `LocationProvider` when no place is selected), unit switching, city search, and pull-to-refresh. `WeatherUiState.Success` carries a `cachedAt: Long?` timestamp; non-null means the data came from cache and triggers a "Showing data from Xm ago" label. Background refreshes keep existing data visible (only the spinner changes).
- `ChatViewModel` — Exposes `messages`, `sending`, and `streamingText: StateFlow<String>`. Accumulates SSE/simulated chunks into `_streamingText`; on completion moves the full text into `_messages`. `clear()` cancels any in-flight stream job.
- `WeatherScreen` / `ChatScreen` — Compose screens consuming the ViewModel via `collectAsStateWithLifecycle`.
- `ui/components/` — Reusable Compose functions (header, hourly row, daily list, detail tiles).
- `ui/theme/` — Material 3 color scheme, typography, `WeatherlyTheme`.

**Widget:** `WeatherWidget` (Jetpack Glance) fetches weather independently at system-scheduled update intervals. `WeatherWidgetReceiver` wires it into the manifest. The widget creates its own `WeatherRepository` instance and does not share the app's `ForecastCache`.

- **Size-aware layouts** — `sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, WIDE, LARGE))`. Glance picks the largest declared breakpoint that fits the actual widget size; `WidgetContent` dispatches on `LocalSize.current`:

  | Constant | DpSize | Cell grid | Content |
  |---|---|---|---|
  | `SMALL` | 110×50 dp | 2×1 | Emoji + current temp (22 sp Bold), centered |
  | `MEDIUM` | 110×110 dp | 2×2 | Location header + chrono-dynamic vertical stack |
  | `WIDE` | 250×50 dp | 4×1 | `emoji temp° · location` + compact next-4-hour text |
  | `LARGE` | 250×110 dp | 4×2 | `LargeHeader` (2-column) + `HourlyStrip` (up to 5 cells) |

  `weather_widget_info.xml` declares `minWidth="110dp"`, `minHeight="40dp"`, `targetCellWidth="2"`, `targetCellHeight="2"`, `resizeMode="horizontal|vertical"`.

- **Chrono-dynamic content** — `currentTimeOfDay()` reads `Calendar.HOUR_OF_DAY`. MEDIUM and LARGE layouts branch on the result:

  | TimeOfDay | Hours | Focus |
  |---|---|---|
  | `MORNING` | 5–10 | Today's high temp (hero) + rain probability |
  | `DAYTIME` | 11–17 | Current temp (hero) + condition + H/L |
  | `NIGHT` | 18–4 | Tomorrow's H/L (hero) + tomorrow's condition |

- **Material You colors** — `resolveWidgetColors(context)` runs outside the composable (no `GlanceTheme` dependency needed). On API 31+ reads `android.R.color.system_accent1_100/700/900` via `ContextCompat.getColor` and detects dark mode from `context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK`. Light: accent1-100 bg + accent1-900 text. Dark: accent1-700 bg + accent1-100 text. Pre-API 31 falls back to the app's static dusty-blue palette. Returns `WColors(bg, textPrimary, textSecondary: ColorProvider)`. Only the `ColorProvider(Color)` single-argument overload is used — `ColorProvider(day, night)` does not exist in `glance-appwidget:1.1.0`.

**Launcher icon:** Adaptive icon (`mipmap-anydpi-v26/`) — warm-gold sun glyph (`#E0B15C`) on deep navy (`#0F1923`), with three concentric speech-wave arcs to the right of the sun (radii 20/25/30 dp, fading opacity) representing the "speak" dimension. XML-only; no PNG fallbacks needed since `minSdk = 26`.

**WMO weather codes** are mapped to emoji and text in `util/WeatherIcon.kt`. All temperature/wind/precip values in `WeatherData` are stored in the user-selected unit (they come back from Open-Meteo already converted); `WeatherAdvisor` converts back to metric internally for threshold comparisons.

## UI theme & branding

**Logo:** Two-tone text wordmark — an `AnnotatedString` in `WeatherScreen.kt` rendering `"sky"` in `TextSecondary` and `"speak"` in `TextPrimary`, at 20 sp, `FontWeight.Light`, `letterSpacing = 5.sp`. The color split is subtle in light mode (muted slate → near-black) and more expressive in dark mode (slate → near-white). Colors are captured as local vals before the `buildAnnotatedString` block so they are read in composable scope. Do not reintroduce image assets or collapse the two spans into a single `color` parameter.

**Colour scheme (`ui/theme/Theme.kt`):**
- Light: background `#F4F1EB` (warm cream), surface `#FDFCFA` (barely warm white), primary `#6B86A3` (dusty blue).
- Dark: background `#0F1923` (deep navy), surface `#1A2530`, primary `#7FA3C2`.
- `AppBackground`, `TextPrimary`, `TextSecondary` are `@Composable` vals in `WeatherComponents.kt` resolved from `MaterialTheme.colorScheme` — always use these rather than hardcoded colours.

**Condition gradient (`conditionGradient` in `WeatherComponents.kt`):** A `@Composable` function that maps a WMO weather code + `isDay` flag to a two-stop `List<Color>`. The first stop is a sky tone (blue for clear day, indigo for thunder, slate for rain, etc.); the second stop is always `MaterialTheme.colorScheme.background` so the gradient fades seamlessly into the card area. Used as the background of the hero section in `WeatherScreen.kt` via `Brush.verticalGradient`.

**`GlassCard` (`ui/components/WeatherComponents.kt`):** The shared card wrapper. Shadow adapts per theme: `1 dp` in light (airy), `6 dp` in dark (depth). Border opacity likewise adapts. All major content sections (hourly, daily, metric tiles) use `GlassCard`.

**`TipBanner`:** Left-border annotation style — 4dp accent bar on the left edge, very subtle tint (`fg` at 8–10% alpha), rounded only on the right corners. Reads as editorial context rather than an alert.

**`WeatherGlyph`:** Accepts an optional `contentDescription: String?` parameter (defaults to `wmoText(code)`). Pass `null` for decorative instances where adjacent text already carries the meaning.

**Section labels:** 11 sp uppercase with `letterSpacing = 0.8.sp` — keep this style consistent across any new sections.

**`CurrentHeader` element order:** location (12 sp, Medium, 2 sp letter-spacing, uppercase) → glyph + condition (Row, 20 dp glyph, 15 sp Normal) → temperature (96 sp, Thin — the undisputed hero) → H/L → feels-like → lookahead pill. The large standalone glyph (76 dp) no longer appears in the hero.

**Lookahead pill (`WeatherData.headline`):** Populated by `WeatherRepository.buildUpcomingHeadline()`. Operates on the raw `HourlyBlock` + `nowIndex` (the exact position of the current hour in the full 7-day API array) so the scan always starts from the true "now" and naturally crosses into the next day. Scans the next 12 raw hourly entries (`nowIndex+1` .. `nowIndex+12`) for the first significant condition change — Thunderstorm, Snow, Rain, Freezing rain, Drizzle, Fog — relative to the current WMO code. Appends a wind note when the next-12-hour max exceeds 40 km/h / 25 mph. Trusts WMO codes directly; only light drizzle (51–57) requires a ≥30% precipitation-probability floor. Falls back to `comparedToYesterday` text when null.

**`TipBanner` source data:** Tips are generated by `WeatherRepository.buildTips()`. At night (`isDay = 0`) the tip day switches to the next daily entry so tips reflect upcoming conditions rather than the day just passed. The precipitation probability passed to `buildTips` is always `max(tipDay.precipProbMax, maxPrecipChanceInNext12Hours)` to capture short-range rain even when the daily summary hasn't rolled over yet.

**Daily forecast precipitation probability:** Shown inline below the WMO glyph in `DailyCard` rows, using the same `Indigo` style as the hourly row, whenever `precipProbMax > 0` and the day's WMO code is in the precipitation/fog range (45–99).

## Key invariants

- `WeatherData` fields store values **in the user's current unit system** (not always metric). This is set at fetch time via `UnitSystem.apiTemp`/`apiWind`/`apiPrecip` passed to Open-Meteo, and the unit labels (`windUnit`, `precipUnit`, `visibilityUnit`, `tempLabel`) travel alongside.
- The 30-minute cache in `WeatherRepository` is in-memory only and scoped to the process. Widget updates bypass the app's ViewModel and create their own `WeatherRepository` instance.
- OpenRouter key resolution: `PreferencesStore.getOpenRouterKey(buildDefault)` returns the on-device key first, falling back to the build-time `BuildConfig` value. Always pass `BuildConfig.OPENROUTER_API_KEY` as the fallback.
- Drawable resource names must be all-lowercase (`a-z`, `0-9`, `_`) — Android's AAPT2 rejects uppercase characters.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Weatherly is an Android weather app (Kotlin + Jetpack Compose) that pulls forecast data from Open-Meteo (no API key) and optionally uses OpenRouter for an AI chat assistant.

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
OPENROUTER_MODEL=google/gemma-4-26b-a4b-it:free   # optional override
```

Both values are injected at build time into `BuildConfig.OPENROUTER_API_KEY` and `BuildConfig.OPENROUTER_MODEL`. If no key is present at build time, users can enter one directly in the chat screen; it is stored in `SharedPreferences` via `PreferencesStore`.

## Architecture

**Single-activity, no Navigation component.** `MainActivity` holds a `WeatherViewModel` (activity-scoped so both screens share it) and toggles between `WeatherScreen` and `ChatScreen` via `AnimatedContent` + a `showChat` boolean.

**Data layer:**

| Class | Role |
|---|---|
| `NetworkModule` | Singleton object wiring four Retrofit clients: Open-Meteo forecast, Open-Meteo geocoding, Open-Meteo air quality, OpenRouter. Exposes `makeStreamingCall(Request)` for raw SSE calls. No DI framework. |
| `WeatherRepository` | Single source of truth. Fetches forecast + air quality in parallel (`coroutineScope`/`async`). 30-minute in-memory cache keyed by `"lat,lon,units"`. |
| `ChatRepository` | Streams OpenRouter responses token-by-token via SSE (`askStreaming`) and simulates the same feel for rule-based answers (`simulateStreaming`). Both return `Flow<String>`. Retries once on HTTP 429. |
| `PreferencesStore` | `SharedPreferences` wrapper for unit system, saved places, selected place, and on-device OpenRouter key/model. |
| `WeatherAdvisor` | Pure object (no network). Answers six hard-coded intents (UMBRELLA, JACKET, WALKING, DRIVING, HIKING, CLOTHING) locally from the current `WeatherData`. |

**UI layer:**

- `WeatherViewModel` — `AndroidViewModel` exposing `StateFlow<WeatherUiState>`. Handles location resolution (falls back to `LocationProvider` when no place is selected), unit switching, city search, and pull-to-refresh. Background refreshes keep existing data visible (only the spinner changes).
- `ChatViewModel` — Exposes `messages`, `sending`, and `streamingText: StateFlow<String>`. Accumulates SSE/simulated chunks into `_streamingText`; on completion moves the full text into `_messages`. `clear()` cancels any in-flight stream job.
- `WeatherScreen` / `ChatScreen` — Compose screens consuming the ViewModel via `collectAsStateWithLifecycle`.
- `ui/components/` — Reusable Compose functions (header, hourly row, daily list, detail tiles).
- `ui/theme/` — Material 3 color scheme, typography, `WeatherlyTheme`.

**Widget:** `WeatherWidget` (Jetpack Glance) fetches weather independently at system-scheduled update intervals and renders a compact tile. `WeatherWidgetReceiver` wires it into the manifest.

**WMO weather codes** are mapped to emoji and text in `util/WeatherIcon.kt`. All temperature/wind/precip values in `WeatherData` are stored in the user-selected unit (they come back from Open-Meteo already converted); `WeatherAdvisor` converts back to metric internally for threshold comparisons.

## UI theme & branding

**Logo:** Text wordmark — `"weatherly"` rendered as a `Text` composable in `WeatherScreen.kt` at 20 sp, `FontWeight.Light`, `letterSpacing = 5.sp`. Resolves colour from `TextPrimary` so it adapts to light/dark automatically. Do not reintroduce image assets for the logo.

**Colour scheme (`ui/theme/Theme.kt`):**
- Light: background `#F4F1EB` (warm cream), surface `#FDFCFA` (barely warm white), primary `#6B86A3` (dusty blue).
- Dark: background `#0F1923` (deep navy), surface `#1A2530`, primary `#7FA3C2`.
- `AppBackground`, `TextPrimary`, `TextSecondary` are `@Composable` vals in `WeatherComponents.kt` resolved from `MaterialTheme.colorScheme` — always use these rather than hardcoded colours.

**Condition gradient (`conditionGradient` in `WeatherComponents.kt`):** A `@Composable` function that maps a WMO weather code + `isDay` flag to a two-stop `List<Color>`. The first stop is a sky tone (blue for clear day, indigo for thunder, slate for rain, etc.); the second stop is always `MaterialTheme.colorScheme.background` so the gradient fades seamlessly into the card area. Used as the background of the hero section in `WeatherScreen.kt` via `Brush.verticalGradient`.

**`GlassCard` (`ui/components/WeatherComponents.kt`):** The shared card wrapper. Shadow adapts per theme: `1 dp` in light (airy), `6 dp` in dark (depth). Border opacity likewise adapts. All major content sections (hourly, daily, metric tiles) use `GlassCard`.

**Section labels:** 11 sp uppercase with `letterSpacing = 0.8.sp` — keep this style consistent across any new sections.

**`CurrentHeader` element order:** location (12 sp, Medium, 2 sp letter-spacing, uppercase) → glyph + condition (Row, 20 dp glyph, 15 sp Normal) → temperature (96 sp, Thin — the undisputed hero) → H/L → feels-like → comparison pill. The large standalone glyph (76 dp) no longer appears in the hero.

## Key invariants

- `WeatherData` fields store values **in the user's current unit system** (not always metric). This is set at fetch time via `UnitSystem.apiTemp`/`apiWind`/`apiPrecip` passed to Open-Meteo, and the unit labels (`windUnit`, `precipUnit`, `visibilityUnit`, `tempLabel`) travel alongside.
- The 30-minute cache in `WeatherRepository` is in-memory only and scoped to the process. Widget updates bypass the app's ViewModel and create their own `WeatherRepository` instance.
- OpenRouter key resolution: `PreferencesStore.getOpenRouterKey(buildDefault)` returns the on-device key first, falling back to the build-time `BuildConfig` value. Always pass `BuildConfig.OPENROUTER_API_KEY` as the fallback.
- Drawable resource names must be all-lowercase (`a-z`, `0-9`, `_`) — Android's AAPT2 rejects uppercase characters.

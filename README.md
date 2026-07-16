# SkySpeak: Premium Weather Chat

*(formerly Weatherly — the repo and Kotlin package name still say `weatherly`)*

A clean, ad-free weather app for Pixel (and any Android phone), built with
Kotlin + Jetpack Compose. Weather data comes from **Open-Meteo** — free for
non-commercial use, no API key, no sign-up.

There is no ad SDK anywhere in this project; "ad-free" is simply the default
state of your own app. The app is free with no paywalled features; an optional
in-app donation link supports the developer if you'd like to.

## Features
- Current conditions, next 24 hours, and a 7-day forecast in a single API call
- Condition-responsive hero gradient — the background shifts tone to match the sky (clear blue, rain slate, thunder indigo, snow white-blue), with a subtle time-of-day tint at dawn/golden hour/dusk
- Precipitation radar map (OpenStreetMap + RainViewer, no API key) with play/pause and a frame scrubber
- Official National Weather Service advisories (severe warnings, watches, air quality alerts — US locations only), no API key, shown with severity-colored cards and full detail sheets
- Home-screen widget with size-aware layouts, chrono-dynamic content (morning/daytime/night), and Material You dynamic colors
- Automatic location via FusedLocationProvider + on-device reverse geocoding
- Pull-to-refresh, plus quiet auto-refresh on resume and every 30 minutes
- Offline-first: the last successful forecast is cached so the app never opens to a blank screen
- Built-in AI weather assistant (OpenRouter) that answers practical questions
  like "can I jog this evening?" using your actual forecast as context
- Material 3 UI with full light/dark theme support and Compose previews
- No weather API key, no credit card, no usage worries for personal use

## Setup
1. Open the `weatherly` folder in Android Studio (Quail or newer) and let Gradle sync.
2. Run on a Pixel or emulator. Grant the location permission when asked.

That's it — there is no key to configure. Open-Meteo requires no authentication
for non-commercial use.

## AI weather assistant
The chat icon (top-right of the weather screen) opens an assistant. The quick
suggestion chips (umbrella, jacket, walk/jog, driving, hiking, what to wear) are
answered instantly on-device from the current forecast — no key, no network. For
free-form typed questions it uses **OpenRouter**, configured entirely by the
developer (the user never sees or enters a key):

1. Create a free key at https://openrouter.ai/keys.
2. Add it to `local.properties` (never committed): `OPENROUTER_API_KEY=...`
3. Optionally set `OPENROUTER_MODEL` there too (default: a free Gemma route).
   Free model IDs rotate — see https://openrouter.ai/models (filter: Free).

Both values are read at build time via `BuildConfig`. If no key is set, the
suggestion chips still work; only typed questions are disabled.

## Data source & attribution
Weather data is provided by Open-Meteo (https://open-meteo.com) under the
CC BY 4.0 licence, which requires attribution. The app shows an attribution
footer to satisfy this. Free non-commercial use allows up to ~10,000 calls/day,
far beyond personal needs; this app also caches results for 30 minutes in memory.

Weather advisories are provided by the National Weather Service
(https://api.weather.gov), a free public U.S. government API — no key, no
attribution requirement (public domain), US locations only.

## Project structure
```
app/src/main/java/com/example/weatherly/
├─ MainActivity.kt           # shares WeatherViewModel across Weather/Chat/Radar screens
├─ data/
│  ├─ model/        # Open-Meteo models, WeatherData domain model, chat models, NWS alert models
│  ├─ remote/       # Retrofit interfaces (OpenMeteo, OpenRouter, NWS) + network module
│  ├─ repository/   # WeatherRepository + ChatRepository (weather-aware prompts)
│  └─ prefs/        # unit/place selection, on-device OpenRouter key/model, forecast cache
├─ location/        # FusedLocationProvider wrapper
├─ ui/
│  ├─ WeatherViewModel.kt / WeatherScreen.kt   # pull-to-refresh + chat/radar entry
│  ├─ ChatViewModel.kt / ChatScreen.kt         # AI assistant
│  ├─ RadarScreen.kt                           # OSMDroid + RainViewer precipitation radar
│  ├─ Previews.kt   # @Preview composables with sample data
│  ├─ components/   # Header, hourly row, daily list, metric tiles, attribution
│  └─ theme/        # Colors, type, Material 3 theme
├─ widget/          # Jetpack Glance home-screen widget
└─ util/            # WMO weather-code -> emoji + text, moon-phase calculator
```

See `CLAUDE.md` for full architecture details.

## Notes
- Conditions use WMO weather codes (Open-Meteo's format); see `util/WeatherIcon.kt`.
- Library versions are recent stable picks; bump them if Android Studio suggests.
- minSdk 26; compileSdk/targetSdk 35.
- Play Store submission status and checklist: see `PLAYSTORE_LAUNCH.md`.

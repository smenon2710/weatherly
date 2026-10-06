# SkySpeak: Premium Weather Chat

*(formerly Weatherly — the repo and Kotlin package name still say `weatherly`)*

[<img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">](https://play.google.com/store/apps/details?id=io.github.smenon2710.skyspeak)

A clean, ad-free weather app for Pixel (and any Android phone), built with
Kotlin + Jetpack Compose. Weather data comes from **Open-Meteo** — free for
non-commercial use, no API key, no sign-up.

There is no ad SDK anywhere in this project; "ad-free" is simply the default
state of your own app. The app is free with no paywalled features; an optional
in-app donation link supports the developer if you'd like to.

## Features
- Current conditions, next 24 hours, and a 7-day forecast in a single API call
- Full-screen animated weather background (rain, snow, fog, clouds, haze, sleet, hail, thunder, freezing rain, and more — 26 conditions in all) driven entirely by real data: WMO code, cloud cover %, visibility, air quality, wind speed, and active NWS alerts. Visible in the hero and in the gaps around cards, which stay fully opaque for legibility.
- Rain and snow are tracked and shown as genuinely distinct hazards — separate real amounts (not a single ambiguous "precipitation" figure), distinct units where they actually differ (snowfall is cm/in, not mm/in), and the AI assistant and quick-suggestion advice (umbrella, driving, hiking, etc.) all correctly distinguish "it's snowing" from "it's raining" rather than treating a generic precipitation-chance percentage as if it always meant rain
- Official National Weather Service advisories (severe warnings, watches, air quality alerts — US locations only), no API key, shown as a compact severity-colored strip with full detail sheets
- Optional background notifications (both off by default): severe-alert start/end notifications and an ongoing current-conditions notification, checked roughly every 30 minutes for the device's current location — in versionCode 16, which is in Google's review for Production (submitted 2026-10-06)
- A tap-to-open forecast briefing in plain language: temperature, sky, precipitation, active alerts, and how tomorrow compares with today — generated on-device, no AI call
- Today's high/low tide times for US coastal locations (NOAA CO-OPS, no API key)
- Home-screen widget with size-aware layouts, chrono-dynamic content (morning/daytime/night), and Material You dynamic colors
- Automatic location via FusedLocationProvider + on-device reverse geocoding
- Pull-to-refresh, plus quiet auto-refresh on resume and every 30 minutes
- Offline-first: the last successful forecast is cached so the app never opens to a blank screen
- Built-in AI weather assistant (OpenRouter) that answers practical questions
  like "can I jog this evening?" using your actual forecast as context
- Settings screen: light/dark/system theme, units, and on-device OpenRouter key/model management
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
free-form typed questions it uses **OpenRouter**. A build can ship with a
developer key (below), and a user can also enter their own key and model in
Settings, which takes precedence. To build with a key:

1. Create a free key at https://openrouter.ai/keys.
2. Add it to `local.properties` (never committed): `OPENROUTER_API_KEY=...`
3. Optionally set `OPENROUTER_MODEL` there too (default: a free Gemma route).
   Free model IDs rotate — see https://openrouter.ai/models (filter: Free).

Both values are read at build time via `BuildConfig`. If no key is set at build
time or in Settings, the suggestion chips still work; only typed questions are
disabled. A build-time key is embedded in the APK and can be extracted, so give
it a spending limit on the OpenRouter side — the in-app daily cap and model
lock only restrain the app itself.

## Data source & attribution
Weather data is provided by Open-Meteo (https://open-meteo.com) under the
CC BY 4.0 licence, which requires attribution. The app shows an attribution
footer to satisfy this. Free non-commercial use allows up to ~10,000 calls/day,
far beyond personal needs; this app also caches results for 30 minutes in memory.

Weather advisories are provided by the National Weather Service
(https://api.weather.gov), a free public U.S. government API — no key, no
attribution requirement (public domain), US locations only. Tide predictions
come from NOAA CO-OPS (https://api.tidesandcurrents.noaa.gov) on the same terms.

## Project structure
```
app/src/main/java/com/example/weatherly/
├─ MainActivity.kt           # shares WeatherViewModel across Weather/Chat/Settings screens
├─ data/
│  ├─ model/        # Open-Meteo models, WeatherData domain model, chat models, NWS alert + tide models
│  ├─ remote/       # Retrofit interfaces (OpenMeteo, OpenRouter, NWS, NOAA tides) + network module
│  ├─ repository/   # WeatherRepository, ChatRepository (weather-aware prompts), AlertTracker
│  ├─ advice/       # WeatherAdvisor (local rule-based advice) and ForecastBriefing (the forecast sheet's text)
│  └─ prefs/        # unit/place selection, on-device OpenRouter key/model, forecast cache
├─ location/        # FusedLocationProvider wrapper
├─ notifications/   # WorkManager background check + alert/status notifications
├─ ui/
│  ├─ WeatherViewModel.kt / WeatherScreen.kt   # pull-to-refresh + chat/settings entry
│  ├─ ChatViewModel.kt / ChatScreen.kt         # AI assistant
│  ├─ SettingsViewModel.kt / SettingsScreen.kt # theme, units, notifications, OpenRouter key/model
│  ├─ Previews.kt   # @Preview composables with sample data
│  ├─ components/   # Header, hourly row, daily list, metric tiles, attribution, WeatherBackground
│  └─ theme/        # Colors, type, Material 3 theme
├─ widget/          # Jetpack Glance home-screen widget
└─ util/            # WMO weather-code text, moon phase, tide stations, haptics, local time
```

See `CLAUDE.md` for full architecture details.

## Notes
- Conditions use WMO weather codes (Open-Meteo's format); see `util/WeatherIcon.kt`.
- Library versions are recent stable picks; bump them if Android Studio suggests.
- minSdk 26; compileSdk/targetSdk 36.
- Play Store submission status and checklist: see `PLAYSTORE_LAUNCH.md`.

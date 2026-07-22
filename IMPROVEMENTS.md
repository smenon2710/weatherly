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
| 22 | Chat suggestion chips: semantic colour tinting (2026-07-01) |
| 23 | ChatScreen: compact weather context strip |
| 24 | MetricsGrid: primary compact strip + grouped secondary sections (2026-07-01) |
| 26 | `WeatherGlyph` accessibility content descriptions |
| 27 | `TipBanner`: left-border annotation style |
| 31 | 12-hour lookahead headline under temperature (`buildUpcomingHeadline`) |
| 31a | Fix: time-aware tips — use tomorrow's forecast at night, not today's stale high |
| 31b | Fix: headline WMO-code trust — remove false precip-probability gate on rain codes |
| 31c | Fix: headline scans raw `HourlyBlock` + `nowIndex` for accurate cross-day lookahead |
| 32 | Daily forecast: precipitation probability shown on rain/snow/fog days |
| W1 | Widget: size-aware layouts via `SizeMode.Responsive` (2×1, 2×2, 4×1, 4×2) |
| W2 | Widget: chrono-dynamic content — morning/daytime/night focus per size |
| W3 | Widget: Material You dynamic colors from system accent palette (API 31+) |
| R1 | Rebrand to SkySpeak: Premium Weather Chat — app name, two-tone wordmark, speech-wave launcher icon, all UI strings |

### Completed — Priority 3

| # | Title |
|---|---|
| 11 | Adaptive launcher icon (warm-gold sun on deep navy, XML only — minSdk 26) |

### Completed — Settings Screen (2026-07-13)

| # | Title |
|---|---|
| 25 | In-app OpenRouter key + model settings UI — new `SettingsScreen.kt`/`SettingsViewModel.kt`. The key field never displays the stored secret (see B9 below); model field is a plain editable override. Both backed by `PreferencesStore` |
| 35 | "Rate this app" entry point — deep-links to the Play Store listing (`market://details`, falling back to the web listing) rather than the In-App Review API, per Google's own guidance that manual settings buttons should link directly to the Store listing |
| 36 | Manual theme toggle: Light / Dark / System — new `ThemePreference` enum persisted via `PreferencesStore`, `WeatherViewModel.themePreference`/`setThemePreference()`, resolved into `WeatherlyTheme`'s `darkTheme` param in `MainActivity`. Also introduced `LocalIsDarkTheme` (`ui/theme/Theme.kt`) and switched `GlassCard`, `conditionGradient`, `tipColors`, and `TipBanner` (`WeatherComponents.kt`) from `isSystemInDarkTheme()` to that CompositionLocal — without this, a manual Light/Dark override would apply the chosen colour scheme while shadows/gradients/tinted pills kept following the raw system setting |

Entry point: a new gear icon in `WeatherScreen`'s header row (alongside Radar/Chat).

### Completed — On-Device Testing Follow-ups (2026-07-13)

Found by testing the Settings screen build on a physical phone.

| # | Title |
|---|---|
| — | Header crowding, round 1 — the app-bar row (`WeatherContent` in `WeatherScreen.kt`) was rearranged after invoking the `frontend-design` skill. Root cause was hierarchy, not just spacing: adding a third right-side icon (Settings) gave a rarely-used action the same visual weight as the primary Chat CTA, and crowded the centered wordmark on narrow screens. Fixed by grouping icons by function instead of by which side had room — Locations + Settings are both "configuration" actions (plain icon, left side, matching each other's low visual weight); Radar + Chat stay as tinted/filled "feature" chips (right side). Also switched the row from `Box` + `Alignment.align()` to a 3-slot equal-weight `Row` |
| — | Header crowding, round 2 — the round-1 fix still wrapped "skyspeak" onto two lines on-device. Root cause: giving all three slots (leading icons / wordmark / trailing icons) equal `weight(1f)` capped the wordmark to exactly 1/3 of screen width regardless of how much space the icon groups actually needed, which wasn't enough room for the letter-spaced wordmark. Fixed with the standard toolbar idiom instead: only the wordmark `Box` gets `weight(1f)`; the leading and trailing icon `Row`s size to their own content, so the wordmark always gets whatever width is left over rather than a fixed fraction |
| 2 (follow-up) | Units moved from the Locations bottom sheet into Settings, alongside the theme toggle — `LocationsSheet` no longer takes `units`/`onUnits`; `WeatherViewModel.units`/`setUnits` are now wired directly into `SettingsScreen` from `MainActivity` |
| B7 | Fix: same class of bug as B6, in `HourlyCard` (`WeatherComponents.kt`) instead of `DailyCard` — each hour's icon+precipitation-percentage `Column` had no fixed height, so hours with a shown percentage pushed the temperature text below them further down than hours without one, breaking alignment across the strip. Fixed with the same `height(40.dp)` treatment as B6 |
| B8 | Fix: "Today" label in `DailyCard` mid-word wrapped onto two lines ("Toda" / "y") inside its fixed `width(52.dp)` — "Today" (5 letters) is wider than the 3-letter weekday abbreviations (`EEE` format) the same column normally holds. Fixed with `maxLines = 1` + `TextOverflow.Ellipsis`, and widened the column slightly (52dp → 56dp) so "Today" now fits without truncation |
| — | Lookahead headline accuracy — the "Clear skies for the next few hours" style pill under the main temperature is entirely locally computed (`WeatherRepository.buildUpcomingHeadline()`), not sourced from the API; user flagged it as inaccurate/not useful. Root cause: the no-significant-event fallback path picked whichever WMO code was most common across the next 6 hours by bare plurality (e.g. 3 of 6 hours, tied with another condition), and included the current hour itself in that vote — enough to assert a specific condition on weak, sometimes-wrong evidence. Fixed by requiring a real majority (≥60% of the 6-hour window) before asserting a specific condition; otherwise returns "Mixed conditions over the next few hours." instead of guessing |
| B9 | Security fix: the OpenRouter API key field in Settings originally prefilled with the actual stored key (masked, with a reveal toggle) — meaning anyone with the phone unlocked could reveal and copy out a key that isn't theirs. Redesigned so the field is always empty and only ever holds a *new* value about to be saved; `SettingsViewModel.hasOpenRouterKey` exposes only whether a key exists, never its value. Added an explicit "Remove saved key" action (`PreferencesStore.setOpenRouterKey("")` via `removeOpenRouterKey()`) since Save no longer treats a blank field as "clear the key" |

### Completed — Weather Alerts (2026-07-16)

The app previously showed no official advisories at all — a real functional gap versus other weather apps. Integrated the National Weather Service's free, no-key, US-only active-alerts API (`api.weather.gov/alerts/active`).

| # | Title |
|---|---|
| — | Core feature: `NwsApi`/`NwsAlertModels` (raw response), `NetworkModule.nwsApi` (dedicated client with the `User-Agent` header NWS requires), `WeatherRepository.mapAlerts()` (fetched in parallel with forecast/air-quality, gracefully degrading via `runCatching` so an NWS outage or non-US point never breaks the weather load), `WeatherData.alerts` domain field |
| — | UI: `AlertBannerList` (severity-colored `GlassCard`, first shipped above the hero — see the Design Audit follow-ups below, which took it through a full-bleed/square treatment and finally into the ordinary card flow below the hero, its current position), "+N more" affordance instead of stacking every alert, `DetailSheet.Alert`/`AlertList` + `AlertDetailContent` for the full text |
| — | AI chat awareness: `ChatRepository.weatherBrief()` now prepends active advisories to the system prompt context, and the assistant is told to volunteer them even when not asked directly |
| — | Design pass (invoked `frontend-design` skill): replaced an initial stock Tailwind-style severity palette with one rooted in the app's own tokens — Critical is the one new hue (warm rust/oxblood, not a clinical red); Advisory reuses the launcher icon's warm-gold; Info reuses the existing `tipColors()` RAIN tone verbatim |
| — | Accuracy fix, found via live NWS data: NWS separates `ends` (when the hazard itself ends) from `expires` (when the CAP message expires, often much sooner for long-duration products like a multi-day Flood Watch). Was using `expires`; switched to prefer `ends`, relabeled "Expires" → "In effect until" |
| — | Accuracy fix, also found via live NWS data (Franklin Park, NJ): NWS tags nearly every Air Quality Alert `severity: "Unknown"` (no air-quality category in its CAP taxonomy), so a "Code Red...unhealthful for the general population" alert rendered in the calmest visual tier — same as a Small Craft Advisory. `parseSeverity()` now parses EPA's standardized "Code Red/Orange/Purple/Maroon" AQI names from the description for Air Quality Alerts specifically, defaulting to Moderate (never the calmest tier) rather than Unknown |
| — | Added a same-severity sort tiebreaker (effective time) — NWS can have multiple same-severity alerts active at once (e.g. today's and tomorrow's Air Quality Alert), so ordering was previously arbitrary API-array order |
| — | Added `certainty`/`urgency` badges to the detail sheet (e.g. "IMMEDIATE", "LIKELY") — reuses the alert's own severity color rather than a separate badge palette; NWS's own `"Unknown"` value is treated as absent |
| — | Text-quality fix: NWS text products are hard-wrapped at ~80 columns with a literal newline at every wrap point. Rendered verbatim this produced choppy short lines instead of paragraph reflow; `normalizeNwsText()` collapses wrap-newlines to spaces while preserving genuine paragraph breaks |

All of the above verified on-device (screenshots) against live NWS data in both light and dark theme before landing, including the R8-minified release build (confirms no repeat of the B4 Moshi-stripping issue — the existing package-agnostic `-keep @com.squareup.moshi.JsonClass` rule already covers the new models).

### Completed — Design Audit (2026-07-16)

User-reported bug plus a broader `frontend-design` skill pass, grounded in on-device screenshots (light + dark) rather than guessing.

| # | Title |
|---|---|
| B10 | Bug fix, user-reported: the `DailyCard` 7-day temperature range bar rendered a uniformly different color per unit system for the exact same real temperatures — yellowish in °C, reddish-orange in °F. Root cause: `tempColor()`'s thresholds (0/8/15/22/28°) are calibrated in Celsius, but `DayEntry.lowC`/`highC` hold values in the user's *current display unit* despite the "C" suffix (a documented existing caveat). Fahrenheit numbers are always larger, so they skewed uniformly into the ">28" hot bucket. Fixed by converting to true Celsius first via `toCelsius()`, mirroring the existing `WeatherAdvisor.toC` pattern. `DailyCard` derives `metric` from `data.windUnit` rather than adding a new field. Verified on-device: identical bar colors for the same real temperatures in both unit systems |
| — | Fix: `CurrentHeader`'s location name had no `textAlign`, so a name that wraps to two lines (e.g. "Franklin Park, New Jersey") left-justifies its second line inside an auto-sized text box that's only centered as a whole — reads as left-aligned. Fixed with explicit `textAlign = Center` + `fillMaxWidth()` |
| — | Design fix: dark mode's condition-responsive hero gradient — a signature feature — was nearly invisible. Most dark sky-tone stops in `conditionGradient` were only a few RGB steps from the background color (`#0F1923`), so the "hero shifts with the sky" effect that reads clearly in light mode rendered as a flat navy rectangle for most conditions (confirmed via screenshot: overcast, rain, snow, clear-day, fog all affected; only thunder and clear-night had adequate contrast). Recalibrated each dark stop for real lightness/hue separation from the background, same hue families as light mode |
| — | Design fix: `Indigo` was a byte-for-byte duplicate of `Cyan`, the primary accent used everywhere for icons/buttons. Precipitation callouts (hourly %, daily %, Visibility tile) shared the exact same color as generic app chrome, diluting the color-coding. Gave it a distinct dusty periwinkle (`#7B88BC`) so "this is rain data" reads as its own signal |
| — | Follow-up, user-reported: the alert banner's earlier full-bleed/square treatment (deliberately breaking the app's soft-rounded-corners language, see the Weather Alerts section above) didn't fit the app's overall aesthetic — it read as a foreign, bolted-on system notification rather than part of the app. Rebuilt `AlertBannerList` on the same `GlassCard` every other surface uses (same corner radius, shadow, border, 16dp side margin), with only the fill color overridden by severity; the "+N more" row moved inside the card behind a `HorizontalDivider` instead of its own full-bleed footer strip. Urgency now comes from color and top-of-screen position alone, consistent with how the rest of the app differentiates content without changing its shape language |
| — | Second follow-up, user-reported: even after the GlassCard rework above, the alert card's icon+bold-title+subtitle+chevron layout was structurally the same template Android's own system notifications use — sitting as the very first thing on screen, before any app branding, it could be mistaken for an OS-level notification rather than in-app content, a real concern given it's a safety-relevant message. Fixed two ways: (1) added a small "NATIONAL WEATHER SERVICE" eyebrow (`AccountBalance` icon, small-caps, alert's own color) above the title to self-identify the source at a glance — something an actual OS notification would never render in the app's own typographic voice; (2) moved the card from *before* the hero to *after* the "sky·speak" wordmark/icon row (still before the temperature), so the app's own branding is always visible above the alert, reinforcing "this is content inside SkySpeak" from the first frame rather than something floating above/outside the app |
| — | Third follow-up, user-reported: still didn't fit — the actual root cause was structural, not color/position within the hero. The hero is pure borderless typography (no cards anywhere in it); dropping a shadowed, bordered `GlassCard` into that space was always going to look like a foreign object no matter how it was tuned. Fixed by moving the alert out of the hero entirely: it's now the first card in the ordinary card flow, right after the hero, styled identically to `HourlyCard`/`DailyCard` below it (same margin, same 12dp vertical rhythm). Presented two options via mockups before implementing (this one, vs. an integrated no-shadow pill inside the hero matching the lookahead-pill language) — user picked moving it to the card flow. Kept the "NATIONAL WEATHER SERVICE" eyebrow from the previous fix, since the notification-template resemblance concern is independent of where the card sits |
| — | Alert resolution tracking (user-requested, same conversation): the app previously went silent when an alert cleared — a user who'd seen an earlier warning had no confirmation the danger had passed, they'd just notice its absence (or not). Added `WeatherViewModel.trackAlertChanges()`, which diffs each fetch's alert IDs against a persisted `PreferencesStore.getTrackedAlerts()` set; any ID that drops out is surfaced as a dismissible `ResolvedAlertCard` ("$event has ended") — sage green, checkmark icon, deliberately opposite the active-alert styling since this is reassurance, not a hazard. Renders below any currently-active alert, above `HourlyCard`. Tracking resets on a location change (`selectPlace`/`selectCurrentLocation`) so a previous city's cleared alert doesn't produce a false "resolved" notice for the new one. Verified end-to-end on-device: installed with a fake active alert, confirmed it was tracked, then rebuilt without it (same app data, no cache clear) and confirmed the real background refresh detected the drop and surfaced the resolved card; also verified the dismiss button and the Moderate-severity color tier |
| B11 | Bug fix, found during a documentation-sync audit (not by inspection alone — caught by re-reading the code against what CLAUDE.md claimed and then verifying on-device): `WeatherContent` rendered `resolvedAlerts` *before* the active-alert check in `WeatherScreen.kt`, so a resolved acknowledgment would appear above a currently-active alert whenever both existed at once — the exact opposite of the documented and intended "an ongoing hazard always outranks an acknowledgment" ordering. The in-code comment even asserted the correct behavior right above the code that contradicted it. Swapped the two blocks. Verified on-device with both present simultaneously (a real active Flash Flood Warning + a resolved Heat Advisory) before and after the fix |
| B12 | Bug fix, user-reported: user saw two identical-looking "Heat Advisory" cards for Franklin Park, NJ and asked whether the app had duplicated one. Investigated with live NWS data rather than guessing: confirmed the app's `distinctBy { it.id }` dedup was intact and correct, and confirmed via a live query that NWS genuinely issues multiple alerts sharing an identical event name for one point (found 3 simultaneously-active "Air Quality Alert" entries for that exact location, each with a distinct ID and none referencing the others as updates) — either from day-by-day reissuance or overlapping NWS zone coverage for a border location. Not a code bug, but a real legibility gap: (1) `formatNwsTime()` only formatted clock time with no date, so two alerts at the same time-of-day on different days rendered as literally identical text; fixed to include "Tomorrow, "/"Yesterday, "/"MMM d, " context except for today. (2) The primary alert card's subtitle showed NWS's raw `headline` prose, which often ellipsized before its differentiating "until X" time was visible; replaced with a new `alertTimeWindow()` helper built from the (now date-aware) `effectiveLabel`/`expiresLabel` fields, always a single short line, never truncated. Verified on-device with two same-named "Heat Advisory" alerts spanning different days — the compact card, the "+N more" list, and the detail sheet are now all clearly distinguishable |
| B13 | Follow-up bug fix, user-reported: user still saw "+1 more advisory" for a location with what they considered one advisory, even after B12. Re-investigated live: found the real root cause. For that exact point (Franklin Park, NJ), NWS is currently serving **two completely separate, independently-issued** "Air Quality Alert" products (fully distinct base IDs, not zone-suffix variants of one issuance) that cover the identical 3 UGC zones, `sent` a day apart, with empty `references` linking them — a rolling day-ahead reissuance pattern where consecutive ~30-36hr windows overlap. This is real NWS data (verified via `/alerts/active?point=` returning both, with full IDs, UGC codes, and `sent` timestamps compared side by side), not an app bug — but it's exactly the kind of accurate-but-confusing raw-API artifact NWS's own consumer-facing site collapses before showing the user. Added a `groupBy { (event, areaDesc) }` collapse step in `mapAlerts()` that keeps only the most-recently-`sent` entry per group (added a `sent` field to `NwsAlertProperties` and a `parseInstantOrMin()` helper for this). Verified on-device by replaying the exact real Franklin Park payload (both real IDs, real UGC-derived areaDesc, real `sent` timestamps): before the fix, showed "+1 more advisory"; after, shows one card with the newer issuance's (correct, longer) time window and no "+more" text |

All verified on-device in both light and dark theme, plus a full `assembleRelease` build to confirm nothing regressed under R8.

### Completed — Radar Removed, Animated Weather Background (2026-07-17)

User feedback: the radar map wasn't earning its place ("I don't see anyone going to like it as it is not that useful"). Removed it entirely rather than continuing to invest in it, and replaced the resulting gap with a full-screen animated background reacting to real conditions — the app's biggest single visual change since dark mode.

| # | Title |
|---|---|
| — | Removed `RadarScreen.kt` (OSMDroid + RainViewer), the `osmdroid` Gradle dependency, the `ACCESS_NETWORK_STATE` permission it required, and its ProGuard `@JsonClass` keep rule — which turned out to be the *only* thing in the codebase using `@JsonClass` at all, so the whole rule went with it. Also removed the now-dead `WeatherViewModel.lastLatLon`, which existed solely to center the radar map |
| — | New `WeatherBackground.kt`: a full-screen animated backdrop behind the entire scrolling `WeatherScreen`, not just the hero. A single shared `timeMs` clock drives every particle; positions are pure functions of `(timeMs, per-particle seed)` computed inside one `Canvas`'s draw phase, so each tick redraws only that Canvas, not the whole screen |
| — | 26 distinct `Scene` values across Clear/Cloud, Liquid Precipitation, Frozen Precipitation, Atmospheric Obscuration, and Convective/Severe categories, classified from real data only: WMO code (extended to split out freezing rain, sleet, and thunder+hail beyond what `WeatherRepository`/`WeatherAdvisor` need), `cloudCoverPct` (Partly/Mostly Cloudy/Overcast — a 3-way split the 4-value WMO code can't express alone), `visibility` (unit-aware mist-vs-fog split at the standard ~1km threshold), `aqi` (haze, and a smoky tint on fog). `Tornado`/`Hurricane` have no forecast code at all, so they're driven by real active NWS alert text instead ("Tornado Warning", "Hurricane Warning", etc.) — more honest than inventing a trigger. `Dust Storm`/`Volcanic Ash` are a deliberate exception: no code, no alert signal, no data source at all — built as real, complete scenes per an explicit product decision, but `classify()` can never actually select them today |
| — | A separate `severeWind` boolean (real `windKmh`/`windGustKmh` ≥ 45 km/h) layers wind-streak particles on top of *any* scene except Tornado/Hurricane — this is what makes heavy snow + high wind read as a blizzard without a dedicated `BLIZZARD` scene |

### Completed — Rain/Snow Accuracy Overhaul (2026-07-17)

User framing: "we are here to help save people's lives... that's the direction we are taking." Audited every place the app talked about precipitation and found it was only ever using Open-Meteo's type-agnostic `precipitation`/`precipitation_probability` fields — real, but unable to say whether it's rain or snow, which are different hazards.

| # | Title |
|---|---|
| — | Added Open-Meteo's real `rain`/`showers`/`snowfall` fields (current, hourly, daily) alongside the existing generic ones. Verified live against the API before writing any code: `snowfall` does **not** follow `precipitation_unit=mm` — it stays in cm, a real 10x unit mismatch, not just a label difference. Added `UnitSystem.snowLabel` rather than reusing `precipUnit` |
| — | The Precipitation metric tile now shows the real, correct type — icon, value, and description all reflect whichever is actually happening (current conditions first, then whichever type dominates the next 12 hours), not a generic "precipitation" figure that could quietly mean either |
| B14 | Bug fix, found while building the above: `WeatherAdvisor.umbrella()` (the free, always-available advice path — no API key needed) could recommend an umbrella on snow days. Its rain-percentage branch fired off `precipitation_probability`, which doesn't distinguish rain from snow, so a high reading read as "high chance of rain" regardless of actual type. Added a real `Ctx.isSnowy` signal (`currentSnowfall`/`hourlySnowfall`, falling back to a WMO-code check) checked before the rain logic in all six advice intents (`umbrella`, `jacket`, `walking`, `driving`, `hiking`, `clothing`), not just the one that was visibly wrong |
| B15 | Bug fix, user-reported: `SparklineTile`'s chart auto-scaled to its own local min/max — correct for an unbounded series like temperature, wrong for a 0–100 probability. A day where precipitation chance only wobbled between 10–25% rendered as a dramatic full-height peak, visually indistinguishable from a genuine 80–100% day. Added `MetricChart.fixedRange` and applied `0f..100f` to the Precipitation tile specifically; other sparklines (Feels Like, Wind) keep auto-scaling, which is correct for them |
| — | `PrecipDetailContent`'s hourly bar chart now colors snow-forecast hours with a distinct fixed color at every intensity, instead of running them through the rain-only green→red intensity ramp — using the same real per-hour `hourlySnowfall` data, threaded through a new `DetailSheet.Metric.hourlySnowfall` field. Its "HOURLY RAIN CHANCE" label (wrong on snow days, since the underlying data is type-agnostic either way) is now the honest "HOURLY PRECIPITATION CHANCE" |
| — | `ChatRepository.weatherBrief()` reports rain and snow as separate, explicit lines (`"snowing: 1.2 cm"` / `"rain: 0.4 mm"`, `"(2.1 cm snow)"` next to an hour/day) instead of one flat `precip` figure; the system prompt now tells the model a bare "% precip" reading doesn't imply rain |

### Completed — Weather Background Light/Dark Contrast Fixes (2026-07-17)

Both found by the user testing on a real device — text description alone wasn't enough to diagnose either correctly; a screenshot settled the first, and precise symptom language ("dark mode is okay, light mode isn't") settled the second.

| # | Title |
|---|---|
| B16 | Bug fix, user-reported + screenshot: a "frosted glass" translucent card fill (`GlassCard` alpha-blending `surface` at ~0.72–0.78 over the new animated background, gated by a `LocalTranslucentCards` composition local) produced a visibly patchy card with hard-edged seams, worst at the rounded corners where more of the busy animated background bled through than the flatter center did. Worse in light mode specifically — dark-on-dark blends forgivingly, light-on-colored doesn't. Real glassmorphism needs actual blur (`RenderEffect`, API 31+), not alpha blending alone. Reverted `GlassCard` to its original always-opaque fill and removed `LocalTranslucentCards` entirely rather than keep a half-working feature around; the animated background is unaffected and still shows in the hero and in the gaps/margins around cards |
| B17 | Bug fix, user-reported: "light mode does not show the changing background according to weather, dark mode seems okay." Root cause: `drawRain`/`drawSnow`/`drawSleet`/`drawHail`/`drawIceSheen`/`drawWindStreaks` all drew particles in white or near-white unconditionally. White pops against the dark-mode gradient; the identical white is nearly invisible against light mode's pale pastel sky stops — alpha tuning alone can't fix a hue that's fundamentally wrong for a light background. The `Scene` classification and base gradient *were* correctly changing per condition in light mode the whole time; the particle layer that would make that obvious just wasn't visible. Every particle renderer now takes a theme-aware `ink: Color` param — dark mode keeps white, light mode reuses this app's existing rain/snow "slate blue" tip-tone family (`tipColors()`) rather than inventing new hex values, plus a modest alpha bump since even a well-chosen color needs enough opacity to read as a 1–2px streak. `drawStars`/`drawThunderFlash` correctly keep plain white unconditionally — stars only render at night, when the sky gradient is dark navy in *both* app themes, and a lightning flash is inherently a white light burst |

Both verified by re-reading the full diff against the reported symptom before committing. Subsequently confirmed on a real release build too: the versionCode 9 sanity-install (see the Launch Checklist in `PLAYSTORE_LAUNCH.md`) showed the animated background rendering correctly and cards fully opaque with no seams, in both light and dark screenshots taken from the device.

### Pending — Priority 3 (larger scope)

| # | Title | Effort |
|---|---|---|
| 28 | Localization: replace hardcoded strings with `strings.xml` | 1–2 days |
| 29 | Weather-change push notification | 1–2 days |
| 30 | Share current weather | half-day |
| 33 | Onboarding walkthrough for new users | 1–2 days |
| 34 | Play Store screenshot text overlays | half-day (asset work, not app code) |

### Completed — Alert Display Redesign (2026-07-18)

User-reported: two related problems found in the same testing pass — the alert detail sheet cropped content with multiple active alerts, and separately, the main-screen alert card was judged too large for what it needed to show at a glance. Both are the same subsystem (`AlertBannerList`, `ResolvedAlertCard`, `DetailSheet.AlertList`), handled together.

| # | Title |
|---|---|
| B18 | Bug fix, user-reported: with multiple active NWS alerts, `DetailSheet.AlertList` cropped after one screen's worth of content — the 2nd/3rd alert's description/instructions were pushed off the bottom with no way to scroll to them. Root cause: `DetailSheetContent` wraps every sheet type's content in a plain `Column` with no scroll modifier; `ModalBottomSheet` does not add scrolling on its own, and every other sheet type (one metric, one day, one alert) happened to fit within the sheet's max height by chance, so this was a latent bug since the sheet system was built, not something new in this release — `AlertList` was just the first case to reliably exceed it, since it stacked full `AlertDetailContent` (description, instructions, badges) for every active alert in that same unscrollable Column. Fixed two ways, not one: (1) `DetailSheetContent`'s root `Column` now wraps in `Modifier.verticalScroll(rememberScrollState())` as a general safety net for every sheet type, not just alerts; (2) `DetailSheet.AlertList` no longer shows full content at all — it's now a tappable summary-row list (`AlertSummaryRow`: severity dot, event name, `alertTimeWindow()`), and tapping a row opens that one alert's full `DetailSheet.Alert` view via a new `onAlertSelected` callback threaded through `DetailSheetContent`. (1) alone would have technically resolved the crop, but left multiple simultaneous *safety-relevant* alerts buried under each other's full text, which is a worse failure mode here than in an ordinary list |
| — | Design change, user-requested: the main-screen alert indicator (`AlertBannerList`) shrunk from a full four-line `GlassCard` (icon + bold title + subtitle + chevron, plus a "NATIONAL WEATHER SERVICE" eyebrow and a separate "+N more" row) to a single-line severity strip — the same left-accent-bar language `TipBanner` already established elsewhere in the app, not a new visual pattern. This is a different axis of change from two *earlier*, already-reverted attempts at making the card visually exceptional (full-bleed/square, placed above the hero) — smaller, not differently-shaped — so it doesn't reopen that history; it still lives in the ordinary card-flow position. A `SeverityDotCluster` (one dot per active alert, each colored by that alert's own severity, capped at 4 with a "+N" fallback) replaces the old spelled-out "+N more" row, so someone can gauge overall risk ("one rust dot, one gold dot") without tapping in — user confirmed this addressed the stated requirement (severity color + alert count, both still visible, in less space). `ResolvedAlertCard` got the identical treatment (sage green) for consistency between the two — leaving it as a full card while the active-alert strip shrunk would have put more visual weight on the "resolved" state than the "active hazard" one |

Both parts verified by re-reading the full diff against each reported symptom and design requirement, then confirmed on-device against a real live case: South Brunswick Township, NJ was actively carrying two simultaneous NWS alerts (a Severe Flood Watch + an Air Quality Alert) at build time. Screenshots taken of the full flow — the compact strip showing two severity dots, tapping through to the `DetailSheet.AlertList` chooser (summary rows for both), and drilling into the Flood Watch's full `AlertDetailContent` (severity badge, urgency/certainty pills, description) — with a clean logcat throughout (no crashes across launch, strip tap, chooser, and detail drill-down). Shipped as versionCode 10 / 1.0.9.

### Completed — Android 16 Target SDK Compliance (2026-07-21)

Play Console's Policy status page flagged a new, separate-from-Production-access issue while the Production access application was still pending review: "App must target Android 16 (API level 36) or higher," action required by 2026-08-31 or the app loses the ability to publish any future update (existing live listings are unaffected).

| # | Title |
|---|---|
| B19 | Compliance fix: bumped `compileSdk` 35 → 36 and `targetSdk` 35 → 36 in `app/build.gradle.kts` (`app/build.gradle.kts:23,28`), shipped as versionCode 11 / 1.0.10. Low-risk for this codebase: edge-to-edge — the main behavioral surface API 36 changes — was already handled correctly via `enableEdgeToEdge()` (fixed for the API 35 requirement back in B5), and the app has no NDK/native code, no foreground services, and no other component types affected by Android 16's behavior changes. Verified with the same bar as every prior release: `assembleDebug`/`bundleRelease`/`lint` all `BUILD SUCCESSFUL` under Gradle 9.5.0 + AGP 9.3.0 with the new SDK levels (0 new lint issues, same 33 pre-existing warnings), `jarsigner -verify` on the signed AAB, and — new for this release — sanity-installed on a freshly-booted **API 36 emulator** specifically (`Medium_Phone_API_36.0`), not just an older device/emulator: confirmed `target_sdk_version=36` in `nativeloader` logs, a real forecast loaded with the animated background rendering correctly, navigated Weather → Chat → Settings (Settings correctly showed "Version 1.0.10"), zero `FATAL EXCEPTION`/crashes in logcat throughout |

See the "Android 16 (API 36) Target SDK Requirement" section in `PLAYSTORE_LAUNCH.md` for the full notice text and remaining upload steps.

### Completed — Hero Text Contrast Fix (2026-07-21)

User-reported: in light mode, the hero section (location, condition, H/L, feels-like) wasn't clearly legible against `WeatherBackground`'s animated scene — reproducible only at the user's real current location, Franklin Park, NJ. Invoked the `frontend-design` skill and reproduced for real on an API 36 emulator (GPS/city search set to Franklin Park, NJ, light theme) rather than guessing.

| # | Title |
|---|---|
| B20 | Bug fix, user-reported: `CurrentHeader` and the "sky\|speak" wordmark used the fixed app-wide `TextPrimary`/`TextSecondary` tokens, which don't adapt to the actual `WeatherBackground` scene they sit on top of (no card or scrim behind the hero, by design). Reproduced on-device: Franklin Park, NJ's real current condition (Overcast, 95% cloud cover) renders `WeatherBackground`'s flat gray `OVERCAST` rect blended over the sky gradient — pixel-sampled from a real screenshot at RGB ~(193,195,195). `TextPrimary` (near-black) still contrasted fine there, but `TextSecondary` (`onSurfaceVariant`, a mid-gray, ~0xFF78848F) measured only **~2.2:1** against it — a real WCAG failure (floor is 4.5:1 for body text), which is why "FRANKLIN PARK", "Overcast", "H:83° · L:67°", and "Feels like" all read as washed-out gray-on-gray in the reported screenshot while the temperature stayed perfectly legible. Investigating further (via contrast-ratio calculation against the app's own real hex values, not guessing) turned up a second, worse, related failure not directly reproducible at the user's location today: `conditionGradient`'s light-mode sky is deliberately *dark* for night (`0xFF1A2448`) and thunderstorms (`0xFF3A2F50`) regardless of app theme — weather doesn't get lighter because the user's theme preference is light — so the fixed dark `TextPrimary`/`TextSecondary` pair measured **~1.1:1** there, effectively invisible. Both are the same root cause (fixed theme text vs. a data-driven background) so fixed with one mechanism: new `heroBackdropIsDark()` in `WeatherBackground.kt` (reuses the existing private `classify()` so it can never drift from the actually-drawn `Scene`) and `heroTextColors()` in `WeatherComponents.kt`, wired into `WeatherScreen.kt`'s `WeatherContent`. Dark theme is untouched (its sky stops are already recalibrated dark enough — see B17/the Design Audit section). In light theme: night/thunder/thunder+hail/tornado/hurricane/volcanic-ash scenes flip to a light-on-dark pair borrowed verbatim from the dark theme's own `onBackground`/`onSurfaceVariant`; every other scene keeps `TextPrimary` unchanged but swaps the secondary tone for `0xFF3F5670` — reused from `tipColors()`'s existing RAIN tone and `drawRain`'s light-mode ink (B17) rather than inventing a new color. Measured after the fix: ~4.29:1 for the exact reproduced Overcast case (pixel-sampled again from a real after screenshot: text `0xFF3F5670` against background ~(196,194,202)), up to ~5.6:1 for lighter pastel scenes (calculated against Snow's sky stop), and ~4.3–12:1 for the dark-backdrop flip pair against Thunder's and Night's sky colors. Two considered-and-rejected alternatives, consistent with this app's own design history: a translucent panel behind the hero (already tried and reverted, B16 — patchy seams, worse in light mode) and a bordered/shadowed card treatment (already tried and reverted for the alert banner in the Design Audit section — the hero is deliberately borderless typography, and the "system notification" look was the specific thing user feedback rejected there). Verified on-device: before/after screenshots at the identical real Franklin Park, NJ location and time, both pixel-sampled (not eyeballed) to confirm the measured contrast improvement; `assembleDebug`/`lint` both `BUILD SUCCESSFUL` with the change |

Not yet committed — left staged for review, per the user's usual workflow of driving commits themselves.

### Completed — Hero Font Weight Fix (2026-07-21)

User-reported, same conversation as B20: "the font in the hero section looks weak" in light mode, "not that prominent in dark mode" — a related but distinct issue from the color-contrast fix above (this one persists even with correct contrast).

| # | Title |
|---|---|
| B21 | Bug fix, user-reported: even after B20 fixed hero text *color* contrast, the hero's already-light font weights (`Thin` for the 96sp temperature, `Light` for the "sky\|speak" wordmark, `Normal` for location/condition/H-L/feels-like) read as visually weak specifically in light theme. Root cause is a real optical effect, not a rendering bug: dark text on a light background reads thinner than the identical weight in light text on a dark background (irradiation — a bright surround makes a dark shape look smaller/thinner than the same shape reversed) — confirmed by the user's own direct comparison between the two themes, not assumed. Fixed with `heroWeight(base: FontWeight)` in `WeatherComponents.kt`: steps every hero weight up one level in light theme only (`Thin→ExtraLight`, `Light→Normal`, `Normal→Medium`, `Medium→SemiBold`), returns `base` unchanged in dark theme. Applied to every `CurrentHeader` text element and the wordmark — the same element set `heroTextColors` (B20) covers. Verified on-device at Franklin Township, NJ (same region/conditions as B20's Franklin Park repro): light-mode hero visibly reads heavier in an after screenshot compared to the B20 fix alone; dark mode re-screenshotted and confirmed unchanged, as designed (the function returns the original weight there) |

Verified with `assembleDebug`/`lint` both `BUILD SUCCESSFUL`. Not yet committed — left staged for review alongside B20.

### Completed — Design Upgrades (2026-06-30)

| # | Title | Impact |
|---|---|---|
| D1 | Time-of-day hero gradient tinting | High |
| D2 | Hourly strip edge fade (scroll affordance) | Medium |
| D3 | Sparkline "Now" dot — slow pulse animation | Low–Medium |
| D4 | Chat empty state with example prompts + hasKey note | High (AI positioning) |
| D5 | Daily forecast temperature range bar | High — was already implemented |
| D6 | Radar timestamp badge — GlassCard styling | Low |

### Completed — Launch Prep & Fixes (2026-07-01)

| # | Title |
|---|---|
| L1 | `applicationId` changed off `com.example.*` for Play Store eligibility (`io.github.smenon2710.skyspeak`); Kotlin namespace/package intentionally left unchanged |
| L2 | Privacy policy written and hosted via GitHub Pages (`docs/privacy.html`) |
| L3 | Monetization decided: donation model — in-app "Support the developer" link added to `AttributionFooter`, keeps the app within Open-Meteo's non-commercial free tier |
| L4 | Consolidated three overlapping Play Store launch docs into one `PLAYSTORE_LAUNCH.md` |
| B1 | Fix: `WeatherScreen`'s permission-launcher callback was calling a cold `load()` on every screen re-composition (cold start, and every return from Chat/Radar), flashing visible content back to a full loading spinner before the refetch completed |
| L5 | Release keystore generated and wired into `app/build.gradle.kts` signingConfigs (`STORE_PASSWORD`/`KEY_PASSWORD` from gitignored `local.properties`) |
| B2 | Fix: `bundleRelease` failed with "Unresolved reference 'logging'" — the `if (BuildConfig.DEBUG)` guard around `HttpLoggingInterceptor` in `NetworkModule` doesn't stop the *import* from being compiled, and `logging-interceptor` is a debug-only dependency. Fixed with a `src/debug`/`src/release` source-set split (`addDebugLogging()` extension in `DebugLogging.kt`) |
| L6 | Release build verified end-to-end on-device (installed via `adb -t` due to an unrelated Android Studio baseline-profile deploy bug) — loads, renders, and the donation link works in the actual signed release build |
| L7 | Fixed Data Safety table inconsistency: location sharing with Open-Meteo was previously marked "No", contradicting `docs/privacy.html` |
| L8 | Drafted Play Console content: detailed Data Safety form answers, app title/short/full description (character counts verified) — see `PLAYSTORE_LAUNCH.md` |
| B3 | Fix: `RadarScreen`'s header/back button was completely invisible (drawn over by the native `MapView`) in every release build, though still tappable underneath — a real dead-end-screen bug, not just a screenshot issue. Root cause: `MapView`'s own drawing bled past its Compose-measured bounds (confirmed via `uiautomator dump` — the view's layout bounds were correct, only its painted content overflowed). Fixed with `Modifier.clipToBounds()` on the map's `Box`. Also removed a redundant manual `layoutParams` assignment in the `AndroidView` factory (a known Compose interop anti-pattern) that turned out not to be the actual cause |
| L9 | Captured Play Store screenshots (Weather, Chat, Radar) from a release build running on an emulator |

### Completed — Internal Testing Fixes (2026-07-02)

| # | Title |
|---|---|
| B4 | Fix: `RadarScreen` playback controls (play/pause button, scrubber, timestamp badge) silently failed to render whenever the RainViewer frames fetch failed — found by an internal tester whose map tiles loaded fine but no controls ever appeared. First pass added a `retryTrigger` state re-keying the fetch `LaunchedEffect` plus a "Couldn't load radar / Retry" state in the map `Box`, since the fetch's `catch (_: Exception) { }` gave no visible signal of failure. Retest showed the fetch failing consistently for every tester, pointing at the release build itself rather than network flakiness — **actual root cause**: `RainViewerResponse`/`RadarFrames`/`RadarFrame` are `@JsonClass` Moshi models defined locally in `RadarScreen.kt` (`com.example.weatherly.ui` package), outside `data.model.**`, the only package `proguard-rules.pro` had a keep rule for. R8 (`isMinifyEnabled = true` in release) stripped them, breaking Moshi's reflective `KotlinJsonAdapterFactory` adapter at runtime in every release build — this was very likely the true cause of the original "play button not visible" report too, not just this retest. Fixed with a package-agnostic `-keep @com.squareup.moshi.JsonClass class * { *; }` rule so no Moshi model anywhere in the codebase can silently break this way again. The retry UI from the first pass stays as a legitimate defensive fallback for genuine network failures |

### Completed — Play Console Technical Quality Fixes (2026-07-13)

| # | Title |
|---|---|
| B5 | Fix: Play Console flagged "deprecated APIs or parameters for edge-to-edge" against release 5 (1.0.4). Root cause: `android:windowSoftInputMode="adjustResize"` on `MainActivity` in `AndroidManifest.xml`, left over from before `enableEdgeToEdge()` was adopted. On API 35, edge-to-edge apps are expected to handle the IME via `WindowInsets` directly — `ChatScreen.kt` already did this correctly via `.imePadding()` — so the manifest attribute was both redundant and the actual deprecated parameter being flagged. Fixed by removing the attribute entirely; no other screen has a text input, so nothing else depended on it. Also bumped `androidx.glance:glance-appwidget` 1.1.0 → 1.1.1 in `app/build.gradle.kts`, since 1.1.0 transitively pulled in `glance-appwidget-proto`/`glance-appwidget-external-protobuf` versions affected by CVE-2024-7254 (the "critical note" Play Console surfaced under Technical quality) — 1.1.1 is the patched release. Also added `ndk { debugSymbolLevel = "FULL" }` to the release build type so future signed AABs automatically bundle native debug symbols, resolving a separate (non-blocking) Play Console notice about missing symbol files for crash/ANR analysis. `versionCode`/`versionName` bumped 5/1.0.4 → 6/1.0.5 to ship these fixes as a new release to the existing `skyspeak-testers` closed-testing track (does not reset the 14-day tester clock). |
| B6 | Fix: uneven row spacing in the 7-day forecast list, found via direct visual review (2026-07-13). Root cause: in `DailyCard` (`WeatherComponents.kt`), each day's icon column is a plain `Column` (no fixed height) containing the `WeatherGlyph` plus, on precipitation/fog days, an extra `"$it%"` text below it. Since `Row` height is driven by its tallest child, rows with the extra percentage text render taller than rows without one, even though every row shares the same `padding(vertical = 8.dp)` — breaking the list's vertical rhythm. Fixed by giving the icon column a fixed `height(40.dp)`, so every row is the same height regardless of whether the percentage text renders. |

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

## Tier 3 — Larger features ✅ Completed

### 10. ✅ Offline last-known forecast (survive cold start with no network)

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

### 11. ✅ Adaptive launcher icon

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

### 36. Manual theme toggle (Light / Dark / System)

**Source:** External tester feedback (2026-07-13, `Testers Community` report) flagged "no dark mode" as missing — inaccurate, since the app has had a fully system-aware dark theme since item 6. The real gap the tester was reacting to: there's no explicit in-app toggle, only automatic system-theme following (`isSystemInDarkTheme()` in `WeatherlyTheme`), so a tester who never changed their device-level theme setting would never see it and could reasonably conclude dark mode "doesn't exist." Standard apps offer an explicit Light/Dark/System three-way choice independent of the OS setting — worth adding for discoverability, not because dark mode itself is missing.

**Approach:** Add a persisted `ThemePreference` (LIGHT/DARK/SYSTEM) to `PreferencesStore`. `WeatherlyTheme`'s `darkTheme` parameter resolves from the stored preference (falling back to `isSystemInDarkTheme()` when SYSTEM is selected) instead of always reading the system value directly. Exposing the toggle needs a settings surface — natural to bundle into the same Settings screen as items 25 and 35 rather than building three separate entry points.

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

### 33. Onboarding walkthrough for new users

**Source:** External tester feedback (2026-07-13, `Testers Community` report). No first-launch walkthrough or tooltips exist today — a new user opens straight into the live weather screen with no guidance on chat/widget.

**Approach:** A brief interactive first-launch sequence (2–3 screens) highlighting the AI chat assistant and the animated weather background, with a skip option. Persist "seen" state in `PreferencesStore` so it only shows once. Genuine scope item, not a quick fix — sequence alongside items 25/35/36 if a Settings screen is built (could add a "Replay walkthrough" entry there too).
*(Updated: originally also called out the radar map, which was removed as low-value — see the git history around WeatherBackground.kt's introduction. Settings screen shipped 2026-07-13.)*

---

### 34. Play Store screenshot text overlays

**Source:** External tester feedback (2026-07-13). Current screenshots (`store_assets/screenshot-0{1,2,3}-*.png`, per the L9 entry above) already show real app screens (Weather/Chat — the third was Radar, since removed and due for a reshoot), not generic mockups — the tester report's framing was slightly off — but adding concise text overlays ("Rain, snow, and fog — shown as they really are", "AI assistant grounded in your forecast") would still make them more effective on the store listing.

**Approach:** Pure asset work — re-render the three screenshots with a short text callout per image, same tooling as `store_assets/render_feature_graphic.py`. No app code changes; re-upload to Play Console store listing when ready.

---

### 35. "Rate this app" prompt in settings

**Source:** External tester feedback (2026-07-13). No in-app path exists to prompt for a Play Store rating.

**Approach:** Use the Play Core **In-App Review API** (`com.google.android.play:review-ktx`) rather than a raw settings link — it lets Google throttle/gate the prompt per its own quota rules instead of showing every time. Trigger after a positive interaction (e.g., a completed AI chat exchange), not on cold start. Needs the same Settings screen as items 25/36 to also offer a manual "Rate the app" entry point for users who want to rate anytime, not just when prompted.

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

---

## Design Upgrades ⬜ Pending

### D1. Time-of-day hero gradient tinting

**What:** The condition gradient (`conditionGradient` in `WeatherComponents.kt`) already adapts to WMO code + `isDay`. Add a time-of-day warm/cool tint blended into the first stop so the hero changes subtly through the day. Dawn and dusk are the highest-anxiety weather-check moments — lean into them visually.

**Tint map (blend at ~15–20% alpha over the existing condition color):**

| Hour | Tint | Hex |
|---|---|---|
| 5–7 AM (dawn) | warm apricot | `#E8936A` at 18% |
| 7–17 (day) | no tint | — |
| 17–19 (golden hour) | amber | `#D4A44C` at 15% |
| 19–21 (dusk) | dusty violet | `#A0668A` at 20% |
| 21–5 (night) | already handled by `isDay = false` | — |

**Implementation:** In `conditionGradient`, read `Calendar.HOUR_OF_DAY`, pick the tint color + alpha, and use `lerp(baseColor, tintColor, alpha)` for the first gradient stop. No new parameters needed. `lerp` is in `androidx.compose.ui.graphics.lerp`.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt`

---

### D2. Hourly strip edge fade (scroll affordance)

**What:** The `LazyRow` in `HourlyCard` hard-clips at the card edge, giving no cue that more hours are scrollable. Wrap the `LazyRow` in a `Box` and overlay a `Brush.horizontalGradient` from `AppBackground → transparent` on the right edge (24dp wide) and a mirror fade on the left once scrolled past item 0.

**Implementation:**
```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    LazyRow(...) { ... }
    // Right fade
    Box(modifier = Modifier
        .align(Alignment.CenterEnd)
        .width(28.dp).fillMaxHeight()
        .background(Brush.horizontalGradient(listOf(Color.Transparent, AppBackground))))
    // Left fade — only show after scroll
    if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
        Box(modifier = Modifier
            .align(Alignment.CenterStart)
            .width(20.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(AppBackground, Color.Transparent))))
    }
}
```

Use `rememberLazyListState()` and pass it to `LazyRow`. `AppBackground` must be captured as a local val before the lambda.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt` (`HourlyCard`)

---

### D3. Sparkline "Now" dot — slow pulse animation

**What:** The filled circle at index 0 on each `SparklineTile` marks the present moment but is static. A gentle scale + alpha pulse (1.0 → 1.4 → 1.0 over ~2.4 s, infinite) reads as "live data" without distracting from the chart.

**Implementation:** In `SparklineTile`'s Canvas block, the now-dot is drawn with `drawCircle`. Hoist the dot draw out of the Canvas into an overlay `Box`, or — easier — animate a `scale` value via `rememberInfiniteTransition` and pass it into the Canvas as a captured val (Compose will recompose the Canvas on each animation frame):

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "pulse")
val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f, targetValue = 1.4f,
    animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ), label = "pulse"
)
// Inside Canvas:
drawCircle(color = accent.copy(alpha = 0.28f), radius = 3.5.dp.toPx() * pulseScale,
    center = Offset(xAt(0), yAt(values.first())))
drawCircle(color = accent, radius = 3.5.dp.toPx(),
    center = Offset(xAt(0), yAt(values.first())))
```

Respect `LocalReduceMotion` (skip animation if user has reduced motion enabled):
```kotlin
val reduceMotion = LocalReduceMotion.current
val pulseScale = if (reduceMotion) 1f else /* animated value */
```

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt` (`SparklineTile`)

---

### D4. Chat empty state with example prompts

**What:** `ChatScreen` shows blank space above the suggestion chips when no conversation has started. Users don't know what to ask an AI in a weather app. Two or three example queries in italic `TextSecondary` — removed as soon as the first message is sent — teach users the AI's real value (planning + context queries, not "will it rain?").

**Example prompts to show:**
- *"Best day this week for a long run?"*
- *"I'm flying out Saturday — what's the weather like at my destination?"*
- *"Packing for 4 days in Boston — what should I bring?"*

**Implementation:** In `ChatScreen`, when `messages.isEmpty() && streamingText.isEmpty()`, show a `Column` centered in the message area with these strings as `Text(style = TextStyle(fontStyle = FontStyle.Italic), color = TextSecondary, fontSize = 14.sp)`. Wrap each in a `clickable` that populates the input field.

Also: check `viewModel.hasKey` here. If false, show an additional note: *"Tip: add your OpenRouter key in Settings to enable AI replies."* This surfaces the key-entry gap (currently `hasKey` is defined in `ChatViewModel` but never checked in the UI).

**File:** `app/src/main/java/com/example/weatherly/ui/ChatScreen.kt`

---

### D5. Daily forecast temperature range bar

**What:** `DailyCard` rows show H/L as text (e.g., `H: 31°  L: 18°`). Without week context, users can't tell if today's high is near the weekly peak or trough. A 28–32dp wide horizontal bar per row — scaled to `weekMin`/`weekMax` on `WeatherData` — with the day's L→H range filled in the accent color gives instant week-level temperature context at a glance.

**Data available:** `WeatherData.weekMinC` and `WeatherData.weekMaxC` are already on the model (same-unit caveat: they match the current unit system). `DayEntry.highC` and `DayEntry.lowC` are the per-day endpoints.

**Visual:** A `Canvas` element 28dp wide × 14dp tall, positioned between the day label and the H/L text. Track: `TextSecondary` at 15% alpha, rounded caps. Filled range: accent color at 70% alpha, also rounded. Scale: `((value - weekMin) / (weekMax - weekMin)).coerceIn(0f, 1f)`.

**Implementation note:** `DayEntry` currently receives the data — `weekMin`/`weekMax` need to be passed into `DailyCard` or propagated down. Since `WeatherData` is already passed to `MetricsGrid` and `DailyCard`, this is straightforward. Pass `data.weekMinC` and `data.weekMaxC` into `DailyCard` → `DailyRow`.

**Files:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt` (`DailyCard`, `DailyRow`)

---

### D6. Radar timestamp badge — GlassCard styling

**What:** The timestamp badge in `RadarScreen` (top-right corner showing the frame's UTC time) is currently a plain styled `Box` with a hardcoded background. It reads like a debug overlay rather than a polished UI element.

**Change:** Replace the custom background with a `GlassCard`-style surface — same shadow, border, and `colorScheme.surface` fill as the metric tiles — at a smaller `corner = 14.dp` and `padding = 8.dp`. One line of change in `RadarScreen.kt`. The badge will then visually belong to the same design system as the rest of the app.

**File:** `app/src/main/java/com/example/weatherly/ui/RadarScreen.kt`

---

## AI Assistant — Strategic Note (2026-06-30)

Average weather app session: **60–90 seconds, 3–5 sessions/day.** Sessions are too short for a conversational interface to be the primary interaction model — which is why the local `WeatherAdvisor` chips (zero latency, zero API cost) should handle the daily-use questions.

The AI's job is the **weekly planning query**: *"What's the best day for a BBQ this week?"*, *"I'm hiking Saturday — what gear do I need?"* These require multi-day synthesis and personal context that no chart can answer. The name "SkySpeak" makes AI load-bearing to the brand — removing it would create a bigger problem than keeping it.

**One fix needed before ship:** `ChatViewModel.hasKey` is defined but `ChatScreen` never checks it. A user with no API key hits a dead chat input with no explanation. Fix this as part of D4 (empty state) or item 25 (key-entry settings UI).

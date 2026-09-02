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

Verified with `assembleDebug`/`lint` both `BUILD SUCCESSFUL`. Committed alongside B20.

### Completed — Hero Backdrop-Darkness Detection Fix (2026-07-21)

Found during release prep for versionCode 11 — not user-reported, caught by the project's own sanity-install process. Selecting Franklin Park, NJ on the signed release build (real device time put it at night there) reproduced a variant of the B20 bug that B20 itself didn't cover.

| # | Title |
|---|---|
| B22 | Bug fix, found via on-device release-build sanity testing: `heroBackdropIsDark()` (added for B20) derived hero-darkness from `WeatherBackground`'s `classify()` — the decision tree that picks which *particles* to draw — checking only whether the result was `Scene.CLEAR_NIGHT`/`FAIR_NIGHT`/`THUNDER`/etc. But `conditionGradient()` (the function that actually paints the hero's background color) is a *separate* decision tree with different branch order: its codes-2-3 (Overcast/Cloudy) case doesn't check `isDay` in `classify()`, yet `conditionGradient`'s own `sky` selection *does* fall through to a dark navy `!isDay` branch for those same codes, since Overcast isn't one of the four explicit precip/fog ranges checked first. Net effect: night + Overcast (and likely night + any other cloudy/clear/haze condition) painted a dark backdrop that `heroBackdropIsDark()` didn't detect, so hero text stayed in the light-backdrop (dark-text) pair — reproduced live at Franklin Park, NJ at night on the signed release APK, pixel-sampled: temperature text (`0xFF2B2F36`) measured only ~2.3:1 against the real background (~RGB 93,100,122), the same failure class as B20's original Overcast case. Fixed by rewriting `heroBackdropIsDark()` to directly mirror `conditionGradient`'s own `sky` branch order/ranges (`code in 95..99`/`71..86`/`51..82`/`45..48`/`!isDay`/else) instead of reusing `classify()`'s `Scene`, plus keeping the alert-driven Tornado/Hurricane check separately (since `conditionGradient` has no alert awareness — `WeatherBackground` layers its own dark overlay rect for those independent of the base gradient). Simplified the function's signature in the process — it no longer needs `cloudCoverPct`/`visibility`/`visibilityUnit`/`aqi`, since none of those affect `conditionGradient`'s sky color. Verified on the exact same live repro (signed release APK, Franklin Park NJ, real nighttime data): temperature now renders `0xFFE0E6ED` against the same dark background, ~4.68:1 contrast; re-verified Weather/Chat/Settings navigation with zero crashes |

This is exactly the kind of thing the project's "always sanity-install the real signed build, don't just trust a successful compile" practice (see the Launch Checklist history) exists to catch — a scenario the light-mode-daytime testing in B20/B21 simply never exercised. Verified with `assembleRelease`/`lint` both `BUILD SUCCESSFUL`.

### Open — Android Vitals Findings, First Production Release (2026-07-27)

Play Console's **Monitor and improve → Android vitals → Overview** surfaced "5 actions recommended" against the versionCode 11 (1.0.10) build within ~10 minutes of it going live in Production — all `Technical quality`/`User experience` recommendations, none of them policy violations or anything blocking the app's continued publish/update ability (that's a separate, already-satisfied requirement — see the Android 16 target SDK section above). Investigated each against the actual codebase (`app/build.gradle.kts`, source tree) rather than acting on the Play Console text alone, since three of the five clearly share one root cause.

**Root cause hypothesis, three of five findings:** every AndroidX/Compose dependency in `app/build.gradle.kts` was last bumped around when `compileSdk`/`targetSdk` was still 35 (see dependencies block, e.g. `compose-bom:2024.12.01`, `androidx.core:core-ktx:1.13.1`, `androidx.activity:activity-compose:1.9.3`, `androidx.glance:glance-appwidget:1.1.1`, `com.google.android.gms:play-services-location:21.3.0`) — B19's Android 16 fix bumped only `compileSdk`/`targetSdk`, not the library stack itself. That stack is now well over a year stale relative to this release, which is a plausible single explanation for the fragment/edge-to-edge findings below, since none of the flagged deprecated calls (`setStatusBarColor`, `setNavigationBarColor`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`) appear anywhere in this app's own source — confirmed via `grep` across `app/src/main` — so they're coming from inside a library's own internals, not app code. `MainActivity.kt` already calls `enableEdgeToEdge()` correctly (the fix from B5), so the app-code side of edge-to-edge handling isn't the gap.

| # | Play Console finding | Investigation | Recommended fix |
|---|---|---|---|
| V1 | "Your app uses an outdated SDK version of androidx.fragment:fragment" (reports 1.1.0, recommends 1.2.1+) | Not a direct dependency anywhere in `app/build.gradle.kts` — confirmed via grep. It's pulled in transitively, most likely via `activity-compose`, `play-services-location`, or `glance-appwidget`, all of which are themselves stale (see above). | Bump the direct dependencies that plausibly pull it in (`androidx.activity:activity-compose`, `androidx.glance:glance-appwidget`, `com.google.android.gms:play-services-location`) to their current latest stable releases; re-check this vitals page after the next release to confirm the transitive version moved. Don't hand-pin `androidx.fragment` directly unless the bump alone doesn't resolve it — this app has no direct UI dependency on Fragments (it's 100% Compose), so a pin would just be papering over the real transitive source. |
| V2 | "Edge-to-edge may not display for all users" — "apps targeting SDK 35 will display edge-to-edge by default... investigate this issue" | Likely the same root cause as V3 below (deprecated internals in a stale library), not a gap in this app's own edge-to-edge handling — `enableEdgeToEdge()` is already correctly called in `MainActivity.kt` (verified working on a real API 36 emulator during the B19 release). | Same fix as V1/V3 (dependency bump), then a real on-device visual check across Weather/Chat/Settings on a fresh API 36 device/emulator to confirm insets still render correctly post-bump — this is a user-experience finding, not just a lint warning, so it needs an eyes-on check before considering it closed, same bar as every other release in this project. |
| V3 | "Your app uses deprecated APIs or parameters for edge-to-edge" — `android.view.Window.setStatusBarColor`, `setNavigationBarColor`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`; starts in `androidx.core.graphics.drawable.IconCompat$Api23Impl.toIcon` plus two obfuscated symbols (`na0.b`, `f2.o`) | None of these three deprecated APIs appear in this app's own source (confirmed via grep) — the stack trace starting in `androidx.core`'s `IconCompat` confirms this is internal to the `androidx.core` library itself (currently pinned to `1.13.1`), not app code needing a migration. | Bump `androidx.core:core-ktx` to its current latest stable release — this is very likely a library-internal fix already shipped upstream in a version newer than 1.13.1, not something to migrate by hand in this codebase. |
| V4 | "Improve your app's performance with bitmap downsampling" — `BitmapFactory` used without `Options.inSampleSize` in obfuscated class `cr0.c` | `BitmapFactory` does not appear anywhere in this app's own source (confirmed via grep). The only dependency in this codebase that renders bitmaps at all is `androidx.glance:glance-appwidget` (Glance renders widget composables to bitmaps for `RemoteViews`) — `cr0.c` is consistent with an R8-obfuscated class from that library, currently pinned to `1.1.1`. | Not directly fixable in this codebase's own source since it's inside a third-party library's internals. Bump `androidx.glance:glance-appwidget` as part of the same dependency-refresh pass as V1–V3 and re-check whether a newer release addresses it; if not, this one may simply need to wait on upstream and isn't worth working around locally. |
| V5 | "Improve your app's memory and performance with R8 optimisation" — "Optimised resource shrinking isn't enabled" | `app/build.gradle.kts`'s release build type already sets `isShrinkResources = true` (added back at initial launch prep, item L-something / "Add `isShrinkResources = true`" in the original Launch Checklist) — so *some* resource shrinking is on. Play Console appears to be asking for a newer/stricter shrinking mode specific to whatever this project's current AGP version (9.3.0, or 9.3.1 per the uncommitted bump sitting in the working tree) actually calls it — **not confirmed**, since guessing the wrong Gradle DSL syntax here risks silently breaking `bundleRelease` (a release-signing build), which is a worse outcome than leaving this one finding open a bit longer. | **Needs research before touching `build.gradle.kts`** — check Android Studio's own Build Analyzer suggestion (it usually offers a one-click "apply fix" for this exact recommendation) or the AGP 9.3.x release notes for the actual current flag/DSL name, rather than a blind guess here. Do this as part of the same release that resolves V1–V4, verified with a full `assembleDebug`/`bundleRelease`/`lint` pass same as every prior release in this project, not just a successful compile. |

**Not yet done:** none of V1–V5 have been implemented as code changes — this section is the investigation and fix plan only, written up before touching any dependency versions so the reasoning is on record. Bundling all five into one dependency-refresh release makes sense given the shared root cause for V1–V4; V5 needs its own confirmed-correct fix rather than being bundled in on a guess. Whenever this ships, it'll be the first post-launch update to a **live Production app**, so the same discipline the launch releases used (`assembleDebug`/`bundleRelease`/`lint` clean, `jarsigner -verify`, a real on-device sanity install navigating all three screens with a clean logcat) still applies — arguably more so now that real users, not just closed testers, are on the other end of a bad release.

### Completed — Store Listing Content Fix (2026-07-27)

Found while spot-checking the live Play Store listing right after the Production launch, using the actual copy pasted back from the live page rather than assuming Console matched this repo's drafted content.

| # | Title |
|---|---|
| B23 | Content bug, found via direct comparison of the live listing text against `PLAYSTORE_LAUNCH.md`'s drafted description: the Store listing's full description ("About this app") was stale by two shipped features. It still described "Live precipitation radar with play/pause and a frame scrubber" and OpenStreetMap/RainViewer attribution — the entire radar feature was removed from the app on 2026-07-17 (see "Completed — Radar Removed, Animated Weather Background" above), so a prospective user reading the live listing would expect a feature that doesn't exist. It also still described the old static per-condition hero gradient rather than the full-screen animated `WeatherBackground` that replaced it the same day, and omitted both the NWS severe-weather-alerts feature (shipped 2026-07-16) and the rain-vs-snow accuracy work entirely. Root cause: the corrected description was drafted in `PLAYSTORE_LAUNCH.md` back on 2026-07-17 but the Console-side paste was tracked as an open checklist item that never got done before Production launch — Console holds its own independent copy of the listing text, so editing this file alone was never going to fix it. Fixed by pasting the corrected description (already-drafted, unchanged) into Play Console → Store presence → Main store listing and submitting for review. Screenshot count on the live listing was confirmed correct (5, matching `store_assets/`); individual screenshot content wasn't independently re-verified. The Data Safety summary card on the live listing showed only "Location" and not chat data — not confirmed as an actual form gap vs. an abbreviated public-facing summary view; flagged for a direct check next time the Data Safety form is open in Console |

### Completed — Dependency Refresh (Android Vitals V1–V3) & Internal Testing Track Fix (2026-08-03)

User caught this by checking Play Console directly rather than assuming versionCode 11's Production publish closed the loop: the Android 16 target-API warning was still showing a full week after publish, now with specific text ("Your highest non-compliant target API level is Android 15 (API level 35)") that ruled out simple re-scan lag. Investigated with real Play Console screenshots rather than guessing: the **Internal Testing track had never been updated past versionCode 4 (1.0.3, targetSdk 35)** — visible in the "Latest releases and bundles" table as still "Available to internal testers," 0% install base, but an *active* release. Google's target-API policy evidently scans every active track, not just Production, so a stale unused Internal Testing release was enough to keep tripping the warning even with versionCode 11 (targetSdk 36) at 100% on both Closed Testing and Production.

| # | Title |
|---|---|
| — | Attempted fix: promote versionCode 11 to Internal Testing. Blocked two ways — uploading the same AAB failed with "Version code 11 has already been used" (Play Console requires versionCodes to be unique app-wide, not per-track), and "Promote release" from Closed Testing/Production didn't offer Internal Testing as a destination at all (promotion appears to only go "up" the ladder). **Conclusion, applies to every future release:** upload to Internal Testing *first*, then promote up through Closed Testing → Production — the reverse of how versionCode 6 through 11 were all actually handled (uploaded directly to Closed Testing, Internal Testing left behind) |
| V1/V2/V3 | Fixed via dependency bump, addressing the three Android Vitals findings that shared a stale-dependency root cause (see the Open Android Vitals section above): `compose-bom` 2024.12.01 → 2026.06.01, `androidx.core:core-ktx` 1.13.1 → **1.17.0**, `androidx.activity:activity-compose` 1.9.3 → 1.13.0, `androidx.lifecycle:lifecycle-runtime-compose`/`-viewmodel-compose` 2.8.7 → **2.10.0**, `com.google.android.gms:play-services-location` 21.3.0 → 21.4.0. The absolute-latest `core-ktx` (1.19.0) and `lifecycle` (2.11.0) were tried first and rejected by the build itself — `checkDebugAarMetadata` failed, both now require `compileSdk 37`, which this project doesn't have (a separate, larger change than intended here). Stepped back to the newest versions confirmed compatible with `compileSdk 36` |
| V4 | Confirmed still unfixable by version bump: `androidx.glance:glance-appwidget` has had no stable release since 1.1.1 (Oct 2024), per the official AndroidX release notes — exactly as predicted when this was first investigated. Left open, waiting on upstream |
| V5 | Not directly touched — still no confirmed-correct DSL to apply. Notable side-observation: `assembleRelease` under AGP 9.3.1 ran a task literally named `optimizeReleaseResources`, suggesting optimized resource shrinking may already be active automatically now that the project is on AGP ≥9.0 with `isShrinkResources = true` (AGP's own release notes describe this as becoming standard behavior at 9.0). Not confirmed resolved — needs a direct check of Play Console's vitals page once versionCode 12 is live on a track |
| — | Also bumped AGP `9.3.0 → 9.3.1` in the top-level `build.gradle.kts` (was already sitting as an uncommitted change before this work started) |
| — | Shipped as versionCode 12 / 1.0.11. Verified: `assembleDebug`, `lint`, `bundleRelease` all `BUILD SUCCESSFUL` (lint: no new errors introduced); `jarsigner -verify` on the signed AAB → jar verified, same keystore (cert valid to 2053-11-16); `assembleRelease` produced a signed installable APK. On-device sanity install performed directly by the user (not automatable in the agent's own environment — no attached device or emulator there): confirmed "Version 1.0.11" in Settings, clean Weather/Chat/Settings navigation, location permission flow, theme/unit toggles, edge-to-edge rendering, and chat streaming, all with no crashes |
| — | New Play Console upload warning, found while uploading versionCode 12 to Internal Testing: "This App Bundle contains native code, and you've not uploaded debug symbols." Non-blocking (a "Warning," not an "Error" — Console still allows the release to proceed), but investigated before dismissing it rather than clicking past it blind. Confirmed via `unzip -l` on the actual AAB: this release bundles `libandroidx.graphics.path.so` for all four ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) — a small prebuilt native library pulled in transitively by the newer Compose UI from this release's `compose-bom` bump, not present in versionCode 11. The app still has zero native code of its own (no `externalNativeBuild`/CMake setup) — the existing `ndk { debugSymbolLevel = "FULL" }` release-build setting (added back in B5) only bundles debug symbols for a project's *own* NDK-built code, so it has no effect on a third-party AAR's already-stripped prebuilt `.so`. Not fixable from this project's side without `androidx.graphics.path` publishing a symbols artifact upstream — same category as V4 (`glance-appwidget`). Safe to proceed past |
| B24 | Bug found during that sanity pass — confirmed pre-existing, not a v12 regression, since `WeatherWidget.kt` wasn't touched by any of the above changes: the home-screen widget shows significant blank space once resized larger than its largest declared breakpoint, and even at some default launcher placements. Root cause: `sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, WIDE, LARGE))` caps at LARGE (250×110dp) — in Glance's Responsive mode, `LocalSize.current` snaps to the nearest declared breakpoint rather than the widget's true on-screen size, so content is measured against that breakpoint's nominal dimensions while the actual `AppWidgetHostView` (often genuinely larger, depending on the launcher's grid) stretches the background to fill anyway — producing a correctly-styled card with content clustered in one corner and the rest left blank. Not fixed here; scoped into the next release (see below) |

### Completed — versionCode 12 Production Rollout & Vitals Root-Cause Investigation (2026-08-04)

versionCode 12 was promoted Internal Testing → Closed Testing → Production (staged rollout starting at 20%, all via "Promote release" — same artifact throughout, no re-uploads, no versionCode conflicts). At 20% rollout (4 installs), Android Vitals showed user-perceived crashes and ANRs flat at 0 for the full window, with crash/ANR *rate* reporting "Data unavailable" (Play Console needs more session volume than 4 installs to compute a rate) and no entries under "Issues affecting the most users." Clean but statistically thin. Given the low functional risk of this release (dependency-version bump only, no behavior changes, already sanity-tested on-device before shipping), rollout was increased to 100%.

User then flagged that the live release's own "Monitor and improve" panel still showed **4 of the original 5 Android Vitals actions** against release 12 — not fully cleared by the versionCode 12 dependency bump as hoped. Investigated for real this time rather than continuing to guess or assume Console lag:

| # | Finding |
|---|---|
| V1, real root cause | Ran `gradle dependencyInsight --dependency androidx.fragment:fragment --configuration releaseRuntimeClasspath` against the actual project. Confirmed `androidx.fragment:fragment:1.1.0` is pulled in by **`com.google.android.gms:play-services-base`/`play-services-basement:18.9.0`**, both transitive dependencies of `play-services-location:21.4.0` — Google's own GMS base libraries pin that fragment version internally, independent of which `play-services-location` release this project uses. The versionCode 12 bump (21.3.0 → 21.4.0) genuinely could never have cleared this finding; the original activity-compose/glance-appwidget guess from the first investigation was wrong. Per the original V1 entry's own pre-approved fallback ("a pin would just be papering over the real transitive source" *unless the bump alone doesn't resolve it* — now confirmed it doesn't), the real fix is a direct `implementation("androidx.fragment:fragment:1.8.9")` declaration to force Gradle's conflict resolution to the higher version app-wide. 1.8.9 (Aug 2025) confirmed as the current stable release without the `compileSdk 37` requirement that ruled out the latest `core-ktx`/`lifecycle` in versionCode 12. **Fixed in versionCode 13 — see below** |
| V2/V3 | Still showing on the live release. Investigated further (see below) — found strong cross-framework corroborating evidence rather than a specific traceable dependency, so treated as the same "wait on upstream" category as V4 rather than fixed |
| V4 | Dropped off the visible "actions recommended" list (5 → 4) at 20% rollout. **Not confirmed as fixed** — `androidx.glance:glance-appwidget` wasn't touched by versionCode 12 at all, so this is more likely just not yet re-detected at low install volume than an actual resolution. Don't treat as closed |
| V5 | Still showing. This answers the open question from versionCode 12's own build (the `optimizeReleaseResources` task appearing under AGP 9.3.1) — that was **not** evidence of an automatic fix as hoped; the actual `optimization { }` DSL change was still needed. **Fixed in versionCode 13 — see below** |

Plan carried out as stated: both fixes folded into **v13 alongside the widget redesign**, rather than a dependency-hygiene-only release in between.

Also surfaced in the same conversation and deliberately deferred rather than fixed now: the widget has no alert indicator at all (a real functional gap, since NWS alerts are a headline in-app feature but invisible on the widget — flagged directly by user feedback that people check the widget specifically for weather *and* alerts without opening the app), uses raw system emoji instead of the app's own `WeatherGlyph` icon set (inconsistent across OEM emoji fonts, off-brand), and has a flat single-color Material-You background with no condition-based visual identity, unlike every other screen in the app. All shipped in versionCode 13 — see below.

### Completed — versionCode 13: Vitals Fixes + Widget Redesign (2026-08-04)

The largest single release in the project's history by file count touched (9 modified, 2 new). Verified at each stage with the project's usual bar (`assembleDebug`/`lint`/`bundleRelease`/`assembleRelease`/`jarsigner -verify`, plus the full 26-test unit suite once `HourEntry` — a widely-shared model — changed shape), and additionally cross-checked against a real device/emulator this time (see the on-device findings below), not compile success alone.

**Vitals fixes, both confirmed via direct experimentation rather than trusting AI-summarized docs (two separate fetches of the same AGP page had disagreed with each other on `enable` vs `enabled`):**

| # | Fix | Verification |
|---|---|---|
| V1 | `implementation("androidx.fragment:fragment:1.8.9")` added directly to `app/build.gradle.kts`, per the root cause found investigating versionCode 12 | `gradle dependencyInsight` re-run: confirms Gradle now resolves the whole graph to 1.8.9 via conflict resolution, overriding the GMS-forced 1.1.0 |
| V5 | `optimization { enable = true }` added to the release build type, alongside (not replacing) the existing `isMinifyEnabled`/`isShrinkResources`/`proguardFiles` lines | Tested directly against the real installed AGP 9.3.1 before touching the committed build: `gradle help` (which fully configures the project, so an invalid DSL member fails immediately) confirmed `enable` is correct, then a full `clean bundleRelease` confirmed the new block coexists with the legacy lines without conflict, despite AGP's own docs implying it "replaces" them |
| V2/V3 | Investigated (not fixed) — found multiple *unrelated* frameworks (Flutter, .NET MAUI, React Native Screens) reporting the identical Play Console warning with the same deprecated-API list, none traceable to their own app code either. Concluded this is a platform/library-internal Android 15+ finding (plausibly `IconCompat`'s internal icon-building path, used pervasively including by Glance), not something any dependency bump in this app can address. Same "wait on upstream" bucket as V4 |

**Widget redesign**, prompted by direct user testing (screenshots at each round, not guesswork):

| # | Change | Notes |
|---|---|---|
| — | **Alert indicator** | Severity-colored dot + event name on MEDIUM/TALL/WIDE/LARGE/XLARGE, reusing `WeatherComponents.kt`'s `alertColors()` accent values (`widgetAlertColor()`, a plain non-Composable copy since Glance has no MaterialTheme). On WIDE specifically, an active alert replaces the hourly line rather than sharing it — not enough room for both, and an alert is more urgent |
| B24 | **Blank-space fix, two rounds** | Round 1 (breakpoints + `Box` centering): added a `TALL` (110×220dp) breakpoint for narrow-but-tall placements that previously fell back to MEDIUM and wasted the extra height, plus wrapped the root composable in a centered `Box` so leftover space read as balanced padding. User feedback after testing: "still a lot of underutilized space." Round 2 (the actual fix): checked Glance's real `Row`/`Column` source — it has no `Arrangement` (no `SpaceBetween`/`SpaceEvenly` like Compose UI), so centering a fixed-size block was never going to be enough. Switched every tier to `fillMaxSize()` content with `Spacer(GlanceModifier.defaultWeight())` at the right points, so real extra space actively distributes instead of sitting as a static border |
| — | **Condition-aware gradient background** | Extracted `conditionGradient()`'s sky-color selection out of `WeatherComponents.kt` into a new plain function, `util/ConditionColors.kt`'s `skyColor(code, isDay, isDark)` — one shared source of truth for both the in-app hero and the widget, instead of a second copy that could drift. `conditionGradient()` itself is now a 2-line wrapper. Widget renders a small gradient bitmap per update (`WidgetRendering.kt`'s `renderGradientBitmap`, via `android.graphics.LinearGradient`) fading from the condition's sky tone into the widget's existing Material You accent color, rather than replacing that personalization outright |
| — | **Real icons instead of emoji** | `WeatherGlyph.kt`'s `drawWeather`/`glyphFor`/`Glyph` made `internal` so the widget can call them directly — same shapes as the rest of the app. `renderGlyphBitmap()` renders them to a `Bitmap` via `CanvasDrawScope` + the `Canvas(c: android.graphics.Canvas): Canvas` factory (verified against the real `androidx.compose.ui.graphics` source rather than guessed, after an initial wrong guess at `asComposeCanvas`). Every `weatherEmoji()` call site across all six tiers replaced with a `WidgetGlyph` composable, `remember()`-cached per (code, isDay, size) |
| — | **Widget background transparency, user-controlled** | New Settings → "Widget Background" section (Opaque/Transparent, same `OptionPill` pattern as Theme/Units), persisted via `PreferencesStore.getWidgetTransparent()`/`setWidgetTransparent()` (defaults opaque — no behavior change unless opted in), applied as `TRANSPARENT_ALPHA = 0.55f` to both the gradient and the flat fallback background. `SettingsViewModel.setWidgetTransparent()` calls `WeatherWidget().updateAll()` immediately so the change is visible without waiting for the next scheduled refresh |
| — | **WIDE hourly-row overflow** | Found via a real device screenshot the user sent — the inline hourly summary line ("6 PM ☀️26° 7 PM...") cropped its last entry, same overflow class as an earlier `HourlyStrip` bug but missed in this second spot (fixed spacers, no `fillMaxWidth()`/weighting). Fixed with the identical `fillMaxWidth()` + per-entry `defaultWeight()` pattern |
| — | **`HourEntry.feelsLikeC`** | Added to the shared hourly model (defaults to `tempC` so pre-existing call sites like `Previews.kt` don't need changes), populated in `WeatherRepository` from Open-Meteo's `apparent_temperature` — which was already being fetched into a separate list for chart use, just never threaded into `HourEntry` itself |
| — | **XLARGE detail rows + vivid widget-only icon colors** | User referenced another weather app's widget (AccuWeather) as a design target — adopted the structural idea (full-width rows: time · icon · temp · feels-like · precip%, replacing the column-strip only on XLARGE, which has the height budget for it; LARGE keeps its existing compact strip) using this app's own wording ("Feels like," not that app's trademarked term). For the more saturated icon look, refactored `WeatherGlyph.kt`'s shape functions to take a `GlyphColors` palette parameter instead of hardcoded constants — `MutedGlyphColors` (unchanged) stays the in-app default, `VividGlyphColors` (new) is used only by the widget's bitmap renderer. The in-app icon palette itself is untouched — it's a documented, deliberate "muted so it sits calmly" choice, not something this request was about |

**Not independently verified by the agent** (no device/emulator visual-render access for placed widgets, confirmed via repeated failed `adb shell input draganddrop`/`swipe` attempts at automating the launcher's long-press-drag gesture) — verified instead through real screenshots the user captured and shared at multiple rounds, which is how the B24-round-2 and WIDE-overflow issues were actually found. App-level testing (install, launch, navigate, Settings toggle) *was* done directly via `adb` against the same emulator the user's own screenshots came from, including catching and fixing an unrelated signature mismatch (a stray debug-signed install left over from Android Studio's Run button) before that testing could even start.

### Completed (investigation) — Widget Visual QA, versionCode 13 (2026-08-13)

Requested directly ("do the widget visual QA. I feel there are issues") given how much changed in the v13 widget redesign and that no independent visual pass had happened yet. Launcher drag/drop automation was re-confirmed unreliable in this environment (see above), so a debug-only harness was built instead:

- **`WidgetQaActivity`** (`app/src/debug/java/com/example/weatherly/widget/WidgetQaActivity.kt`, registered only in `app/src/debug/AndroidManifest.xml`, `exported="true"` so `adb shell am start` — running as the `shell` uid — can launch it; never present in release builds) hosts `WeatherWidget` itself via a real `AppWidgetHost`. Same-package `bindAppWidgetIdIfAllowed` alone returned `false` on this Android 16 build (no automatic same-package exemption, contrary to the initial assumption) — fixed with `adb shell appwidget grantbind --package <pkg> --user 0`, a shell command that exists specifically for this kind of test automation. For each size tier it then sets `AppWidgetManager` options (`OPTION_APPWIDGET_MIN/MAX_WIDTH/HEIGHT`) to drive Glance's `SizeMode.Responsive` breakpoint selection, sets the `AppWidgetHostView`'s actual on-screen `LayoutParams`, waits, and draws the view to a `Bitmap` saved as a PNG under `getExternalFilesDir(null)/widget_qa/`. Environment notes for next time: the dev machine's own Android Studio instance running alongside the emulator was enough to starve the emulator guest's CPU (internal load average briefly hit 25–45) and hang `adb install`/`pm install` for minutes — restarting the emulator cleanly (not just retrying) resolved it; and `am start` needs the real installed package name, `io.github.smenon2710.skyspeak` (the `applicationId`), not the `com.example.weatherly` namespace used for class/component names.
- **First run produced nothing but the loading placeholder in every one of 9 captures**, spaced 3s apart. This turned out to be real signal, not a harness bug: `WeatherWidget.loadWeather()` (see the cache-reuse finding below) blocks the entire composition on a fresh network fetch — no cache-first paint — and that fetch was observed taking **25–50 seconds** on this machine. A 3-second per-tier delay was simply too fast to ever catch a finished composition. Fixed the harness to wait a full 40s per tier (each options change restarts the fetch regardless, so there's no way around paying this cost per capture) and re-ran.
- **Confirmed and FIXED — no cache reuse.** `WeatherWidget.loadWeather()` was constructing a **new `WeatherRepository(context)` on every single `provideGlance()` call**, but `WeatherRepository`'s 30-minute cache (`memoryCache`) is a private instance field, not shared/static. So the widget never benefited from that cache: every scheduled update and every `onAppWidgetOptionsChanged` (i.e. every resize) triggered a brand-new full network fetch (forecast + air quality + NWS alerts in parallel) — which is also *why* the QA harness's rapid resizing kept interrupting itself on the first run (each options change restarted a 25–50s fetch before the previous one finished, producing a perpetual loading placeholder that first looked like a hang). **Fixed**: `WeatherWidget` now holds one `@Volatile`/double-checked-locked `WeatherRepository` in a companion object, built with `context.applicationContext`, so repeated calls within the same process actually hit its cache. Verified two ways: `assembleDebug`, `lint`, and the full 26-test unit suite all still pass; and re-running the QA harness showed every tier after the first resolving to real content within its 6s wait (vs. every tier needing the full 40–50s before) — direct confirmation the fix works, not just a compile check.
- **Confirmed real bug, more severe than first scoped — breakpoint downgrade affects nearly every tier, not just the top two.** Re-ran all six breakpoints in isolation (each given its own long settle time, no competing options changes, to rule out stale/in-flight content as a confound) and found: at **MEDIUM, TALL, WIDE, and LARGE's exact nominal sizes**, the widget renders **`SmallWidget`'s layout** (just a centered icon + temperature — no location, no header, no hourly data). At **XLARGE's exact nominal size**, it renders **`LargeWidget`'s layout** instead of its own full-detail `HourlyRow` list. Only SMALL itself (nothing to downgrade to) and the deliberately-oversized-host tiers rendered their intended layout. Two follow-up checks specifically to find the root cause: (1) suspected the harness's own `dp → px` conversion (`.toInt()` truncates, so an "exact" 300dp width becomes a hair under 300dp) — fixed to `ceil()` and re-tested LARGE/XLARGE in isolation again; **the downgrade persisted even with the real frame confirmed slightly *larger* than nominal** (657×289px = 250.3×110.1dp; 788×657px = 300.2×250.3dp), ruling out sub-pixel rounding as the cause. (2) The three oversized-host tiers (real frame 30–230dp bigger than nominal in each dimension) **did** render their correct, intended layout every time. Together this points to a real, non-trivial system-reserved inset (plausibly widget corner-radius or launcher-applied padding) being subtracted from the reported size before Glance's `SizeMode.Responsive` matches it against declared breakpoints — small margins aren't enough to clear it, but large ones are.

- **Fixed and verified — breakpoint downgrade + clipping, in two passes.** Pass 1: added a `MARGIN` constant to `WeatherWidget.kt` that shrinks every declared breakpoint except SMALL (the floor of the set — nothing to downgrade to, no margin needed) below its true target size, giving Responsive matching headroom to clear whatever inset it's fighting. Binary-searched the margin empirically with the QA harness, driving real host frames fixed at each breakpoint's **original** (pre-margin) target dp size — the exact sizes that reproduced the bug: **8dp was not enough** (MEDIUM still downgraded to SMALL at a real 110×110dp host); **16dp fixed selection for every one of the six tiers**.
  - **But at MARGIN=16, four of the five shrunk tiers (MEDIUM/TALL/WIDE/LARGE) then visibly clipped content** — degree symbols and location text cut off, a clipped last hourly row/column. Root cause: Glance has no way to decouple "match this breakpoint against a smaller size" from "lay out this breakpoint's content for a smaller canvas" — shrinking the declared `DpSize` does both at once, and those four composables' content was tuned to just fit their *original*, larger sizes. Only XLARGE (which had slack in its original design) rendered cleanly.
  - **Pass 2 — layout tightening.** Reduced the four affected composables' outer padding (`WidgetContent`'s per-tier dispatch, 12dp/6dp → 8dp/2dp) and tightened internal spacing and font sizes throughout `MediumWidget`/`TallWidget`/`WideWidget`/`LargeWidget` and the shared `MorningFocus`/`DaytimeFocus`/`NightFocus`/`LargeHeader`/`HourlyStrip` composables they call (roughly 2-4sp off secondary text, 1-3dp off spacers, sized for the real MARGIN-dp-smaller canvas). Re-ran the QA harness at every breakpoint's original target size afterward: **all six tiers now render their own correct layout with no clipping**, confirmed visually (screenshots of MEDIUM, TALL, WIDE, LARGE, XLARGE all clean).
  - **Not independently verified**: the MORNING and NIGHT variants of the shared Focus composables (`MorningFocus`/`NightFocus`) — only DAYTIME was checked, since the emulator's system clock couldn't be changed without root (`adb root` → "adbd cannot run as root in production builds" on this AVD image) and the real device time during testing fell in DAYTIME's range. DaytimeFocus was the more cramped of the three originally (largest hero font, 30sp before tightening vs MorningFocus's 26sp and NightFocus's 22sp) and it now renders clean, so there's reasonable but unconfirmed confidence the other two also fit — flagged as a real device follow-up, not assumed resolved.
  - Verified nothing else broke: `assembleDebug`, `lint`, the full 26-test unit suite, and a full signed `bundleRelease` (R8 + resource shrinking + signing — this project's usual release-readiness bar) all pass with the final `MARGIN=16` + tightened layouts.
- **Confirmed real bug — no stale-while-revalidate. Fixed 2026-08-24, see "Completed — Widget Enhancements, Advisory Text & Fixes" below.** `WeatherWidget.loadWeather()` always attempted a fresh network fetch first and only fell back to `ForecastCache` if that fetch failed or returned null — unlike `WeatherViewModel`/`WeatherScreen`, which shows cached data immediately on cold start and keeps it visible during background refreshes. Even though a valid, recent forecast was sitting in `ForecastCache` the entire time (confirmed — the in-app screen was showing it), the widget showed nothing but a spinner for the full fetch duration (25–50s observed) on the very first `provideGlance()` call of a process. The cache-reuse fix above meant this only bit on the *first* call after a process start rather than on every single update, but it was still a real gap: a fresh process (device reboot, app update, low-memory kill) meant the widget showed nothing but a spinner for up to a minute despite good cached data being instantly available. The fix ultimately used `produceState` inside `provideContent` (not a second `provideContent` call as first sketched here) — see below for why and how it was verified.
- Two stale doc-comments found during the same read-through (harmless, not fixed): `WidgetRendering.kt`'s `renderGradientBitmap()` kdoc says the bitmap is applied "via `ImageProvider(bitmap)` with `ContentScale.Crop`", but the actual call site in `WeatherWidget.kt` uses `ContentScale.FillBounds`. And `WeatherWidget.kt`'s `HourlyStrip()` comment says callers slice hours to "5 for LARGE, 7 for XLARGE" — but `XLargeWidget` doesn't call `HourlyStrip` at all anymore (it renders `HourlyRow`s in a plain `Column`, taking 6 hours, not 7); the comment is left over from an earlier revision.
- The harness (`app/src/debug/java/com/example/weatherly/widget/WidgetQaActivity.kt`) is left in the repo in a reusable state — full six-breakpoint + three-oversized-host sweep, `ceil()`-based px conversion, 40s first-tier / 6s subsequent-tier delays (valid now that the cache-reuse fix makes repeat calls fast).

### Completed — Real-Device Widget Bugs + Manual Refresh (2026-08-13)

Found via actual on-device testing (Pixel 9 Pro, real Play Store install signed with the release key) after installing the versionCode 13 build with the fixes above — the kind of bug class that only shows up with real data and a real launcher, not synthetic QA harness data. All three fixed same-day.

- **Fixed — long location names hard-clipped.** Glance's `Text` composable has **no ellipsize/overflow control at all** (confirmed against the `androidx.glance:glance:1.1.1` library source: `maxLines` only calls `TextView.setMaxLines()`, never `setEllipsize()`), so a long `"City, State"` name (`WeatherRepository.reverseGeocode()`'s format) was cut off mid-character rather than truncated with "…". Fixed with `widgetLocationName()`, a small helper that shows only the city (`name.substringBefore(",")`) everywhere the widget displays location — the only reliable fix given Glance can't ellipsize, at the cost of losing the region for same-named cities in different states.
- **Fixed — widget mirrored the app's browsing selection instead of device location.** `WeatherWidget.loadWeather()` previously checked `PreferencesStore.getSelected()` first and only fell back to live GPS if nothing was selected — meaning if the user had ever searched a different city in-app, the widget silently kept showing *that* city instead of where the user actually was. A home-screen widget is a glance surface, not a navigation state, so this was a real, reported point of confusion. Fixed by deleting the `getSelected()` branch entirely — `loadWeather()` now always resolves via `LocationProvider(context).currentLatLon()`.
- **Added — manual refresh, plus a genuine Glance rendering bug found along the way.** The widget already had a 30-minute auto-refresh (`android:updatePeriodMillis`, the OS-enforced floor — can't be sped up from the manifest side), but no way to force an immediate update. Added `RefreshAction : ActionCallback` (forces a real fetch via `sharedRepository(context).getWeather(..., forceRefresh = true)`, then calls `WeatherWidget().update(context, glanceId)` so the result is visible immediately) and a small tappable refresh glyph, shared by MEDIUM/TALL/WIDE/LARGE/XLARGE (SMALL has no room to spare).
  - **First implementation (a custom-drawn bitmap via `Image`+`ImageProvider`, the same pattern every other icon in this widget already uses successfully) rendered as entirely invisible**, no matter where or how it was placed — confirmed via extensive isolation testing: the bitmap itself was proven correct (saved directly to a file and inspected, real non-transparent pixels present at the exact size/alpha/color used in the widget); a plain `Box` with a solid background rendered fine in the same position; even the already-proven `WidgetGlyph` composable (used successfully everywhere else in this file) failed to render when placed in this specific spot; removing `.clickable()`, removing a `defaultWeight()` sibling, reordering it before vs. after its sibling, and routing the bitmap through the same `CanvasDrawScope` wrapper `renderGlyphBitmap` uses (instead of plain `android.graphics.Canvas`) — none of it fixed it. **Root cause not fully identified** — a genuine Glance/RemoteViews `ImageView` measurement or rendering quirk, most likely specific to this Glance version (`1.1.1`) and this particular composition shape, still not pinned down precisely.
  - **Fixed by switching to a `Text`-based glyph ("↻") instead of a bitmap** — `Text` renders reliably everywhere in this exact same spot (the location text right next to it never had a problem), so this sidesteps the bug entirely rather than continuing to chase it. `renderRefreshIconBitmap()` (the abandoned bitmap approach) was removed from `WidgetRendering.kt` rather than left as unused dead code.
  - **Real regression caught and fixed during this same pass**: the first working version used generous padding (`vertical = 6.dp`) around the refresh glyph, which made the header row *taller* than before (Row height = max of its children) and silently reintroduced clipping on the H/L line below it in MEDIUM — the exact class of bug the whole MARGIN/layout-tightening pass above was fixing. Cut to horizontal-only padding once caught; re-verified all six tiers clean afterward.
  - Verified: `assembleDebug`/`lint`/the full unit test suite/a full signed `bundleRelease` all pass; QA harness re-run across all six tiers post-fix shows the refresh glyph present and no clipping anywhere. **Not yet verified**: that tapping the glyph on a real device actually triggers `RefreshAction` and updates the widget (visual presence and layout were confirmed via the harness; the actual tap→network→re-render path needs a real tap, which the harness can't simulate).

### Open — Next Version Candidates (2026-08-20)

Three items raised for the next release, documented here before any fix work starts so priority can be decided with the actual root-cause investigation in hand rather than from the raw reports alone. None of these have been touched in code yet.

**Vitals re-check — V2/V3/V5 still open, contradicting the versionCode 13 fix.** A fresh Play Console "Android vitals overview" screenshot, tagged **Release name: 13 (1.0.12)**, shows exactly 3 actions still recommended: "Edge-to-edge may not display for all users" (V2), "Your app uses deprecated APIs or parameters for edge-to-edge" (V3), and "Improve your app's memory and performance with R8 optimisation" (V5). V1 (`androidx.fragment` stale version) and V4 (bitmap downsampling) are no longer listed, so those two did clear. This is worth noting precisely because versionCode 13 already shipped a *direct*, non-hypothesized attempt at V5 (`optimization { enable = true }`, confirmed via `gradle help` per the versionCode 13 section above) and V2/V3 were separately investigated and concluded to be a platform/library-internal Android 15+ `IconCompat` finding not fixable from this app's side (same section) — so this screenshot is evidence that conclusion was likely right, not new information calling it into question. Two real possibilities, not yet distinguished: (a) Play Console's per-release vitals view is still showing findings scoped to whichever specific release triggered them and simply hasn't re-scanned/cleared V2/V3/V5 against 13 yet (same re-scan-lag pattern seen with the target-SDK warning after versionCode 11), or (b) V5's fix genuinely didn't take effect for some reason not yet identified. Needs a direct Play Console check (expand each finding, check the "Release name" tagging logic and whether it lists 13 as newly-affected or carried over) before concluding either way — not a code change yet.

**B25 — FIXED (2026-08-20) — Chat screen: keyboard opens with a large panning gap, hides prior messages until dismissed.** Reported via direct testing: user asked a walk/jog advice question in Chat, got an answer, then tapped the input field to ask a follow-up. The keyboard opening pushed the entire screen content up, leaving a large empty gap between the input field and the keyboard, and scrolled the previous chat history fully out of view — only visible again after closing the keyboard.

Root cause, confirmed (not just hypothesized) via a real side-by-side on-device repro: `AndroidManifest.xml`'s `<activity>` entry for `MainActivity` declared no `android:windowSoftInputMode` at all, so it fell back to the platform default (`adjustPan`) rather than `adjustResize`. `ChatScreen.kt`'s root `Column` already applies `Modifier.imePadding()` on top of that — under `adjustPan`, the *system* also pans the whole window when the keyboard opens, so `imePadding()`'s own bottom inset stacked on top of the OS-level pan instead of being the only mechanism moving content.

**Verification, not just a compile check.** Built two APKs from the identical codebase differing only in this one manifest line, installed each on a fresh Pixel 9 Pro emulator (API 36), and ran the exact repro (open Chat, ask "is it a good time to walk or jog right now", get a response, tap the input field again) on both:
- **Pre-fix (`git stash` of the manifest change, real repro captured):** tapping the field panned the whole screen so far that the just-sent message and the "Thinking…" bubble rendered partially *underneath the status bar*, with a large dead gap between the input row and the keyboard's top edge, and the entire chat history (including an earlier real answer) was scrolled fully out of view — only reappeared after pressing back to dismiss the keyboard. Screenshots confirm this exactly matches the reported symptom.
- **Post-fix (`android:windowSoftInputMode="adjustResize"` added to `MainActivity`'s manifest entry):** identical repro — header, weather-context strip, sent message, and "Thinking…" bubble all stayed fully visible above the keyboard with no dead gap and nothing hidden behind the status bar. Confirmed at both the empty-chat-state tap and the with-history tap.
- `adb logcat` showed zero crashes/exceptions across either build during the repro.

**Fix:** one line, `app/src/main/AndroidManifest.xml`'s `<activity>` entry for `MainActivity` — added `android:windowSoftInputMode="adjustResize"`.

**Full verification suite, all passing on the fix:** `assembleDebug`, `lint`, `test` (26/26 unit tests, 0 failures), `bundleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` on the signed release AAB → `jar verified` (cert valid to 2053-11-16, same as every prior release). Not independently re-verified: Settings/Weather-screen keyboard flows (city search field) weren't separately repro'd against the old behavior, though `adjustResize` is strictly the standard-recommended mode and no regression was observed in the sanity pass through Weather → Chat during this testing.

**B26 — CLOSED, no change (2026-08-20) — accepted as by-design.** User confirmed comfortable keeping the chrono-dynamic MORNING/DAYTIME/NIGHT behavior as-is, on the condition it's not far off how other weather-app widgets behave — which it isn't; time-of-day-aware widget content (forecast-planning view in the morning, live conditions midday, next-day prep at night) is a common pattern in the category, not a SkySpeak-specific oddity. No code change. Original writeup kept below for the record.

**B26 (original writeup) — Widget: shows "High" temperature instead of current temperature at certain times, read as a bug.** Reported: home-screen widget "sometimes shows temperature, sometimes shows the high/low for the day" — currently observed showing "High 20°" when the user expected the live current temperature. Investigated against `WeatherWidget.kt`: this is very likely the widget's existing, documented chrono-dynamic behavior working exactly as designed, not a defect — `currentTimeOfDay()` (`WeatherWidget.kt`) reads the device's local `Calendar.HOUR_OF_DAY` and switches focus content on the MEDIUM/TALL/WIDE/LARGE/XLARGE breakpoints: **5–10 AM (MORNING)** renders `MorningFocus`, whose hero line is literally `"High ${data.highTodayC}°"` (`WeatherWidget.kt:502`) — today's forecast high, not the live current reading — plus a rain-chance line; **11 AM–5 PM (DAYTIME)** renders `DaytimeFocus`, whose hero is `"${data.currentTempC}°"` (live current temp); **6 PM–4 AM (NIGHT)** renders yet a third variant, tomorrow's H/L. Only the SMALL breakpoint (2×1 cell) is time-invariant and always shows current temp. So if the widget was checked during the device's local 5–10 AM window, "High 20°" is the intended MORNING-tier content, correctly labeled with the word "High" in the widget itself — not a mislabeled or wrong value. This is a real product-decision item regardless: either (a) confirm the observation happened inside the 5–10 AM window and the behavior is working as designed, in which case the open question is whether users actually want this trade (a fixed always-current-temp widget vs. the current forecast-planning MORNING view) — worth deciding rather than assuming, since this was designed but not user-tested before shipping in versionCode 13 — or (b) if the observation did *not* fall in that window, there's an actual bug in `currentTimeOfDay()`'s local-time handling to chase (e.g. a timezone/locale edge case) that hasn't been found by inspection alone. Needs the exact device local time at the moment of observation to tell which branch this is, then a real on-device check at multiple times of day before deciding a fix.

**Priority ordering:** B25 was picked first (a real, reproduced UX defect) and is now fixed and verified above. B26 is closed (see above). Vitals re-check remains open.

**B27 — FIXED (2026-08-20) — XLARGE widget: 3-day outlook added to fill empty space on large placements.** User feedback, separate from B26: the widget "had a lot of empty space, which makes it look incomplete." Root cause (confirmed by code inspection, matching the documented B24 history): `weather_widget_info.xml` sets no `android:maxResizeWidth`/`maxResizeHeight`, so a generous launcher lets the widget be resized well past its largest declared breakpoint (XLARGE, 300×250dp nominal). Past that point Glance's Responsive mode locks to XLARGE's fixed content (correct, by the earlier B24 fix), and every tier above SMALL (`MEDIUM`/`TALL`/`LARGE`/`XLARGE`) uses `fillMaxSize()` + `Spacer(defaultWeight())` around a fixed amount of content — so any extra real estate becomes blank gaps, not more content.

User chose "add richer content for large sizes" over capping the max resize size or scaling up existing elements. Scoped to **XLARGE only** — it's the tier with documented layout slack ("XLARGE had slack in its original design," per the MARGIN comment in `WeatherWidget.kt`); `MEDIUM`/`TALL`/`LARGE` are already tight-fit at their nominal declared size (per the two-round clipping fix in the versionCode 13 section above), so adding content there risks reintroducing that clipping bug. Not touched in this pass.

**Fix:** `WeatherWidget.kt` — added `DailyOutlookRow` (day label, day-variant glyph, low/high temp — mirrors the in-app `DailyCard`'s convention of always using the day glyph, since `DayEntry` has no `isDay` field) and wired `data.daily.drop(1).take(3)` (skip today, next 3 days) into `XLargeWidget` below the existing 6-row hourly list.

**Verification, via the existing debug-only `WidgetQaActivity` harness** (real `AppWidgetHost`, all 9 tiers including XLARGE at exact nominal size and an oversized real host frame — see the versionCode 13 QA section above for how this harness works):
- **True before/after, same real data.** Built pre-fix (`git stash` of the `WeatherWidget.kt` change) and post-fix APKs, installed each via `adb install -r` (update, not uninstall, so `ForecastCache` — real London data — carried over identically between runs), and ran the QA harness on both. At **XLARGE oversized real host (400×340dp, tier `7_xlarge_oversized_host`)**: pre-fix showed the hourly list ending at 10 PM with a large blank gap down to the "Updated 8m ago" footer; post-fix shows the identical hourly list followed immediately by a Fri/Sat/Sun outlook row (icon + low/high) filling that same space, footer pushed down accordingly. Screenshots sent to the user directly. (An earlier attempt at this same comparison used a fresh install with an empty `ForecastCache`, which showed only the "Open SkySpeak to set up" placeholder on both sides — an invalid comparison, corrected via this reinstall-not-wipe approach.)
- **Separate spot-check, live data with an active NWS alert (Hillsborough Township, NJ — Flood Watch), post-fix build only:** at **XLARGE exact nominal size (300×250dp, tier `6_xlarge`)**, the existing 6-hour list already fills the available height with zero slack when an alert banner is showing (pre-existing behavior, unrelated to this change) — the new outlook rows simply don't render in that tight case, a no-op rather than a regression; confirmed no character-level clipping was introduced (the 6th hourly row rendered in full). `MEDIUM`/`TALL`/`LARGE` screenshots re-checked with the same alert data and confirmed unaffected (unchanged code paths) — `TALL`'s own nominal-size blank space (a separate, un-fixed instance of the same underlying pattern) is visibly still present, noted here in case the user's actual widget turns out to be TALL-sized rather than XLARGE.
- Full verification suite, run after restoring the fix: `assembleDebug`, `lint`, `test` (26/26 passing), `bundleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` → `jar verified`.

**Not yet known:** which breakpoint the user's actual home-screen widget placement resolves to — this fix targets XLARGE specifically since it's both the tier most likely to be resized generously and the one with layout headroom to enrich safely; if their widget is actually TALL/MEDIUM/LARGE-sized, the same technique would need its own (more careful, given the tighter margins) pass.

**Live-device follow-up (2026-08-20):** user placed the actual widget on a real emulator home screen (not the QA harness) and manually dragged it through a range of sizes, sharing 6 real screenshots while resizing. Confirmed the XLARGE fix above works live — one screenshot shows the 3-day outlook rendering correctly with real data (Franklin Park/Hillsborough Township, NJ, active Flood Watch) as the widget was resized larger. But two more screenshots surfaced something new:

### B28 — FIXED (2026-08-20) — LARGE widget clipped/blanked at certain real sizes between its floor and comfortable size

**Symptom:** during manual resize, one of the user's screenshots showed the hourly row's icons rendering but the temperature values directly underneath missing entirely (not just visually empty — genuinely absent). Investigating via the QA harness (three binary-search probe tiers between LARGE's confirmed-too-tight floor, 234×94dp — downgrades cleanly to SMALL — and its "known good" size, 250×110dp, tier 5 — probed at real hosts 240×98dp, 244×102dp, 248×106dp) reproduced something worse than the user's screenshot: **the entire widget rendered as a blank white rounded rectangle, no content at all**, at all three probe sizes.

**First hypothesis, investigated and ruled out:** `adb logcat` showed a real error alongside every widget update —
```
E GlanceAppWidget: Truncated Column container from 11 to 10 elements
E GlanceAppWidget: java.lang.IllegalArgumentException: Column container cannot have more than 10 elements
    at androidx.glance.appwidget.LayoutSelectionKt.insertContainerView-nVsUan0(LayoutSelection.kt:384)
```
— `androidx.glance.appwidget` 1.1.1 does enforce a real, hard 10-child cap on any single `Column`/`Row`/`Box` when translating to `RemoteViews` (confirmed by pulling the actual library sources — `glance-appwidget-1.1.1-sources.jar` — and reading `LayoutSelection.kt` directly rather than guessing from the stack trace alone). Initial theory: `SizeMode.Responsive`'s multi-breakpoint machinery (`SizeBox.kt`'s `ForEachSize`, which composes the widget once per declared breakpoint) builds one shared container holding all 6 size variants, overflowing the cap. **Read the actual translation code (`RemoteViewsTranslator.kt`'s `translateComposition`) to check this directly — it's wrong.** Each `EmittableSizeBox` is translated into its own **fully independent** `RemoteViews` object (fresh `TranslationContext`, `lastViewId` reset to 0) and combined via the native Android 12+ `RemoteViews(Map<SizeF, RemoteViews>)` OS constructor — real platform-level size-switching, not a Glance-authored shared Column. So the 10-child cap, while real, isn't caused by having 6 declared breakpoints, and — confirmed by testing in isolation with the user's other placed widgets removed from the home screen so logcat wasn't picking up their independent update cycles — **this same error fires on every ordinary widget update regardless of size**, including the "known good" 250×110dp tier that was rendering correctly. It's a genuine, separate, still-unlocated pre-existing bug (not pinned to a specific composable yet), but it is **not** what was causing the blank/clipped rendering — ruled out once it was confirmed present even when the visible output was fine.

**Actual root cause, confirmed by direct measurement:** `WidgetContent` gives `LargeWidget` `GlanceModifier.padding(8.dp)` on all sides. At a real host of 250×110dp (previously assumed "comfortable"), that leaves only 250-16=234 × 110-16=**94dp** of usable height — the exact same height as LARGE's declared post-MARGIN floor. There was never any real slack. `LargeWidget`'s content — `AlertIndicator` + `LargeHeader` + `HourlyStrip`, each entry of which stacked hour-label/icon/temp/precip as **3-4 separate lines** — didn't fit that 94dp budget once an active alert banner (the live NWS Flood Watch used throughout this testing) took its own line of height, so `HourlyStrip`'s temperature line silently clipped off the bottom. At the even-tighter probe sizes, the overflow was large enough that (for reasons not fully pinned down — possibly interacting with the still-open Column-cap bug above) the render produced nothing at all instead of a partial clip.

**Fix:** `HourlyStrip` in `WeatherWidget.kt` — combined each hour's icon and temperature onto one `Row` instead of two separate stacked lines, cutting the per-entry height need from ~3-4 text lines to ~2. No change to `LargeWidget`'s own structure or padding.

**Verification, via the QA harness, isolated from the user's other home-screen widgets** (uninstalled/reinstalled fresh so `adb logcat` cleanly attributes every event to this one test instance) **against the same live alert-carrying data throughout (Hillsborough Township, NJ — Flood Watch)**:
- **250×110dp ("known good"):** before the fix, temperatures were missing under each hour's icon (confirmed via screenshot, matching the user's original report exactly). After the fix, all five temperatures render correctly alongside their icons.
- **240×98dp, 244×102dp, 248×106dp, 234×94dp (all four previously-blank/broken probe sizes):** after the fix, all four now render a **clean downgrade to SMALL's icon+temp layout** — Responsive correctly recognizing there still isn't room for LARGE's content and falling back gracefully, rather than attempting a doomed LARGE render. This is the intended, safe behavior (same as the original pre-MARGIN-fix downgrade path), not a new problem.
- Confirmed via a proper before/after isolation (same method as B25/B27): reproduced the blank result on the **original, pre-fix `HourlyStrip`** at these same sizes before applying the fix, then reproduced the clean result after — ruling out coincidence.
- Full 17-tier regression sweep (all original tiers + the tight/oversized-host tiers from B27's investigation) re-run after the fix: XLARGE's daily outlook (B27) still renders correctly at its oversized-host tier, no other tier regressed.
- Full verification suite: `assembleDebug`, `lint`, `test` (26/26 passing), `bundleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` → `jar verified`.

**QA harness updated permanently**: `WidgetQaActivity.kt` now includes 8 additional tiers (`10_medium_tight` through `17_large_probe_c`) probing the true post-MARGIN floor for every breakpoint plus this specific LARGE binary-search window, so this real-size gap in coverage doesn't silently reopen — the original tiers only ever tested generous or exact-nominal sizes, never this narrow real window where the bug actually lived. Per-tier wait times bumped slightly (`60000`/`8000` vs. the original `40000`/`6000`) after observing slower network conditions during this session.

### B29 — FIXED (2026-08-20) — `Column container cannot have more than 10 elements`, the error left open by B28

B28's writeup above flagged this error as real, firing on every widget update, but with its source composable unidentified and no confirmed visible impact — left open rather than guessed at. Investigated properly as its own fix.

**Root cause, found by hand-counting each composable's actual emitted children (not guessed):** `MorningFocus`/`DaytimeFocus`/`NightFocus` (the chrono-dynamic content shared by `MediumWidget` and `TallWidget`) each emit several `Text`/`Row`/`Spacer` elements directly with **no wrapping container of their own** — e.g. `DaytimeFocus` alone emits 5 elements (icon+temp `Row`, a `Spacer`, condition `Text`, another `Spacer`, H/L `Text`). `MediumWidget` and `TallWidget` both called these inline — `when (time) { DAYTIME -> DaytimeFocus(data, c) ... }` directly inside their own root `Column` — so every element `DaytimeFocus` emits becomes a **direct sibling** of that root Column's other children (header row, alert indicator, spacers, upcoming-hours block, staleness label) instead of counting as the one child a naive read of the call site suggests.

Hand-counting `TallWidget`'s root `Column` with DAYTIME + an active alert + upcoming hours present (i.e. the exact conditions used throughout this session's live testing) gives: header row (1) + alert spacer (1) + alert indicator (1) + spacer (1) + DaytimeFocus's 5 flattened elements (5) + spacer (1) + upcoming-hours column (1) + spacer (1) + staleness label (1) = **13** — exactly matching one of the two truncation counts logged together in every prior repro (`11` and `13`, or `11` and `12`, depending on which optional elements were active in a given run). `MediumWidget` under the same conditions comes to **11** — the other logged count. Both numbers are the *same* underlying bug hitting two different composables that both get composed on every single widget update (`SizeMode.Responsive`'s `ForEachSize` composes all six declared breakpoints every time, regardless of which one is actually visible — confirmed by reading the actual `androidx.glance.appwidget` 1.1.1 sources, `SizeBox.kt`, rather than assumed), which is why the error fired unconditionally rather than only at specific real sizes.

`LargeHeader` (used by `LargeWidget`/`XLargeWidget`) already avoided this — its own `when (time) { ... }` dispatch is wrapped in `Column(modifier = GlanceModifier.defaultWeight()) { ... }`, so its Focus composable's elements group into one child there. `MediumWidget` and `TallWidget` just hadn't followed that same pattern.

**Fix:** wrapped both call sites in their own `Column { when (time) { ... } }`, mirroring `LargeHeader`'s existing correct pattern exactly. No change to `MorningFocus`/`DaytimeFocus`/`NightFocus`/`LargeHeader` themselves.

**Verification:** re-ran the full 17-tier QA harness sweep (all original tiers plus B27/B28's tight/probe tiers) against the same live alert-carrying data (Hillsborough Township, NJ — now showing 2 simultaneous alerts, "Flood Watch +1") with a clean `adb logcat` capture across the whole run — **zero `Column container` errors**, down from firing on every prior run. Visually re-checked `TallWidget` and `MediumWidget` at their nominal sizes: all content (location, alert, hero temp/condition, H/L, upcoming hours) renders fully, nothing newly clipped or missing. `LargeWidget`'s B28 fix and `XLargeWidget`'s B27 fix both still render correctly, confirming no regression from touching the shared `MorningFocus`/`DaytimeFocus`/`NightFocus` call pattern. Full verification suite: `assembleDebug`, `lint`, `test` (26/26 passing), `bundleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` → `jar verified`.

**Not fixed here, noted for the record:** `MediumWidget`'s header row shows a very minor (1-2px) top-edge clip on the location text at its exact nominal size — a separate, pre-existing, cosmetic issue unrelated to the element-count bug, spotted incidentally during this verification pass. Not chased further; flag if it turns out to matter in practice.

### B30 — FIXED (2026-08-21) — real-device feedback: remove daily outlook, 6 entries on LARGE, temperature colors, staleness-label clipping

First real-device round after B27–B29 shipped to the working tree — user tested the actual widget on their own Pixel 9 Pro (connected via `adb`, installed the signed release APK, not just the emulator) and reported four things:

1. **"The next 3 days is not required"** — B27's XLARGE daily outlook, removed. `DailyOutlookRow` and its call site deleted from `XLargeWidget`; the now-unused `DayEntry` import removed. This also incidentally fixes part of item 4 below, since the outlook was one of the things competing for XLARGE's limited real height.
2. **"The horizontal widget can show 6 entries"** — `LargeWidget`'s `HourlyStrip` call bumped from `data.hourly.take(5)` to `take(6)`; already used `defaultWeight()` per column so 6 entries divide the row evenly with no code changes needed beyond the count.
3. **"Add colors wherever possible... attractive to put on the home screen"** — added `widgetTempColor(tempC, metric, isDark)`, a new theme-aware 6-bucket cool-to-warm gradient (same concept as the in-app `DailyCard`'s `tempColor()`, but with its own light/dark color pairs per bucket rather than reusing that function's values directly — `tempColor()` fills a Canvas bar, where any lightness reads fine; here the same hues sit as *text* directly on the widget's own background, so each bucket needs its own contrast-safe light-mode and dark-mode variant, mirroring the pattern `widgetAlertColor()` already used). Applied to every hero and list temperature across all six tiers: `SmallWidget`, `MorningFocus`/`DaytimeFocus`/`NightFocus` (shared by `MediumWidget`/`TallWidget`/`LargeHeader`), `HourlyStrip`, `HourlyRow`, and `WideWidget`'s upcoming-hours row (its single-line hero text combines temp and location in one string — left uncolored rather than awkwardly tinting the location name too).
4. **"The last updated text... looks like you removed it"** — this turned out to be a real, reproduced clipping bug, not a coincidence of the outlook removal. Investigated using the QA harness: at `TallWidget`'s own *exact nominal* size (110×220dp, not even a tight/probe size), the 4th upcoming-hour row was cut off and the staleness label was entirely absent, one line of `MorningFocus`'s rain-chance text ("💧 X% chance of rain") was wrapping to 2 lines on the narrow real width, eating a full extra line of height it didn't need to. Fixed in three steps, each verified via the QA harness before moving to the next: (a) shortened the rain text to "💧 X% rain", matching the wording `LargeHeader`'s own separate copy of this line already used — a real pre-existing inconsistency; (b) reduced `TallWidget`'s upcoming-hours list from 4 to 3 entries; (c) `MediumWidget` had its own smaller-scale version of the same problem (location text clipped at the top, hero temp clipped at the bottom, even with no upcoming-hours block to trim) — fixed by shaving a few dp of spacing and stepping `MorningFocus`'s hero font down from 22sp to 20sp.

**Verification note on the staleness label specifically:** `stalenessLabel()` returns `null` whenever `cachedAt` is null, which is *every* successful live fetch (`WeatherWidget.loadWeather()` only sets a non-null `cachedAt` when falling back to `ForecastCache`) — so the QA harness, which always completes a fresh live fetch within its wait window, can never render the label under normal conditions, making the layout fix impossible to visually confirm through the harness alone. Verified anyway by temporarily forcing `stalenessLabel()` to return a placeholder string regardless of `cachedAt` (a debug-only one-line change), re-running the harness to confirm the label now renders in full at both `TallWidget`'s and `MediumWidget`'s nominal sizes with visible margin to spare, then reverting the temporary change before finalizing — the real `if (cachedAt == null) return null` logic is unchanged.

**Full verification, this round:** full 17-tier QA harness sweep re-run after each incremental change (rain-text fix, then 3-hour reduction, then MEDIUM spacing) — zero `Column container` errors throughout (B29 holding), no regression to B27 (outlook correctly absent) or B28 (LARGE's hourly temps still fully visible, now 6 of them). `assembleDebug`, `lint`, `test` (26/26 passing), `bundleRelease`, `assembleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` on the AAB → `jar verified`; `apksigner verify` on the APK → verifies (exit 0). Installed the actual signed release APK directly on the user's real Pixel 9 Pro via `adb` (not just the emulator) — launched cleanly, zero crashes in logcat.

### B31 — FIXED (2026-08-21) — consistent large hourly text size across all tiers; color-consistency confirmed already correct

Second round of real-device feedback, same session as B30.

1. **"Keep the text sizing of time and temperature consistent across all widget sizes... the size that can be seen when the widget is max size"** — before this, each tier's hourly list (`HourlyStrip` on LARGE, `HourlyRow` on XLARGE, the upcoming-hours rows on WIDE and TALL) used its own ad hoc font size (9sp/10sp/11sp/13sp depending on which tier happened to render it, sized down individually to fit each tier's own cramped real estate). Unified all of them to XLARGE's values — the roomiest tier, so the only one that was never fighting for space — via two new shared constants, `HOURLY_TIME_SP = 11.sp` and `HOURLY_TEMP_SP = 13.sp`, applied at every call site instead of inlined per-composable sizes.

   This directly reopened space pressure in the smaller tiers, exactly the class of bug B28/B30 already fixed once — bigger text needs more room per entry. Re-verified via the QA harness against the same live data and adjusted entry counts and hero sizes (not the hourly text, which was the explicit ask) where needed rather than shrinking the unified size back down:
   - `WideWidget`'s upcoming-hours row: reduced from 4 to 3 entries (WIDE's 250dp row didn't have width for 4 columns at the larger font).
   - `LargeWidget`, the hardest case: reproduced a real overflow under MORNING focus + an active alert + the new larger 6-entry `HourlyStrip` (temperatures clipped off the bottom, matching B28's original symptom exactly) at LARGE's own *exact nominal* size. Fixed by stepping `LargeHeader`'s hero font sizes down (MORNING 17sp→15sp, DAYTIME 20sp→18sp — hero temps, not part of the "keep hourly text large" request, so these were the right thing to shrink instead) and `AlertIndicator`'s label font down a point (10sp→9sp, shared across every tier that shows both an alert and hourly content). The specific live NWS alert that reproduced the overflow had cleared by the time the `AlertIndicator` trim was ready to re-test against it directly (alerts are real/live, not something that can be forced in the harness) — kept as a deliberate safety margin given the one case that broke wasn't independently re-confirmed fixed, rather than assuming the LargeHeader change alone was enough.
   - `TallWidget`'s upcoming-hours row (already at 3 entries from B30) and `HourlyRow` (XLARGE, already at the target size) needed no further changes — re-verified clean at the larger unified size regardless.

2. **"Color of widget should be consistent for celsius and fahrenheit based on the temperature ranges defined"** — audited every one of `widgetTempColor()`'s 12 call sites; all correctly derive `metric` from the same `data.windUnit == "km/h"` check already established elsewhere in this codebase, and the function itself already converts to true Celsius (`(tempC - 32) * 5.0 / 9.0`) before bucketing when not metric — so the same real-world temperature already gets the same color regardless of which unit system is displayed. No code change was needed here; confirmed correct by inspection rather than assumed.

**Verification:** full 17-tier QA harness sweep after the final round of changes — zero `Column container` errors, all tiers visually re-checked clean (LARGE's 6 entries with an active alert scenario specifically re-verified after the hero/alert font trims). `assembleDebug`, `lint`, `test` (26/26 passing), `bundleRelease`, `assembleRelease` all `BUILD SUCCESSFUL`; `jarsigner -verify` → `jar verified`; `apksigner verify` → verifies (exit 0). Installed the signed release APK on the user's real Pixel 9 Pro — launched cleanly, no crashes (one benign `WM-WorkerWrapper` task-cancellation log line from a stale widget update job restarting, not an error).

### Completed — Widget Stale-While-Revalidate, D7/D8, and B32 (2026-08-24)

Four independent changes verified in one session, on an emulator (Pixel 9 Pro AVD) rather than the usual real device (no physical device available this session) — live Open-Meteo data throughout, cross-checked against a direct `curl` of the same API for the new fields, plus a real-device-style cold-process test using `am force-stop` + the `WidgetQaActivity` harness.

**1. Widget stale-while-revalidate — fixed, closing the gap flagged in "Widget Visual QA, versionCode 13" above.** `WeatherWidget.loadWeather()` was replaced: `provideGlance()` now reads `ForecastCache` synchronously up front, then uses `produceState(initialValue = LoadedWeather(cached...))` *inside* `provideContent { }` — the cached forecast renders on the very first composition, and a background fetch (`fetchFreshWeather()`, the renamed fetch-only half of the old function) updates `value` on success, which recomposes with fresh data and pushes a second RemoteViews update. Chose `produceState` over the originally-sketched "call `provideContent` twice" approach — safer, since it relies on ordinary Compose state/recomposition (which Glance's whole composition model already depends on) rather than an unverified assumption about calling `provideContent` more than once per `provideGlance`.
- **Verified via a true cold-process repro:** `adb shell am force-stop` (kills the process, resetting `sharedRepository()`'s in-memory cache to null — the exact condition this bug needed) → `am start` the QA harness → screenshotted at ~0.3s intervals. Cached data ("76°") was visible within ~1.5s, versus the 25–50s a real fetch takes — confirms the cached render isn't waiting on the network at all. Zero crashes/exceptions in logcat across the whole test.

**2. D8 (top two items) — wind gust forecast + dew point, implemented.** `wind_gusts_10m` (hourly) + `wind_gusts_10m_max` (daily) + `dew_point_2m` (current/hourly) added to `OpenMeteoApi.kt`'s requests; threaded through `OpenMeteoModels.kt` → `WeatherRepository.kt` → new `WeatherData.hourlyWindGust`/`dewPointC` and `DayEntry.windGustMaxKmh` fields (all defaulted for `ForecastCache` backward-compat, same pattern as `alerts`). Wind detail sheet (`WindDetailContent`) gained a third **"PEAK TODAY"** column next to SPEED/GUSTS. Humidity's detail description gained a dew-point sentence. Chat brief (`ChatRepository.weatherBrief()`) gained a dew-point line.
- **Verified against the live API directly:** `curl`'d the same Open-Meteo forecast endpoint for the test coordinates and confirmed `wind_gusts_10m_max: 24.2` (mph) and `dew_point_2m: 53.2` (°F) were really being returned — then confirmed the app's Wind sheet showed "PEAK TODAY: 24 mph" and the Humidity sheet showed "Dew point is 54°" (rounding difference from a few minutes' gap between the two live fetches, not a bug), so the whole pipeline (API → model → repository → UI) is confirmed correct end to end, not just compiling.

**3. D7 — live advisory text on tiles, implemented and extended beyond the original UV/AQI-only scope to Wind and Humidity too (user's call, since the new gust-forecast and dew-point data made natural advisories possible there as well).** `MetricTileData` gained an `advisory: String?` field. UV → "Low risk"/"Wear SPF"/etc. (short form of the existing `uvAdvice` tiers). Air Quality → "Great for outdoors"/"OK for most people"/etc. (short form of `aqiAdvice`). Wind → "Calm all day"/"Breezy"/"Windy"/"Strong gusts", from today's forecast peak gust (new in D8). Humidity → "Dry air"/"Comfortable"/"Muggy"/"Oppressive", from dew point (new in D8) converted to true Celsius via the existing `toCelsius()` helper before bucketing (same unit-ambiguity pattern as `tempColor()`). `PrimaryStatCell` renders it as an optional 4th line (UV/Humidity/Wind); `ArcGaugeTile` renders it below the arc, not inside it (AQI).
- **Verified on-device — the "uneven card height" risk flagged when this was first logged didn't materialize:** all three `PrimaryStatCell` cells (UV/Humidity/Wind) had real advisory text simultaneously in testing, so all three filled the same 4 lines and stayed visually even. AQI's advisory rendered cleanly below its arc with no overflow.

**4. B32 — real bug found during this session's own testing, not caused by the above three changes; fixed same session.** Using the `WidgetQaActivity` harness on a real evening (genuine NIGHT chrono state, 6 PM–4 AM) surfaced the exact gap `IMPROVEMENTS.md` had flagged as unverified back in B31 ("only DAYTIME was checked — no root access on this AVD to change the system clock"): `NightFocus`'s un-constrained `"H:${high}°  L:${low}°"` `Text` word-wrapped onto two lines at MEDIUM's narrow declared width, and the composable's layout wasn't budgeting height for that second line — so it got silently clipped at the widget's bottom edge, reproduced twice on two separate fresh QA-harness runs at MEDIUM's real target size (110×110dp). Fixed by merging the standalone "Tomorrow" line into the icon+condition row (freeing ~17dp) and splitting the H/L string into two explicit `maxLines = 1` `Text`s instead of relying on wrapping to land safely — each half now also picks up its own true `widgetTempColor` rather than the high's color being applied to the low too.
- **Verified:** re-ran the QA harness after the fix — `2_medium` (the exact size that clipped) now shows both H and L fully; `9_medium_oversized_host` and `3_tall` (unaffected before, still fine after) re-confirmed clean; `5_large` (uses a separate, untouched `LargeHeader` code path) unchanged. Zero crashes/exceptions in logcat. **Not fixed, flagged only:** `LargeHeader`'s own NIGHT branch (LARGE/XLARGE) has the identical un-constrained-wrap pattern — not clipping today only because LARGE/XLARGE have more vertical slack to absorb an extra wrapped line, same latent fragility, left alone since it wasn't asked for and isn't actually broken.

**Overall verification, all four:** `assembleDebug`, `lint`, `test` (26/26 passing) all `BUILD SUCCESSFUL` after each change. No `bundleRelease`/signed-APK/real-device pass this session (emulator-only, per the tools available) — worth a real-device sanity pass before this ships in a release build, consistent with every prior widget release's own verification bar.

### Completed — Real-Device Feedback Fixes, Daily Forecast Tips, and Tides (2026-08-25)

Two bug fixes from real-device feedback on the previous round, plus two new features (daily-forecast tips and a coastal tide-predictions integration), all verified this session on an emulator (mocked GPS/city search — no physical device available mid-session; see the verification note at the end).

**1. Wind detail sheet crowding — fixed.** User-reported on a real device with a larger system Display size setting: SPEED/GUSTS/PEAK TODAY (the three-column row added for D8 last round) crowded and competed for space once Android's font-scale multiplier applied. Root cause: three Bold values in one `Arrangement.SpaceEvenly` `Row` had no slack to absorb the scale-up the way the original two-column SPEED/GUSTS row did. Fixed by pulling PEAK TODAY out of the row entirely into its own single-line caption ("Peak gust today: X") below it — a lone line has no sibling to compete with, so it scales safely regardless of system text size. SPEED/GUSTS returned to their original two-column layout, unchanged from before D8.

**2. Dew point explanation — fixed.** User-reported: the dew-point sentence added to the Humidity detail sheet last round ("Dew point is X°, which is what actually determines how muggy the air feels") gave a number with no sense of whether it was good or bad — "a normal person may not know what dew point means and whether it affects [things] if it is low or high." Fixed by naming the comfort band next to the number (reusing `humidityShortAdvisory`, the same value already computed for the tile's `advisory` line, so the two can never disagree) and adding a plain-language explanation of what dew point actually measures and which direction is worse. New text: *"Dew point is 54° (Comfortable) — a more reliable measure of how muggy the air actually feels than humidity alone. Dew point is the temperature air would need to cool to for dew to form: the higher it is, the more moisture is in the air and the stickier it feels, regardless of the actual temperature; the lower it is, the drier and more comfortable it feels."*

**3. Daily forecast tips — new feature.** User request: make the 7-day forecast more actionable — "plan your trip with umbrella/raincoat if staying outdoors," heat-safety advice ("stay indoors, stay hydrated") for hot days — shown per day, not just for today. Implemented as `DayEntry.tips: List<WeatherTip>`, computed by a new `buildDayOutlookTips()` in `WeatherRepository` for every entry in the 7-day list. Deliberately **not** a shared function with the existing `buildTips()` (the hero `TipBanner` generator) despite overlapping threshold logic (hot/cold/windy/rain/snow) — `buildTips()`'s wording is hardcoded "today"-relative for its own night-rollover-aware use case, and reusing it verbatim for an arbitrary future day (e.g. next Wednesday, tapped from the 7-day list) would read as wrong ("Rain likely today" on a day that isn't today). Also adds a second, more severe heat tier (`veryHot`, ≥100°F/38°C, roughly NWS Excessive-Heat-Warning range) beyond `buildTips()`'s single "hot" threshold — user specifically wanted ordinary warm weather ("carry water") distinguished from genuine heat-safety territory ("stay indoors during peak hours ... check on vulnerable neighbors"). Rendered via the existing `TipBanner` composable (reused, not a new visual treatment) in `DetailSheet.Day`, right after the hero H/L row and before the plain `DetailRow`s — conclusion first, numbers after.

**4. Tides — new feature, real US NOAA data, coastal-gated.** User request: real tide predictions, but "only activate if the user is in or looking for weather in [a] coastal area."
- **Data source:** NOAA CO-OPS (`api.tidesandcurrents.noaa.gov`) — free, public domain, no key, the same "US-government API" precedent NWS alerts already established. Verified live via direct `curl` before writing any app code (confirmed real predictions for Atlantic City, NJ: `wind_gusts_10m_max`-style station data returning actual high/low times and heights).
- **Coastal gate — the real finding of this feature.** Initial design used a loose 40km "nearest station" radius. Testing that against this app's own usual test locations found a real false positive: Franklin Park, NJ (inland, this app's own go-to test point) resolved to a station only 12.4km away ("New Brunswick" — on a tidal river, not the coast). Investigating further found the problem is fundamentally not threshold-tunable: NOAA's tide network follows tidal *rivers* far inland, and a station 3.6km from Trenton, NJ (the state capital, ~50 miles from the ocean, but the Delaware is tidal that far up) is *closer* than a genuinely coastal town like Ocean City, NJ measures to its own nearest station (4.8km) — no distance threshold can exclude Trenton without also excluding real coastal towns. Landed on **10km**, chosen empirically to correctly exclude this app's known false positives (Franklin Park 12.4km, Hillsborough Township 17.7km) while keeping every genuinely coastal town checked (all under 5km). Deep-tidal-river state capitals remain a known, unsolved edge case — properly fixing that would need real coastline geometry, not just station proximity, which isn't in the bundled data. Documented in full in `TideStations.nearest()`'s own doc comment and in `CLAUDE.md`.
- **Bundled asset, not a runtime fetch.** NOAA's full station list is ~1.9MB; trimmed to just `id`/`name`/`lat`/`lng` (~283KB) and bundled as `assets/tide_stations.json` (~3500 US stations) — avoids any network call just to check "is this coastal," matches the app's offline-first ethos, loaded once and cached in memory.
- **Wired end-to-end:** new `TideApi`/`TideModels.kt`/`TideStations.kt`, a sixth `NetworkModule` Retrofit client, a parallel `async` fetch in `WeatherRepository.getWeather()` (same `runCatching { }.getOrNull()` defensive pattern as NWS alerts — a lookup miss or fetch failure never fails the overall weather fetch), a new conditional `TideTile`/`TideDetailContent` (only exists when `WeatherData.tides != null`, same "absent, not empty" pattern as the alert banner), and a `UnitSystem.tideApiUnit`/`tideHeightLabel` pair for feet-vs-meters.
- **Bonus fix, same theme:** the Moon Phase detail sheet's phase-cycle description already mentioned "moderate tidal ranges" at quarter moons (neap tides) but never mentioned that new/full moons bring the *strongest* tides (spring tides) — added, closing the exact gap the user's original question about moon-phase/tide linkage raised (their own guess was waxing/waning gibbous; the actual tide-relevant phases are new/full and first/last-quarter).
- **Verified end-to-end on-device (emulator, mocked GPS to Atlantic City, NJ):** Tides tile showed "0.9 ft, Low tide at 12:23 AM"; detail sheet listed all four of the day's events (Low 12:23 AM 0.9ft, High 6:16 AM 3.5ft, Low 12:07 PM 0.9ft, High 6:34 PM 4.7ft) — matching the live `curl` output from the same station exactly (0.859→0.9, 3.508→3.5, 0.857→0.9, 4.676→4.7, rounding only). Coastal-*exclusion* side (Franklin Park/Hillsborough/Princeton all correctly returning no station within 10km) verified offline in Python against the actual bundled asset and the exact haversine formula used in `TideStations.kt`, rather than fighting emulator GPS mocking a second time for the negative case.
- **ProGuard/R8:** no new rule needed — the existing `-keep class com.example.weatherly.data.model.** { *; }` wildcard already covers the new `TideModels.kt` classes; confirmed by a full `assembleRelease` pass.

**Verification note, this whole round:** `assembleDebug`/`lint`/`test` (26/26) and a full `assembleRelease` (R8 + resource shrinking + signing) all pass. On-device testing was emulator-only — GPS mocking was unusually slow to settle this session (Play Services took several minutes to honor `adb emu geo fix` on a fresh boot; a `pm clear` mid-session cost real time re-discovering this), and the in-app city-search sheet's tap targets turned out to be far from where they visually appeared to be in screenshots (confirmed via `uiautomator dump`'s real bounds, not a code bug — just a lesson for next time: get real bounds before tapping blind). No physical-device pass yet; the signed release APK is built and ready, pending the device reconnecting.

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

### D7. Live advisory text on UV/AQI tiles, instead of a metrics glossary — ✅ IMPLEMENTED (2026-08-24)

**Implemented, and extended to Wind and Humidity too — see "Completed — Widget Stale-While-Revalidate, D7/D8, and B32 (2026-08-24)" above for what shipped and how it was verified.** Left below as the original plan/rationale.

**What:** Considered (not yet built) as a lower-overkill alternative to a Pixel-Weather-style expandable "what the numbers mean" glossary section. A full glossary explaining all ~10 metric tiles (UV, Humidity, Wind, Feels like, Precipitation, Air Quality, Pressure, Sunrise, Visibility, Moon Phase) was judged likely overkill: half the metrics are self-evident (Wind, Sunrise, Visibility, Feels like), and the non-obvious ones already have contextual explanation one tap away in the existing `DetailSheet` (e.g. Pressure's description already reads "Atmospheric pressure is 1013 hPa. Around 1013 hPa is average; falling pressure often signals incoming unsettled weather..."). A glossary card would mostly duplicate that.

The higher-value version: surface the "so what does this mean for me today" advisory **directly on the tile**, live, with zero taps — teaching meaning in the moment instead of requiring the user to seek out and expand a separate section.

**Change:**
- `buildMetricTiles()` already computes full-sentence `uvAdvice`/`aqiAdvice` strings used in `DetailSheet.Metric.description` (e.g. "Wear sunscreen and a hat on bright days.", "Air quality is good — a great time to be outdoors."). These are too long for a tile — add a short, tile-length counterpart for each (a few words, e.g. "Wear SPF", "Great for outdoors").
- Add an optional `advisory: String?` field to `MetricTileData`, populated only for **UV Index** and **Air Quality** initially — Wind/Pressure/Visibility/Humidity/Feels like don't currently have a natural "what should I do" phrase computed for them, and shouldn't get a placeholder just to fill the slot.
- `PrimaryStatCell` (the UV/Humidity/Wind strip): render `advisory` as an optional 4th line below `sub`, for the UV cell only.
- `ArcGaugeTile` (AQI, Pressure): render `advisory` as a second line under the existing `sub` (AQI's band label, e.g. "Good") inside the arc.

**Open question before building:** tile-space is already tight (`sub` is capped at `maxLines = 1` in `PrimaryStatCell`) — needs a real on-device check that a 4th line doesn't push the primary strip's height into visibly uneven card sizing next to Humidity/Wind cells that won't have an advisory line.

**File:** `app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt` (`MetricTileData`, `buildMetricTiles`, `PrimaryStatCell`, `ArcGaugeTile`)

---

### D8. Real Open-Meteo data points the app doesn't request yet — items 1–2 ✅ IMPLEMENTED (2026-08-24)

**Items 1 (wind gust forecast) and 2 (dew point) implemented — see "Completed — Widget Stale-While-Revalidate, D7/D8, and B32 (2026-08-24)" above for what shipped and how it was verified.** Items 3–7 (AQI pollutant breakdown, snow depth, daily feels-like range, precipitation hours, sunshine duration) remain unimplemented. Left below as the original plan/rationale for all seven.

**What:** Audited `OpenMeteoApi.kt`'s `current`/`hourly`/`daily` query params and `AirQualityApi.kt`'s params against Open-Meteo's full variable catalog. Nothing already fetched is going unused — but several real, free variables aren't requested at all. Ranked by likely value:

1. **`wind_gusts_10m` (hourly, and `_max` daily) — highest value.** Only the *current* gust is fetched today (`CurrentBlock.windGusts`); there's no gust forecast at all. The existing Wind detail sheet (compass rose + intensity bars) and `WeatherBackground`'s `severeWind` blizzard-streak effect (see CLAUDE.md) both only react to right-now gust — an upcoming windy day can't be shown or forecast-visualized without this.
2. **`dew_point_2m` (current/hourly) — high value.** Not fetched anywhere. A more accurate "how muggy it'll actually feel" signal than relative humidity alone (the same % means something very different at 50°F vs. 90°F). Fits the app's existing pattern of preferring the real physical signal over an approximation — same instinct that drove the rain-vs-snow accuracy work.
3. **Air quality pollutant breakdown (`pm2_5`, `pm10`, `ozone`, `carbon_monoxide`, `nitrogen_dioxide`) — moderate value, bigger lift.** `AirQualityApi.kt` only requests `us_aqi` (the composite number). The AQI tile can say the air is bad but never *why* — smoke (PM2.5) vs. ozone vs. traffic pollution reads very differently for someone with asthma. Needs a new sub-view, not just a new field, so more UI work than the other items here.
4. **`snow_depth` (current/hourly) — moderate value.** Ground accumulation, distinct from `snowfall` (rate) which is all the app currently tracks. Natural complement to the existing rain-vs-snow-amount work: "how much snow is already on the ground," not just "how much fell in the last hour."
5. **`apparent_temperature_max`/`_min` (daily) — low-moderate value.** `HourEntry` already has per-hour feels-like; `DayEntry` doesn't, so the 7-day forecast can't show a daily feels-like range, only raw high/low.
6. **`precipitation_hours` (daily) — low value.** How many hours of the day actually see precip, distinct from the probability/amount already shown (distinguishes "brief shower" from "rain most of the day").
7. **`sunshine_duration`/`daylight_duration` (daily) — low value, niche.** Not fetched; lowest priority of this list.

**Change:** For each adopted item, add the query param to `OpenMeteoApi.kt` (or `AirQualityApi.kt` for #3), the corresponding field(s) to `OpenMeteoModels.kt`/`AirQualityModels.kt`, map into `WeatherData`/`DayEntry`/`HourEntry` in `WeatherRepository.kt`, then wire into whichever UI already has a natural home for it (Wind detail sheet for #1, Humidity tile/chat brief for #2, a new AQI breakdown sub-view for #3, Precipitation tile for #4, `DailyCard`/`DetailSheet.Day` for #5–6).

**File:** `app/src/main/java/com/example/weatherly/data/remote/OpenMeteoApi.kt`, `AirQualityApi.kt`, `app/src/main/java/com/example/weatherly/data/model/OpenMeteoModels.kt`, `AirQualityModels.kt`, `app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt`

---

## AI Assistant — Strategic Note (2026-06-30)

Average weather app session: **60–90 seconds, 3–5 sessions/day.** Sessions are too short for a conversational interface to be the primary interaction model — which is why the local `WeatherAdvisor` chips (zero latency, zero API cost) should handle the daily-use questions.

The AI's job is the **weekly planning query**: *"What's the best day for a BBQ this week?"*, *"I'm hiking Saturday — what gear do I need?"* These require multi-day synthesis and personal context that no chart can answer. The name "SkySpeak" makes AI load-bearing to the brand — removing it would create a bigger problem than keeping it.

**One fix needed before ship:** `ChatViewModel.hasKey` is defined but `ChatScreen` never checks it. A user with no API key hits a dead chat input with no explanation. Fix this as part of D4 (empty state) or item 25 (key-entry settings UI).

---

## AI Chat — Keep, but Tighten LLM Usage + Add Guardrails — ✅ IMPLEMENTED (2026-08-25)

**Decision:** AI chat stays (not removing it — reaffirms the Strategic Note above). Direction going forward: route as little as possible through the actual LLM call, and put explicit guardrails around the calls that do happen.

**Implemented and verified on-device (emulator, real OpenRouter key) — see "Completed — AI Chat Tightening: Local-First Routing, Guardrails, Usage Cap (2026-08-25)" below for what shipped, how it was verified, and the exact cap number/message.** Left below as the original plan/rationale.

### Local-first routing (minimize LLM calls)

**Current state:** the six quick-suggestion chips (umbrella, jacket, walk/jog, driving, hiking, what to wear) already answer for free via `WeatherAdvisor` + `ChatViewModel.addLocalExchange()` — zero network, zero cost (see README/CLAUDE.md). But a **typed** question that means the exact same thing (e.g. "should I bring an umbrella today?") always goes through `ChatViewModel.send()` → `ChatRepository.askStreaming()` → OpenRouter, even though `WeatherAdvisor.advise(AdviceIntent.UMBRELLA, ...)` could answer it identically for free. Free-form text has no local-match check at all today.

**Change:** before `ChatViewModel.send()` calls into `ChatRepository`, run the trimmed input through a lightweight keyword/pattern matcher against `WeatherAdvisor`'s six existing `AdviceIntent` values (umbrella/rain, jacket/coat, walk/jog, drive/driving, hike/hiking, wear/clothing — the same vocabulary the chip labels already use). On a match, call `addLocalExchange()` with `WeatherAdvisor.advise(...)` instead of `askStreaming()` — same six intents already implemented, just reachable from typed text, not only chip taps. Only fall through to the real LLM call when nothing matches, reserving OpenRouter for the genuinely open-ended/multi-day "weekly planning" questions the Strategic Note above says are its actual job (e.g. "what's the best day for a BBQ this week").

**Open question:** how aggressive the keyword match should be — a narrow match (exact phrase) under-triggers and still burns LLM calls on obvious cases; a broad match risks misrouting a genuinely open-ended question (e.g. "should I bring an umbrella or just reschedule the trip?") into a local answer that ignores half the question. Needs real examples tried against the matcher before picking a threshold.

**Files:** `app/src/main/java/com/example/weatherly/ui/ChatViewModel.kt` (`send()`), a new matcher (either a function on `WeatherAdvisor` or a small standalone classifier).

### Guardrails

`ChatRepository.systemPrompt()`'s rules today only cover data accuracy ("Base every answer ONLY on the weather data below — do not invent numbers", the rain-vs-snow instruction). Nothing scopes *what kind* of question the assistant should engage with, which matters more here than in most chat features since most real users hit the **developer's own shared build-time key** (`BuildConfig.OPENROUTER_API_KEY`), not a key they entered themselves — see `PreferencesStore.getOpenRouterKey()`'s fallback order. Proposed additions:

1. **Topic scope** — instruct the model to decline/redirect anything not about weather, the forecast, or weather-driven practical advice. Today a user could ask it to write code or translate a paragraph and it would likely try, burning the developer's own key on off-brand requests.
2. **Safety/liability tone** — for driving/hiking-type questions, avoid definitive-sounding safety claims beyond what the official NWS alert data actually says (e.g. don't assert "it's safe to drive" outright; frame as "no active advisories, but conditions can change").
3. **Untrusted-data awareness** — location names, condition text, and NWS alert `headline`/`description` text are all external data (Open-Meteo geocoding, NWS CAP feed) interpolated into the system prompt alongside the actual instructions. Low realistic risk today (no user ever chooses their own NWS alert text), but worth one explicit line telling the model to treat the WEATHER DATA block as data, not instructions, same principle as untrusted content anywhere else.
4. **Usage cap on LLM-routed messages** — no current limit on how many free-form (LLM-routed) messages a session can send using the developer's fallback key. Once local-first routing (above) is in place, add a soft on-device counter (`PreferencesStore`) capping *only* the messages that actually reach OpenRouter — chip taps and local-intent matches stay free and uncounted, since they cost nothing. Exact cap number is an open question, not decided here.

**Files:** `app/src/main/java/com/example/weatherly/data/repository/ChatRepository.kt` (`systemPrompt()`), `app/src/main/java/com/example/weatherly/data/prefs/PreferencesStore.kt` (if a usage cap is adopted).

---

## Completed — AI Chat Tightening: Local-First Routing, Guardrails, Usage Cap (2026-08-25)

All three pieces of the direction logged above, implemented and verified on-device (emulator, real OpenRouter key configured in `local.properties`) in one session.

**1. Local-first routing.** `WeatherAdvisor.matchIntent(text): AdviceIntent?` (new) classifies free-form chat text against the same six intents the chips already answer for free. Resolved the "how aggressive" open question empirically rather than guessing: (a) only messages ≤12 words are considered at all — a longer message is far more likely to be compound/nuanced and would only get half-addressed by a single-intent local answer; (b) intents are checked in a fixed priority order, first match wins, so a message mentioning two keywords still gets exactly one predictable local answer rather than an ambiguous one. `ChatViewModel.send()` calls this before ever considering the LLM path. Also refactored `sendLocal(intent, question, weather, units)` out as the single source of truth for "answer via WeatherAdvisor" (including the "open the weather screen first" fallback) — both `send()`'s new routing and `ChatScreen`'s existing suggestion-chip taps call this one function now, removing a small duplication that existed before (the chip tap handler used to compute the fallback message itself).
- **Verified on-device:** typed "should I bring an umbrella today" produced the exact `WeatherAdvisor.umbrella()` response text ("Probably not needed — only a 1% chance of rain...") with zero OpenRouter calls in logcat. A genuinely off-topic message and a legitimate open-ended weather message ("what is the best day this week for a picnic") both correctly fell through to the LLM (confirmed via a real `POST .../chat/completions` in logcat) — the picnic question got a real, useful multi-day synthesis answer, confirming the routing isn't over-triggering and blocking the LLM's actual job.
- **Tests:** 12 new unit tests for `matchIntent()` (case-insensitivity, each of the six intents, unrelated/empty text falling through to null, the word-count cutoff on a real compound example). 38/38 total passing.

**2. Guardrails.** Three rules appended to `ChatRepository.systemPrompt()`: topic scope (decline/redirect anything with no weather angle), safety/liability tone (no definitive-sounding guarantees for driving/hiking questions), untrusted-data awareness (treat the WEATHER DATA block as data, not instructions).
- **Verified on-device:** "what is the capital of France" got *"I'm a weather assistant—feel free to ask about any weather-related questions!"* instead of an answer — the topic-scope rule working exactly as intended, and (per the point above) not so aggressive that it also blocked the legitimate picnic-planning question asked right after it in the same session.

**3. Daily usage cap.** Resolved the "exact cap number, what happens when hit" open questions this session: `ChatViewModel.LLM_DAILY_CAP = 20` — a generous default (this app's own usage pattern is short, infrequent sessions per the Strategic Note above; the LLM's job is occasional synthesis questions, not constant chatting), meant as a real safety net against runaway cost on the shared build-time key, not a number expected to bother normal usage. `PreferencesStore.getLlmUsageCountToday()`/`incrementLlmUsageToday()` store a count + date string that self-resets once the stored date isn't today (no explicit midnight reset job needed). Only increments on a *successful* completed exchange — a failed/errored call costs the user nothing. Gated by `PreferencesStore.hasOwnOpenRouterKey()` (new) — the cap **never** applies once the user has entered their own key in Settings, since it exists specifically to protect the developer's shared key, not to limit one the user is paying for themselves.
- **User-specified UX**: when the cap is hit, don't error out — redirect to the local rule engine instead. Implemented as `CAP_REACHED_MESSAGE`, delivered via the existing `addLocalExchange()` (same warm streaming treatment as any other local answer, not an error bubble): *"You've reached today's limit for AI-powered answers — it resets tomorrow. I can still help right now with quick questions like umbrella, jacket, driving, hiking, walking, or what to wear, either typed or from the suggestions below."*
- **Verified on-device** by seeding `weatherly_prefs.xml` directly via `adb run-as` (`llm_usage_date`/`llm_usage_count`) to simulate already being at the cap, rather than sending 20 real LLM messages: an off-topic question at the cap produced the redirect message with zero OpenRouter calls in logcat; a local-intent question ("should I wear a jacket") sent immediately after *still worked normally* even while capped — confirms local routing is checked before the cap, so the cap only ever blocks the LLM path, never the free one.

**Overall verification:** `assembleDebug`, `lint`, `test` (38/38), and a full `assembleRelease` all pass. Zero crashes across the whole on-device test session (checked via a clean `logcat -d | grep FATAL` after each round).

---

## Completed — versionCode 15 (1.0.14): Local Time, Six Real Bug Fixes (2026-09-01)

One session, driven almost entirely by real on-device reports against versionCode 14 (a Pixel 9 Pro, real GPS, real weather including a live Severe Thunderstorm Watch) rather than inspection alone — consistent with this file's running theme that real bugs keep surfacing from actual use, not a sign anything's wrong with the process.

**1. Local time — new feature.** Live-updating clock under the location name in `CurrentHeader`, and next to every row in the locations sheet. Both Open-Meteo's forecast and geocoding responses already return an IANA zone id per result (`timezone=auto` was already being requested); this was pure additive field-mapping, no new API call. See CLAUDE.md's "Local time" section for the full write-up. Verified on-device against real search results spanning wildly different offsets — Nepal's 45-minute offset and a US date-line crossing both computed correctly — and confirmed ticking forward on its own across a live 2-minute window on a real screenshot.

**2. Humidity detail sheet jumping on scroll — fixed.** `ModalBottomSheet`'s default state has an intermediate "partially expanded" stop that fights the sheet content's own `verticalScroll` nested-scroll handling. `skipPartiallyExpanded = true` on the metric-detail sheet's state removes the stop causing the conflict.

**3. OpenRouter model-override exploit — fixed.** Any user on the shared build-time key (i.e., anyone who hadn't entered their own) could still type a paid or `auto`-routing model ID into Settings and have every call bill against the developer's key — `ChatViewModel`'s model resolution never checked whether the user actually had their own key. `PreferencesStore.getEffectiveOpenRouterModel()` now enforces this at the single resolution point; `SettingsScreen`'s model field is also disabled until a key is saved as a first-line UI signal. Verified via the UI state transition on-device: field disabled/grayed with no key, `"Remove saved key"` correctly absent (it had previously shown regardless, since it was keyed off "any effective key" including the build-time fallback, not "the user's own key" — fixed as part of the same pass); field enables and the button appears after saving a real key.

**4. Chat markdown showing as literal asterisks — fixed twice, second time for real.** First pass added a system-prompt rule asking the model not to use markdown — insufficient, since general-purpose models don't reliably follow that, confirmed by a direct user report after the first build shipped. Second pass added actual client-side rendering: `ChatScreen.kt`'s `renderChatText()`, a minimal single-pass `**bold**` parser via `buildAnnotatedString`, applied to both `MessageBubble` and `StreamingBubble` (the chat screen never had any formatting layer before this — literal `Text(text = msg.text)`). Streaming-safe: an unclosed `**` is left as literal text rather than swallowed.

**5. Off-topic chat messages burning a real OpenRouter call — fixed.** The existing topic-scope prompt rule declines off-topic questions, but only *after* a real, billed call already went out — confirmed in an earlier session's own verification notes ("what is the capital of France" reached OpenRouter, then got redirected). `WeatherAdvisor.isObviouslyOffTopic()`: a conservative denylist (strong non-weather signals, only fires when the message also has zero weather-adjacent vocabulary) checked in `ChatViewModel.send()` before the LLM path, deliberately the inverse of `matchIntent()`'s allowlist approach so ambiguous messages still reach the LLM as before.

**6. Light-mode hero legibility (H/L, feels like) — three rounds on the same underlying complaint, escalating as each fix turned out incomplete:**
   - **Round 1** targeted the wrong scenario initially raised (night + rain/snow/fog, which used the same bright daytime sky tone at any hour in light theme — a real, separate bug, fixed in `skyColor()`/`heroBackdropIsDark()`) but didn't resolve the user's actual complaint, since they were describing Overcast conditions specifically.
   - **Round 2** found the real cause for Overcast: `heroTextColors()`'s light-backdrop secondary color measured ~4.3:1 against Overcast's near-gray sky — *under* WCAG's 4.5:1 floor, despite the code's own doc comment describing it as already fixed (the value had been carried over from a different UI context without being re-verified here). Replaced with a dedicated, purpose-tuned color computed to ~6.5:1 against that same gray.
   - **Round 3**, after the user reported it *still* hard to read across multiple different real locations/conditions even with round 2's fix: the remaining gap wasn't color choice at all — `WeatherBackground` is an animated scene (moving particles), not the flat color any contrast-ratio math assumes, so no single static hex value can guarantee legibility against every pixel behind it. Added a subtle drop shadow to just the H/L and feels-like text specifically (not hero-wide), the standard fix other weather apps use for hero text over photographic/animated skies.
   - **Verified:** confirmed via real on-device screenshot at Atlantic City, NJ, at night, during genuinely rainy/Overcast conditions with an active Severe Thunderstorm Watch — all three fixes visible together in one shot (local time, alert banner, and legible H/L/feels-like against the dark scene).

**7. Spurious "Couldn't connect" network errors — real root cause found, not a network issue.** A user report of a persistent connection error survived a manual retry, which ruled out most transient-network explanations already checked (Open-Meteo confirmed healthy independently, phone's WiFi/cellular both validated, raw TCP-443 reachable, no VPN/private-DNS/firewall interference). Added exception logging (`Log.w`, survives release builds) to `WeatherRepository.getWeather()`'s catch block rather than keep guessing, and the very next capture revealed the actual cause: a `kotlinx.coroutines.JobCancellationException` — a normal, expected cancellation event (a fetch superseded by a newer one, or torn down with the screen) — was being caught by the general `catch (e: Exception)` block and reported to the user as a fake network failure. `ChatViewModel` already had the correct guard for this exact class of bug (`catch (e: CancellationException) { throw e }` before the general catch); `WeatherRepository.getWeather()` never did. Fixed by adding the same guard. Also bumped the forecast fetch's retry from 1 to up to 2 retries (3 attempts, 1s/2s backoff) as a secondary resilience improvement, prompted by real (if unrelated) packet loss observed on the reporting user's network during investigation.

**Housekeeping fixed alongside:** a `ConstantLocale` lint warning in the new `util/LocalTime.kt` — a `DateTimeFormatter` built from `Locale.getDefault()` was cached in a top-level `val`, which would freeze in whatever locale was active at first use rather than reflecting a live in-session locale change; moved to compute fresh per call, matching `WeatherRepository.formatNwsTime()`'s existing (correct) pattern.

**Verification:** `compileDebugKotlin`/`assembleDebug`/`lint`/`test` all green throughout (only the one new lint warning above, now fixed); `bundleRelease` + `assembleRelease` both built clean for the final versionCode 15 artifacts; `apksigner verify` (APK) and `jarsigner -verify` (AAB, "jar verified" — same self-signed/no-timestamp warnings as every prior release, cert valid to 2053-11-16) both pass; `aapt2 dump badging` confirms `versionCode='15' versionName='1.0.14'` embedded correctly. Every fix in this list was sideloaded and sanity-tested on the reporting user's real Pixel 9 Pro as it shipped (signature-matched release builds via the actual `weatherly-release.jks`, updating in place over the Play Store install with no data loss) — not just compiled and assumed correct.

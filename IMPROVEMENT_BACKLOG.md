# SkySpeak — Improvement Backlog

**Compiled:** 2026-10-06, against `main` at `14af251` (versionCode 16 / 1.0.15, then in Production review — published at 100% by 2026-10-27).

Everything still open, in one place. Pulled from `IMPROVEMENTS.md`, `NOTIFICATIONS_ROADMAP.md`,
`PREMIUM_UX_ROADMAP.md`, `AI_ROADMAP_NEXT_VERSION.md`, `PLAYSTORE_LAUNCH.md`, a same-day review
note, and two documents since removed (`premium_widget_strategy.md`,
`USER_FEEDBACK_2026-09-09.md` — both in git history), with duplicates merged.

The remaining files are the history (what was done, why, how it was verified). This file is only the
to-do list: when an item is finished, delete it here and record the detail in `IMPROVEMENTS.md`.

**How the code items were checked:** each one in sections 1 and 2 was looked up in the source on
2026-10-06 and is still present at the line given. Nothing was run — no build, no tests, no
device. The IDs (R1, V5, #28 …) are the ones used in the source documents.

**Re-checked 2026-10-12** against `main` at `90de198` (same code as `14af251`): every item in
sections 1 and 2 is still present at the line given, and the unit tests pass (54/54). That
read-through added the items marked "(2026-10-12)" below; none of them was reproduced on a device.

---

## 1. versionCode 17 — correctness fixes

**Status, 2026-10-27:** versionCode 17 (1.0.16) is built and published to Internal Testing and
Closed Testing. **Code freeze until it is in Production** — the items still open below (R5, R6,
A2, A10, the R1 "couldn't check alerts" UI) and the in-app disclosure fix in section 2 move to
versionCode 18. Before Production: the device check below and the privacy step in section 2.

Keep this release small: fixes, their tests, and the privacy corrections in section 2. Ordered by
priority.

**Progress, 2026-10-27:** fixed on `main` with unit tests (suite now 81, all passing), none yet
checked on a device — R1, R2, R2a, R3, R4, R7, R8, R8a, the load race, the denied-location reload,
the chat error wording, A7 (search race and states) and A9 (chip busy guard). Each row says how.
Still open in this section: R6, R5 (can wait), A10 (`OptionPill` semantics, optional), A2
(stretch). Then section 2. Rows stay here until the signed-build checks below pass; then move
them to `IMPROVEMENTS.md`.

| # | Problem | Where | Fix |
|---|---|---|---|
| R1 | **A failed NWS request produces a false "has ended" alert.** `runCatching { }.getOrNull()` turns a timeout or 5xx into an empty alert list, which the tracker reads as every alert having ended. The worker then posts "\<event\> has ended" for an alert still in effect and re-announces it on the next good check; the app shows the green "has ended" strip. This is live: versionCode 16 is in Production at 100%. **Fixed on `main` 2026-10-27, not yet checked on a device** — `WeatherData.alertsChecked` is false after a failed NWS request (HTTP 400, NWS's out-of-coverage answer, still counts as checked), and `AlertTracker.diffAndUpdate(null)` then reports nothing and keeps the tracked set; 5 unit tests in `AlertTrackerTest`. Still open: the UI doesn't yet say "couldn't check alerts". Move to `IMPROVEMENTS.md` once the signed-build check below passes. | `WeatherRepository.kt:111`; `WeatherAlertWorker.kt:68`; `WeatherViewModel.kt:148` | Carry "alerts could not be checked" separately from "no alerts" and skip the diff when the fetch failed. A small sealed result for the optional sources (alerts, air quality, tides) would cover this and let the UI say "couldn't check" instead of implying none. |
| R2 | **Wind thresholds ignore the unit system.** Raw mph is compared with km/h thresholds: a 39 mph gust reads "Breezy" where the same wind in metric reads "Strong gusts", and the wind-streak overlay needs 45 mph instead of 28. | Wind tile advisory, `WeatherComponents.kt:1572`; `windIntensityColor()`, `WeatherComponents.kt:1703`, and its legend at `:1910`; `severeWind`, `WeatherBackground.kt:75` | Convert to km/h first, as `WeatherAdvisor.toKmh` does. **Fixed on `main` 2026-10-27, not yet checked on a device:** all three sites go through `util/WindUnits.kt` (`windToKmh`, `windAdvisory`, `isSevereWind`); the legend's sample values were already km/h and needed no change. `WindUnitsTest` checks the same wind gives the same result in both units. The other wind thresholds (insights, tips, briefing, advisor) were already unit-aware. |
| R2a | **Driving advice mixes units.** The gust is converted correctly but printed as "km/h" to imperial users; visibility is compared raw (`<= 2`, so 2 mi and 2 km are treated alike). | `WeatherAdvisor.kt:195`, `:201` | Print the user's own value and unit; convert visibility before comparing. **Fixed on `main` 2026-10-27, not yet checked on a device:** `Ctx.gustLabel` prints the user's own gust value and unit; `Ctx.visibilityKm` converts miles before the 2 km check. Two new `WeatherAdvisorTest` cases. |
| R3 | **"Rain expected" can appear during snow.** With snow showers now (85/86) and snow ahead (71–77), the snow branch is skipped as "already snowing" and the hour falls through to `code in 61..82`. | `WeatherRepository.buildDayInsights()`, `WeatherRepository.kt:629–644` | Restrict the rain branch to `61..67` and `80..82`. **Fixed on `main` 2026-10-27, not yet checked on a device:** both the "already raining" check and the rain branch now use `61..67`/`80..82`, snow uses `util/WeatherIcon.kt`'s `isSnowCode()`. `buildDayInsights()` itself has no direct test (private); `PrecipCodesTest` covers the ranges. |
| R7 | **Rain/snow labelling slips.** `skyColor()` tests `71..86` before `51..82`, so rain showers (80–82) get the snow sky (`heroBackdropIsDark()` mirrors the order). The day view says "Chance of rain" and has no snowfall row on snow days. The chat context strip and the widget say "% rain" for a probability that doesn't distinguish type. | `ConditionColors.kt:25–28`; `WeatherComponents.kt:2280`; `ChatScreen.kt:217`; `WeatherWidget.kt:585`, `:1014` | Split the ranges; say "precipitation", or pick the word from `snowfallSum`. **Fixed on `main` 2026-10-27, not yet checked on a device:** `skyColor()`/`heroBackdropIsDark()` use `isSnowCode()`/`isRainCode()`; the day view, chat strip and widget say "rain" or "snow" via `precipWord(snowfallSum)` (same length, so the widget's tight layouts are unaffected); the day view adds a Snowfall row in `snowUnit` on snow days. `PrecipCodesTest`. |
| R4 | **The showcase chat example is misrouted.** "Best day this week for a long run?" — shown in the chat empty state and the store description — matches `WALKING_RE` on "run" and gets the local "right now" answer instead of reaching the LLM. | `ChatScreen.kt:270`; `WeatherAdvisor.kt:45`, `matchIntent()` | Don't route locally when the message names another time frame ("this week", "tomorrow", a weekday, "best day"). **Fixed on `main` 2026-10-27, not yet checked on a device:** `matchIntent()` returns null when the message mentions tomorrow, tonight, later, week/weekend, a weekday, or "best/which/what day", so it reaches the LLM. Two new `WeatherAdvisorTest` cases, including the showcase question itself. Not run against a real phrasing corpus — "this afternoon"/"this evening" still route locally. |
| R8 | **Hero temperature and the "Now" hourly cell can differ by a degree** (user-reported 2026-10-06, intermittent). The hero uses Open-Meteo's `current` block; "Now" is the first hourly slot at or after the current time, i.e. the next full hour. Cause confirmed 2026-10-12: `current` is 15-minute data (`interval: 900`), and a live call with `current.time` at 21:30 selected the 22:00 hourly slot — so "Now" is the next hour's forecast except in the first quarter of each hour. | `nowIndex`, `WeatherRepository.kt:194`; `hourEntryAt()` | Show the current temperature in the "Now" cell, or relabel it. The day view shares `hourEntryAt()`, so re-check the "identical values in both strips" guarantee afterwards. **Fixed on `main` 2026-10-27 (with R8a), not yet checked on a device:** `nowIndex` is the slot at or before `current.time` (`WeatherRepository.currentHourIndex()`), and `hourEntryAt()` gives the "Now" cell the `current` block's temperature, feels-like, icon and day/night, so it matches the hero. Since that happens inside `hourEntryAt()`, the day view's Today strip shows the same values — the guarantee holds by construction. `CurrentHourIndexTest`. Not changed: the metric sparklines' first point (e.g. Feels Like) is still the hour slot's forecast. |
| R8a | **Late-evening knock-on of R8** (2026-10-12). From about 23:15 to midnight the hourly window starts at tomorrow's 00:00, labelled "Now", so no "12 AM" label exists in it and today's corrected rain chance is taken from all 24 of tomorrow's hours. Current UV and visibility are also read from the next hour's slot at any time of day. | `todayRemainingCount`, `WeatherRepository.kt:274`; `dayPop`, `:291`; `uvNow`/`visMeters`, `:342` | Pick the hourly slot at or before the current time instead of at or after it. Fixes this and R8 together. **Fixed on `main` 2026-10-27 with R8.** |
| — | **Full reload on every return from Chat or Settings for users who denied location but use a saved city** (2026-10-12). `WeatherScreen` re-requests the permission each time it re-enters composition; the denied branch calls `onPermissionDenied()`, which does a foreground `load()`. The screen drops to the spinner, scroll position is lost, and the condition haptic can replay. The granted branch already guards against this. | `WeatherScreen.kt:147`; `WeatherViewModel.kt:159` | Use a background load when content is already showing, as the granted branch does; better, don't re-request a permission that was already denied. **Fixed on `main` 2026-10-27, both ways, not yet checked on a device:** the screen loads directly when location is already granted, shows the system prompt at most once per app session (`WeatherViewModel.locationPermissionRequested`), does nothing on return when a forecast is already showing (the `ON_RESUME` background refresh covers it), and the denied callback uses a background load when content is on screen. The "Grant location access" button still asks. No unit test (permission flow). |
| — | **Chat error wording** (2026-10-12). The 401 message says "Check it in the key field above", but the key field is in Settings. The 402 message refers to "your OpenRouter account" even for users on the shared key, which is what they would see if the shared key's credit limit is reached. | `ChatRepository.httpMessage()`, `ChatRepository.kt:133` | Point to Settings; give shared-key users a message that doesn't mention an account they don't have. **Fixed on `main` 2026-10-27, not yet checked on a device:** `httpMessage(code, ownKey)` (now in `ChatRepository`'s companion, `ownKey` from `PreferencesStore.hasOwnOpenRouterKey()`) points own-key users to Settings, and tells shared-key users the assistant is unavailable (401) or has reached its usage limit (402), that the quick suggestions still work, and that they can add their own key in Settings. `ChatErrorMessageTest`. |
| — | **Two quick place changes can show the older result.** `load()` keeps no job handle and never cancels an in-flight load. | `WeatherViewModel.kt:93` | Hold the load `Job` and cancel it when a new load starts. **Fixed on `main` 2026-10-27 via `util/LatestJob` (with A7 below), not yet checked on a device.** |
| R6 | **Device clock used for a remote city.** The sun dot, the dawn/dusk sky tint and the golden-hour motes use the phone's time against the viewed city's sunrise/sunset. | `WeatherComponents.kt:1115`; `ConditionColors.kt:38`; `WeatherBackground.kt:84` | Take "now" from `WeatherData.timezone`. |
| A7 | **City search can show results for an older query** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). Each `search()` launches independent work, so a slow earlier response can overwrite a later one. Shortening the query below two characters leaves old results showing, and a failed search (`searchCity()` returns an empty list on any error) looks the same as "no matches". | `WeatherViewModel.search()`, `WeatherViewModel.kt:215`; `WeatherRepository.searchCity()`, `WeatherRepository.kt:172`; `LocationsSheet`, `WeatherScreen.kt:317` | Hold and cancel the search `Job` (same fix as the load race above); clear results for short queries; show "no matches" and "couldn't connect" separately. **Fixed on `main` 2026-10-27, not yet checked on a device:** `searchCity()` returns a `Result`, the sheet renders a `SearchState` (Idle/Searching/Results/NoMatches/Failed), and queries under 2 characters clear the results. `LatestJobTest` covers the superseding. |
| A9 | **Suggestion chips can start a second answer while one is streaming** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). `send()` returns early while `sending` is true; `sendLocal()`/`addLocalExchange()` don't, and both write the shared streaming state. | `ChatViewModel.sendLocal()`, `ChatViewModel.kt:137` | Same early return as `send()`, or disable the chips while sending. **Fixed on `main` 2026-10-27, both ways, not yet checked on a device:** `addLocalExchange()` returns early while sending, and the chips are disabled while an answer streams. No unit test (`ChatViewModel` needs Android). |
| A10 | **Settings option pills have no selected/toggle semantics** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). Selection is shown by colour only, so TalkBack can't tell which unit or theme is chosen. Small; include only if it fits. | `OptionPill`, `SettingsScreen.kt:614` | `Modifier.selectable(selected, role = Role.RadioButton)` (or `toggleable` for on/off pills). |
| A2 | **Stretch for 17 — the widget can show a browsed city as if it were the device's location** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). `ForecastCache` holds one forecast, written by the app for whatever place is being viewed; the widget renders it first and keeps it if its own location fetch fails. The widget's fresh results are never persisted. Separately, the app's "Showing data from Xm ago" label doesn't keep ageing on screen, and a memory-cache hit is saved with the current time as if newly fetched. | `ForecastCache.kt`; `provideGlance()`, `WeatherWidget.kt:182`; `WeatherViewModel.load()` | A second cache slot for the device's location, read and written by the widget; keep the original fetch time. Ship in 17 if ready, otherwise first item for 18. |
| R5 | **Background animation probably gets choppy with uptime.** The frame clock is converted to `Float`, which loses millisecond precision as it grows — about 16 ms steps after 37 hours awake, 64 ms after 150. From arithmetic, not observed. | `wrap01()`, `WeatherBackground.kt:372`, and each `timeMs / 1000f` | Record the first frame time and animate on the difference. Can wait for a later release. |

**Tests to add with these fixes.** The suite is 54 JVM tests across `WeatherAdvisorTest`,
`ForecastBriefingTest` and `AlertTrackerTest`; nothing covers repository mapping, which is where
most of the bugs above live.

- Alert fetch failed vs. succeeded-and-empty (R1).
- A superseded search or load never overwrites the newer one (A7, load race).
- Same physical wind and visibility in metric and imperial gives the same advice (R2, R2a).
- WMO 80–82 take the rain path; 71–77 and 85–86 take the snow path (R3, R7).
- `matchIntent()` returns null for future-time-frame questions (R4).
- "Now" selection with a mid-hour current timestamp (R8), and today's rain chance with a
  current timestamp after 23:00 (R8a).
- Old cached JSON without newer fields still deserializes (`ForecastCache` defaults).

**Signed-build checks before shipping 17:** NWS unreachable with an alert tracked; an alert
genuinely ending; metric/imperial switch; a snow forecast; background location denied;
notifications toggled on and off.

---

## 2. Privacy and policy

Fix `docs/privacy.html`, the in-app disclosure text and the Play Console declaration together —
they are meant to say the same thing.

| Problem | Where | Fix |
|---|---|---|
| NOAA CO-OPS (tides) receives a station id but isn't listed among third parties. | `docs/privacy.html` (no mention of NOAA) | Add it. **Page fixed 2026-10-27.** |
| Reverse geocoding isn't disclosed (2026-10-12). When no place name is supplied, coordinates go to Android's `Geocoder`, which is Google-backed on most phones — in the app, the background worker and the widget. The in-app disclosure says location is sent "only to Open-Meteo and the U.S. National Weather Service", and the privacy page lists the same two. Whether a platform service counts as a recipient is a judgement call; the word "only" is the weak point. | `reverseGeocode()`, `WeatherRepository.kt:807`; `BACKGROUND_LOCATION_DISCLOSURE`, `SettingsScreen.kt:594`; `docs/privacy.html` | Mention the device's geocoding service in all three, or drop reverse geocoding from the background path (the worker only needs a name for the status notification). **Page fixed 2026-10-27.** **Still open:** the in-app `BACKGROUND_LOCATION_DISCLOSURE` and the Play Console declaration still say "only to Open-Meteo and the U.S. National Weather Service" — needs versionCode 18 (17 is already uploaded); recommended fix is dropping the background name lookup so that wording becomes true. |
| Says chat text leaves the device "only if you've configured an OpenRouter API key"; the Play build ships with one. | `docs/privacy.html:64`, `:134` | Describe the shipped behaviour. **Page fixed 2026-10-27.** Also now says the place name and recent messages are sent, and that local answers aren't. |
| Says the key is entered in the chat screen; it is in Settings. | `docs/privacy.html` | Correct. **Page fixed 2026-10-27.** |
| Says NWS receives background location only with Alert Notifications on; the Weather Status Notification alone also calls NWS. | `docs/privacy.html:53` area | Correct. **Page fixed 2026-10-27.** The widget's location use is now described too. |
| Says uninstalling removes all data, but `android:allowBackup="true"` lets preferences — including a user-entered OpenRouter key — into device backups. | `docs/privacy.html:152`; `AndroidManifest.xml:34` | Exclude the preferences from backup (preferred, at least the key), or disclose it. **App side fixed on `main` 2026-10-27, not yet checked on a device:** the user's key moved to its own `weatherly_secrets` preferences file (an existing key is moved on first read), excluded from cloud backup and device transfer by `res/xml/backup_rules.xml` (Android 11 and lower) and `data_extraction_rules.xml` (12+); places, units and settings still back up. `docs/privacy.html` now (2026-10-27 draft) says device backups may include the app's data and that uninstalling doesn't remove a backup — true for both 16 and 17. **Still open:** when versionCode 17 goes to Production, add to the "Data stored on your device" uninstall paragraph: "Your OpenRouter API key is never included in a backup." Not before — GitHub Pages serves `main` straight away and it's false for versionCode 16. |
| No in-app link to the privacy policy. Google tends to expect one for apps using background location. | Settings | Add a link row in Settings. **Fixed on `main` 2026-10-27, not yet checked on a device:** "Privacy policy" row under Settings → About opens the same GitHub Pages URL Play Console lists. |
| The shared OpenRouter key ships inside the APK; in-app limits don't stop someone who extracts it. Currently limited only by a $2 credit cap on OpenRouter's side. | build config | Accept as is, or move calls behind a small proxy — needs a decision (see section 6). |

---

## 3. Release and Play Console follow-ups

No code needed unless a check turns something up.

- **Non-Pixel testing of notifications.** Never done; the battery-exemption prompt is a
  mitigation, not a substitute. Ask a few Closed Testing testers on Samsung/Xiaomi/OnePlus to
  confirm checks fire on a normal day.
- **Paths not yet run on any device:**
  - the disclosure flow for a user who never granted ordinary location;
  - a real severe-alert notification, and an NWS update arriving without a false "ended";
  - the background-location disclosure flow as a whole ("compiles; not yet run on a device");
  - whether the widget's scheduled refresh falls back to cache without "Allow all the time";
  - haptic patterns (timings are a first pass);
  - `ForecastBriefing` wording on a real screen;
  - the Extreme UV insight — no location reached UV 11 in September; retry near the December
    solstice with a high-altitude Southern Hemisphere site.
- **Known gap:** granting background location directly in system Settings while on the Weather
  screen doesn't trigger an immediate check; it waits up to 30 minutes.
- **Android Vitals:** watch crash and ANR rate once versionCode 16 has installs. Re-check
  Policy → App content after rollout; `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` may still be queried.
- **V5 — "Optimised resource shrinking isn't enabled"** is still flagged despite
  `optimization { enable = true }`. Experiment: remove the legacy `isShrinkResources` line, confirm
  the build output changes, upload and re-check.
- **Device check of the versionCode 17 fixes.** The release-signed versionCode 17 build
  (`7720e0f`, the same code as the AAB on Internal and Closed Testing) is installed on the Pixel 9
  Pro since 2026-10-27 9:32 PM; test steps emailed to the developer. Record results against each
  section-1 row. R1 can't be exercised by hand — a debug-only "simulate NWS failure" switch
  was offered.
- **Restore the Pixel 9 Pro to the Play Store build** when testing is done (it now runs a
  sideload of versionCode 17 / 1.0.16, ahead of Production). Once 17 is in Production the
  sideload and the Play build are the same signed code, so the Play Store will treat it as
  up to date; a reinstall is then optional.

---

## 4. Product improvements

Not started. Roughly smallest first.

| Item | Notes | Effort |
|---|---|---|
| Notification setup status in Settings | Both toggles can be on while background location is missing, and the worker then does nothing silently. Show "Ready" / "Needs background location" / "Battery restricted" with the existing action buttons. | Small |
| Share current weather (#30) | Share button in the header; plain-text summary via `ACTION_SEND`. No new dependency. | Half a day |
| In-app review prompt (#35, remainder) | The Settings "Rate on Google Play" link exists. Still open: the Play In-App Review API, triggered after a good interaction. | Half a day |
| Play Store screenshot overlays (#34) | Short text callouts on the screenshots; asset work only. | Half a day |
| TALL widget blank space | TALL still shows empty space at its nominal size; XLARGE was fixed (B27), the others weren't. Margins are tight, so check for clipping. | Half a day |
| More local advice intents | Cycling, gardening, outdoor events in `WeatherAdvisor`. | 1 day |
| Onboarding walkthrough (#33) | 2–3 first-launch screens (chat, widget, notifications), skippable, "seen" flag in `PreferencesStore`. | 1–2 days |
| Daily digest notification | One morning notification with high/low and the top tip. Deliberately left out of the first notifications release. | 1–2 days |
| Dead code removal (2026-10-12) | `WeatherData.tips` and `buildTips()` are computed and cached on every fetch but never rendered (only `DayEntry.tips` is). Also unused: the `MetricTile` composable, `OpenRouterApi`, `NetworkModule.openRouterApi` and `ChatCompletionResponse` (chat uses the raw streaming call). Removing `WeatherData.tips` changes the cached JSON shape — old caches still load, since Moshi ignores unknown keys. | Half a day |
| Field naming cleanup | `currentTempC`, `windKmh` and similar hold values in the user's unit, not the unit in the name. This is the trap behind R2 and the earlier range-bar bug. IDE rename across about 15 files; cached JSON keys need care. | 1 day |
| Localization (#28) | `strings.xml` holds only the app name. Around 60 strings to extract. The hour labels are parsed by code and must stay `Locale.US` until that parsing is replaced. | 1–2 days |

---

## 5. Documentation cleanup

Done 2026-10-06 (see the last entry in `IMPROVEMENTS.md`). The `CLAUDE.md` "`TipBanner` source
data" paragraph was corrected 2026-10-28. Nothing open.

---

## 6. Needs a decision or a spike first

| Question | What's known | Next step |
|---|---|---|
| Keep, scale back or expand AI chat? | "Isn't used much" can't be confirmed — the app has no analytics by design. Removing free-form chat touches the SkySpeak name. | Decide whether an on-device-only usage counter is acceptable, or decide on judgment. Voice input stays shelved until this is settled. |
| AI forecasting (WeatherNext 2 via Open-Meteo's Ensemble API) | Available on the free tier, including a pre-computed ensemble mean. No published comparison against the `best_match` model the app uses now. | Half-day spike: compare ensemble mean with `best_match` for a few real locations. |
| Paid data and monetization | Minute-level "rain in 15 minutes" alerts, a second forecast provider and the premium-widget subscription all need a paid source, and a paid app loses Open-Meteo's free non-commercial tier. Raised separately in three documents. | One decision: does the app stay free on Open-Meteo? If yes, close all three. |
| Shared OpenRouter key in the APK | See section 2. | Accept the $2 cap, or build a proxy. |
| True blur for cards (`RenderEffect`, API 31+) | Alpha-only translucent cards were tried and reverted. Real blur is untested and needs a fallback for API 26–30. | Short on-device spike, or shelve. |
| AGSL mesh-gradient background | Needs API 33+ with a fallback below. | Prototype only if the blur spike goes ahead. |
| Tilt-responsive particles | Shelved 2026-09-04, not rejected. | None until picked up. |
| Rename the `com.example.weatherly` package | Not needed by Play; adds release risk. | Leave unless a technical need appears. |

---

## 7. Closed — don't reopen without new information

- Lock-screen widgets; "semantic lifestyle context" (liability) — dropped 2026-09-04.
- Radar screen — removed as low value.
- Alpha-blended translucent cards — tried and reverted (true blur is the separate open question above).
- Widget showing the day's high in the morning (B26) — by design.
- Vitals V1 and V4 — cleared. V2/V3 (edge-to-edge deprecations) come from `androidx.activity`'s
  own compatibility code on the latest stable version; nothing to fix here.
- User feedback of 2026-09-09: offline caching and tap-to-ask chips already existed; deterministic
  extreme-UV and freeze warnings were added the same day.

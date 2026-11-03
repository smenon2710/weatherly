# SkySpeak — Improvement Backlog

**Compiled:** 2026-10-06, against `main` at `14af251` (versionCode 16 / 1.0.15, then in Production review — published at 100% by 2026-10-27). **Updated 2026-11-03:** versionCode 17 (1.0.16) published to Production 2026-10-29; its finished rows moved to `IMPROVEMENTS.md`.

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

## 1. versionCode 18 — next fixes

versionCode 17 (1.0.16) was published to Production at full rollout on 2026-10-29 (Submission 29);
its fixes are recorded in `IMPROVEMENTS.md`. The code freeze is over. Open code items, ordered by
priority (the in-app disclosure fix in section 2 also belongs in this release):

| # | Problem | Where | Fix |
|---|---|---|---|
| R1b | **The app doesn't say when alerts couldn't be checked.** R1's fix (versionCode 17) stops a failed NWS request from reading as "every alert ended", but the screen still looks the same as "no active alerts". | `WeatherData.alertsChecked`; `AlertBannerList` | Show a quiet "Couldn't check weather alerts" line when `alertsChecked` is false. |
| R6 | **Device clock used for a remote city.** The sun dot, the dawn/dusk sky tint and the golden-hour motes use the phone's time against the viewed city's sunrise/sunset. | `WeatherComponents.kt:1115`; `ConditionColors.kt:38`; `WeatherBackground.kt:84` | Take "now" from `WeatherData.timezone`. |
| A2 | **The widget can show a browsed city as if it were the device's location** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). `ForecastCache` holds one forecast, written by the app for whatever place is being viewed; the widget renders it first and keeps it if its own location fetch fails. The widget's fresh results are never persisted. Separately, the app's "Showing data from Xm ago" label doesn't keep ageing on screen, and a memory-cache hit is saved with the current time as if newly fetched. | `ForecastCache.kt`; `provideGlance()`, `WeatherWidget.kt:182`; `WeatherViewModel.load()` | A second cache slot for the device's location, read and written by the widget; keep the original fetch time. |
| A10 | **Settings option pills have no selected/toggle semantics** (2026-10-27, from `USER_ADOPTION_SUGGESTIONS.md`). Selection is shown by colour only, so TalkBack can't tell which unit or theme is chosen. Small. | `OptionPill`, `SettingsScreen.kt:614` | `Modifier.selectable(selected, role = Role.RadioButton)` (or `toggleable` for on/off pills). |
| R5 | **Background animation probably gets choppy with uptime.** The frame clock is converted to `Float`, which loses millisecond precision as it grows — about 16 ms steps after 37 hours awake, 64 ms after 150. From arithmetic, not observed. | `wrap01()`, `WeatherBackground.kt:372`, and each `timeMs / 1000f` | Record the first frame time and animate on the difference. |

**Test still to add:** old cached JSON without newer fields still deserializes (`ForecastCache`
defaults).

---

## 2. Privacy and policy

Fix `docs/privacy.html`, the in-app disclosure text and the Play Console declaration together —
they are meant to say the same thing. The page corrections made for versionCode 17 are recorded in
`IMPROVEMENTS.md`.

| Problem | Where | Fix |
|---|---|---|
| Reverse geocoding isn't disclosed (2026-10-12). When no place name is supplied, coordinates go to Android's `Geocoder`, which is Google-backed on most phones — in the app, the background worker and the widget. The in-app disclosure says location is sent "only to Open-Meteo and the U.S. National Weather Service", and the privacy page lists the same two. Whether a platform service counts as a recipient is a judgement call; the word "only" is the weak point. | `reverseGeocode()`, `WeatherRepository.kt:807`; `BACKGROUND_LOCATION_DISCLOSURE`, `SettingsScreen.kt:594`; `docs/privacy.html` | Mention the device's geocoding service in all three, or drop reverse geocoding from the background path (the worker only needs a name for the status notification). **Page fixed 2026-10-27.** **Still open:** the in-app `BACKGROUND_LOCATION_DISCLOSURE` and the Play Console declaration still say "only to Open-Meteo and the U.S. National Weather Service" — needs versionCode 18 (17 is already uploaded); recommended fix is dropping the background name lookup so that wording becomes true. |
| The shared OpenRouter key ships inside the APK; in-app limits don't stop someone who extracts it. Currently limited only by a $2 credit cap on OpenRouter's side. | build config | Accept as is, or move calls behind a small proxy — needs a decision (see section 6). |

---

## 3. Release and Play Console follow-ups

No code needed unless a check turns something up.

- **Store listing changes under review (2026-11-03).** Developer name Scorp#1 → Scorp Labs Apps,
  app title → "SkySpeak: Weather Chat", new short description — see `PLAYSTORE_LAUNCH.md`'s
  "Store Listing Content". When each is approved, record it there; once the title is approved,
  rename the app in `README.md`'s heading and `docs/privacy.html` (opening paragraph and footer).
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
- **Android Vitals:** watch crash and ANR rate for versionCode 17 (Production since 2026-10-29). Re-check
  Policy → App content after rollout; `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` may still be queried.
- **V5 — "Optimised resource shrinking isn't enabled"** is still flagged despite
  `optimization { enable = true }`. Experiment: remove the legacy `isShrinkResources` line, confirm
  the build output changes, upload and re-check.
- **Pixel 9 Pro and the Play Store build.** The phone runs the release-signed versionCode 17
  sideload (`7720e0f`), the same signed code as Production since 2026-10-29, so the Play Store
  treats it as up to date. A Play Store reinstall is optional.

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

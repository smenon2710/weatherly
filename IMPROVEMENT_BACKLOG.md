# SkySpeak — Improvement Backlog

**Compiled:** 2026-10-06, against `main` at `14af251` (versionCode 16 / 1.0.15, in Production review).

Everything still open, in one place. Pulled from `IMPROVEMENTS.md`, `NOTIFICATIONS_ROADMAP.md`,
`PREMIUM_UX_ROADMAP.md`, `AI_ROADMAP_NEXT_VERSION.md`, `PLAYSTORE_LAUNCH.md`, a same-day review
note, and two documents since removed (`premium_widget_strategy.md`,
`USER_FEEDBACK_2026-09-09.md` — both in git history), with duplicates merged.

The remaining files are the history (what was done, why, how it was verified). This file is only the
to-do list: when an item is finished, delete it here and record the detail in `IMPROVEMENTS.md`.

**How the code items were checked:** each one in sections 1 and 2 was looked up in the source on
2026-10-06 and is still present at the line given. Nothing was run — no build, no tests, no
device. The IDs (R1, V5, #28 …) are the ones used in the source documents.

---

## 1. versionCode 17 — correctness fixes

Keep this release small: fixes, their tests, and the privacy corrections in section 2. Ordered by
priority.

| # | Problem | Where | Fix |
|---|---|---|---|
| R1 | **A failed NWS request produces a false "has ended" alert.** `runCatching { }.getOrNull()` turns a timeout or 5xx into an empty alert list, which the tracker reads as every alert having ended. The worker then posts "\<event\> has ended" for an alert still in effect and re-announces it on the next good check; the app shows the green "has ended" strip. This is in the build now in Production review. | `WeatherRepository.kt:111`; `WeatherAlertWorker.kt:68`; `WeatherViewModel.kt:148` | Carry "alerts could not be checked" separately from "no alerts" and skip the diff when the fetch failed. A small sealed result for the optional sources (alerts, air quality, tides) would cover this and let the UI say "couldn't check" instead of implying none. |
| R2 | **Wind thresholds ignore the unit system.** Raw mph is compared with km/h thresholds: a 39 mph gust reads "Breezy" where the same wind in metric reads "Strong gusts", and the wind-streak overlay needs 45 mph instead of 28. | Wind tile advisory, `WeatherComponents.kt:1572`; `windIntensityColor()`, `WeatherComponents.kt:1703`, and its legend at `:1910`; `severeWind`, `WeatherBackground.kt:75` | Convert to km/h first, as `WeatherAdvisor.toKmh` does. |
| R2a | **Driving advice mixes units.** The gust is converted correctly but printed as "km/h" to imperial users; visibility is compared raw (`<= 2`, so 2 mi and 2 km are treated alike). | `WeatherAdvisor.kt:195`, `:201` | Print the user's own value and unit; convert visibility before comparing. |
| R3 | **"Rain expected" can appear during snow.** With snow showers now (85/86) and snow ahead (71–77), the snow branch is skipped as "already snowing" and the hour falls through to `code in 61..82`. | `WeatherRepository.buildDayInsights()`, `WeatherRepository.kt:629–644` | Restrict the rain branch to `61..67` and `80..82`. |
| R7 | **Rain/snow labelling slips.** `skyColor()` tests `71..86` before `51..82`, so rain showers (80–82) get the snow sky (`heroBackdropIsDark()` mirrors the order). The day view says "Chance of rain" and has no snowfall row on snow days. The chat context strip and the widget say "% rain" for a probability that doesn't distinguish type. | `ConditionColors.kt:25–28`; `WeatherComponents.kt:2280`; `ChatScreen.kt:217`; `WeatherWidget.kt:585`, `:1014` | Split the ranges; say "precipitation", or pick the word from `snowfallSum`. |
| R4 | **The showcase chat example is misrouted.** "Best day this week for a long run?" — shown in the chat empty state and the store description — matches `WALKING_RE` on "run" and gets the local "right now" answer instead of reaching the LLM. | `ChatScreen.kt:270`; `WeatherAdvisor.kt:45`, `matchIntent()` | Don't route locally when the message names another time frame ("this week", "tomorrow", a weekday, "best day"). |
| R8 | **Hero temperature and the "Now" hourly cell can differ by a degree** (user-reported 2026-10-06, intermittent). The hero uses Open-Meteo's `current` block; "Now" is the first hourly slot at or after the current time, i.e. the next full hour. Likely cause, not confirmed against a captured response. | `nowIndex`, `WeatherRepository.kt:194`; `hourEntryAt()` | Show the current temperature in the "Now" cell, or relabel it. The day view shares `hourEntryAt()`, so re-check the "identical values in both strips" guarantee afterwards. |
| — | **Two quick place changes can show the older result.** `load()` keeps no job handle and never cancels an in-flight load. | `WeatherViewModel.kt:93` | Hold the load `Job` and cancel it when a new load starts. |
| R6 | **Device clock used for a remote city.** The sun dot, the dawn/dusk sky tint and the golden-hour motes use the phone's time against the viewed city's sunrise/sunset. | `WeatherComponents.kt:1115`; `ConditionColors.kt:38`; `WeatherBackground.kt:84` | Take "now" from `WeatherData.timezone`. |
| R5 | **Background animation probably gets choppy with uptime.** The frame clock is converted to `Float`, which loses millisecond precision as it grows — about 16 ms steps after 37 hours awake, 64 ms after 150. From arithmetic, not observed. | `wrap01()`, `WeatherBackground.kt:372`, and each `timeMs / 1000f` | Record the first frame time and animate on the difference. Can wait for a later release. |

**Tests to add with these fixes.** The suite is 54 JVM tests across `WeatherAdvisorTest`,
`ForecastBriefingTest` and `AlertTrackerTest`; nothing covers repository mapping, which is where
most of the bugs above live.

- Alert fetch failed vs. succeeded-and-empty (R1).
- Same physical wind and visibility in metric and imperial gives the same advice (R2, R2a).
- WMO 80–82 take the rain path; 71–77 and 85–86 take the snow path (R3, R7).
- `matchIntent()` returns null for future-time-frame questions (R4).
- "Now" selection with a mid-hour current timestamp (R8).
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
| NOAA CO-OPS (tides) receives a station id but isn't listed among third parties. | `docs/privacy.html` (no mention of NOAA) | Add it. |
| Says chat text leaves the device "only if you've configured an OpenRouter API key"; the Play build ships with one. | `docs/privacy.html:64`, `:134` | Describe the shipped behaviour. |
| Says the key is entered in the chat screen; it is in Settings. | `docs/privacy.html` | Correct. |
| Says NWS receives background location only with Alert Notifications on; the Weather Status Notification alone also calls NWS. | `docs/privacy.html:53` area | Correct. |
| Says uninstalling removes all data, but `android:allowBackup="true"` lets preferences — including a user-entered OpenRouter key — into device backups. | `docs/privacy.html:152`; `AndroidManifest.xml:34` | Exclude the preferences from backup (preferred, at least the key), or disclose it. |
| No in-app link to the privacy policy. Google tends to expect one for apps using background location. | Settings | Add a link row in Settings. |
| The shared OpenRouter key ships inside the APK; in-app limits don't stop someone who extracts it. Currently limited only by a $2 credit cap on OpenRouter's side. | build config | Accept as is, or move calls behind a small proxy — needs a decision (see section 6). |

---

## 3. Release and Play Console follow-ups

No code needed unless a check turns something up.

- **Production review of versionCode 16** (Submission 27, sent 2026-10-06): wait for the result,
  then record the rollout percentage that was chosen — it isn't written down anywhere.
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
- **Restore the Pixel 9 Pro to the Play Store build** when testing of debug builds is done.

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
| Field naming cleanup | `currentTempC`, `windKmh` and similar hold values in the user's unit, not the unit in the name. This is the trap behind R2 and the earlier range-bar bug. IDE rename across about 15 files; cached JSON keys need care. | 1 day |
| Localization (#28) | `strings.xml` holds only the app name. Around 60 strings to extract. The hour labels are parsed by code and must stay `Locale.US` until that parsing is replaced. | 1–2 days |

---

## 5. Documentation cleanup

Done 2026-10-06 (see the last entry in `IMPROVEMENTS.md`). One thing left: the "Test device vs.
Production" paragraph in `CLAUDE.md` still describes the Pixel 9 Pro as of 2026-09-04; update it
once the phone is back on the Play Store build.

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

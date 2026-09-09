# SkySpeak — Alerts & Notifications Infrastructure (Discussion Doc)

> **v1 implemented (not yet built/installed anywhere) — see "What shipped" below the TL;DR.**

> Not a decision, not a plan — a starting point for a conversation. Prompted by real user
> research (not this app's own — general observation): most weather-app users never place a
> home-screen widget and rarely open the app directly; they rely on push/local notifications as
> the primary surface. This app currently has **zero** notification infrastructure — no
> `WorkManager`, no notification channel, no background execution outside the foreground app or
> an already-placed widget. Checked against what's actually in the codebase before writing
> anything down, the same way `PREMIUM_UX_ROADMAP.md` and `AI_ROADMAP_NEXT_VERSION.md` did.

---

## TL;DR

- **The valuable data already exists.** Severe NWS alerts are already fetched, deduped, and
  severity-scored (`WeatherRepository.mapAlerts()`), and alert-*resolution* tracking already
  exists (`WeatherViewModel.trackAlertChanges()` / `PreferencesStore.getTrackedAlerts()`) — it
  just only runs while the app is in memory and only ever renders an in-app card. The gap isn't
  data, it's **delivery**.
- **The real gap is background execution.** `WorkManager` isn't a dependency in this project at
  all (confirmed in `PREMIUM_UX_ROADMAP.md`'s item #8), and the widget's own update mechanism
  (Jetpack Glance, OS-scheduled) only runs for users who've actually placed a widget — exactly
  the population this feature is trying to reach *instead of*.
- **The core architectural choice is polling (WorkManager) vs. push (FCM).** Polling fits this
  app's entire design principle — no backend, pure client, free public APIs — but collides with
  Android's Doze/App Standby Bucket system, which throttles background work hardest for apps the
  OS sees opened rarely: **the exact users this feature targets**. Push (FCM) would sidestep
  that but requires standing up a real server this app has never had, which is a much bigger
  commitment than anything shipped so far.
- **Recommendation: start with WorkManager polling, two notification types, opt-in.** Severe/
  extreme NWS alerts (reusing existing dedup logic) and an optional once-daily digest. No new
  backend, no new paid data source, no reopening of the Open-Meteo licensing question. True
  minute-level "rain starting soon" push notifications are a different, harder problem — see
  "Out of scope for v1" below.

---

## What shipped (v1)

Implemented per the recommendation above — WorkManager polling, opt-in, notification types #1
and #2 only (daily digest deliberately not built yet, per the open question below). **Not yet
built or installed anywhere** — source changes only, unverified by an actual compile.

- `androidx.work:work-runtime-ktx:2.10.0` added; `POST_NOTIFICATIONS` declared in the manifest.
- `data/repository/AlertTracker.kt` — the alert diff/update logic extracted out of
  `WeatherViewModel.trackAlertChanges()` into a shared, parameterized class (takes `get`/`set`
  functions rather than hardcoding a `PreferencesStore` slot) so the foreground path and the new
  background path can't independently drift.
- `PreferencesStore` gained `getAlertNotificationsEnabled`/`setAlertNotificationsEnabled` (the
  master opt-in, default off) and a **separate** `getBackgroundTrackedAlerts`/
  `setBackgroundTrackedAlerts` slot, distinct from the foreground's existing `getTrackedAlerts` —
  needed because the background worker always checks the saved/selected place while the
  foreground can be viewing a different one (e.g. "current location") at the same time; sharing
  one slot would corrupt the diff. `WeatherViewModel.resetAlertTracking()` now clears both slots
  on a location change.
- `notifications/WeatherNotifications.kt` — two notification channels (severe alerts / alert
  resolved) and the posting helpers, permission-guarded and silently no-op if
  `POST_NOTIFICATIONS` isn't granted.
- `notifications/WeatherAlertWorker.kt` — the `CoroutineWorker`. **Scoped to a saved/selected
  place only, never live device location** — this is the concrete implementation of the "avoid
  the background-location review trap" design goal below: it sidesteps
  `ACCESS_BACKGROUND_LOCATION` entirely rather than relying on an assumption about last-known-
  location access being exempt from it. A real, honest v1 limitation: a user on "current
  location" mode (the default for a fresh install) gets nothing from this yet.
- `notifications/WeatherNotificationScheduler.kt` — `schedule()`/`cancel()`, 30-minute interval
  (matches the existing forecast-cache TTL), network-connected constraint.
- Settings → "Alert Notifications" toggle (`SettingsScreen`/`SettingsViewModel`), following the
  existing Haptics/Widget Background on-off pattern, requesting `POST_NOTIFICATIONS` on API 33+
  before enabling.

**Explicitly deferred, not forgotten:** the daily digest (#3), and any move toward "current
location" background support (which would reopen the background-location question this
implementation was designed to avoid).

## What shipped (v1.1) — ongoing "current conditions" status notification

User-requested follow-up, same day: an ambient, silently-updating notification showing current
temp/condition (the "weather bubble" pattern other weather apps use), distinct from the
alert-only types above. Reuses `WeatherAlertWorker`'s existing periodic job rather than adding a
second one — no new scheduling infra.

- A third notification channel, `weather_status` (`IMPORTANCE_LOW` — no heads-up/sound on its
  ~30-minute updates, unlike the alert channels).
- `WeatherNotifier.notifyWeatherStatus()`/`cancelWeatherStatus()` — posts to a fixed notification
  ID so each periodic check updates the same notification in place instead of stacking. Built
  **persistent** (`setOngoing(true)`) per explicit user preference over a dismissible one that
  would just silently reappear anyway — turning the Settings toggle off is the only way to
  remove it, since `setOngoing` means it can't be swiped away.
- A **separate** Settings toggle ("Weather Status Notification") from "Alert Notifications" —
  someone might want the ambient status without alert pings, or vice versa — even though both
  toggles now drive the same underlying `WeatherAlertWorker`/`WeatherNotificationScheduler` job
  (`SettingsViewModel.syncScheduler()` schedules whenever either is on, cancels only when both
  are off). Same saved-place-only scoping as alert notifications, for the same
  background-location reason.
- Verified on the reporting user's real device: toggled on, confirmed a real `weather_status`
  notification posts (`dumpsys notification`), `ONGOING_EVENT|ONLY_ALERT_ONCE` flags present as
  designed.
- Real-device testing surfaced two follow-up fixes, both shipped same day (see below): a
  `WeatherNotificationScheduler` bug where toggling one of the two notification features off/on
  while the other stayed enabled silently never touched the underlying job, and a wrong
  notification icon that broke light/dark theming.

## Follow-up fixes (same day, real-device-reported)

**Scheduler bug — `ExistingPeriodicWorkPolicy.KEEP` → `REPLACE`.** `SettingsViewModel.syncScheduler()`
only calls `WeatherNotificationScheduler.cancel()` once *both* notification toggles are off, and
calls `schedule()` otherwise — including when a user flips one toggle off-and-back-on while the
other stays on. With `KEEP`, that second `schedule()` call found existing work under the same
unique name and silently discarded the "new" request, leaving the original ~30-minute countdown
completely untouched — so the "toggle it to force an immediate check" trick (used throughout this
doc's own testing, where only one toggle was ever on at a time) quietly did nothing for a user
with both enabled. `REPLACE` guarantees every `schedule()` call actually cancels-and-re-enqueues,
which is also what gives freshly-enabled work its near-immediate first run. Confirmed fixed via
live logcat on the reporting user's device: four `WeatherAlertWorker` executions fired within
seconds of four toggle taps, versus zero in ~20 minutes before the fix.

**Notification icon — full-color mipmap → monochrome vector.** All three notification types used
`R.mipmap.ic_launcher` (the full-color adaptive launcher icon) as `setSmallIcon()`. Android
notification icons are alpha-only — the system discards RGB and re-tints the icon to match the
current light/dark shade theme — so a full-color source doesn't "not theme," it renders as
whatever the OS's masking of that RGB image happens to produce, unpredictable and specifically
reported as not respecting theme. Fixed by pointing all three at the existing
`drawable/ic_launcher_foreground.xml` (the sun + speech-wave vector already used for the adaptive
launcher icon's foreground layer) — a clean single-shape vector with solid alpha edges and no
gradients, exactly what a notification icon needs; its literal fill color (`#E0B15C`) is
irrelevant since the system ignores it.

## v1.3 — background location fallback (user-requested)

User feedback: requiring an explicitly *selected* place (not just a saved one) for both
notification features read as unnecessary friction — "it should show the current location."
Implemented, with the trade-off named explicitly before building it (see the "Avoid the
background-location review trap" section above, which this directly reopens):

- `ACCESS_BACKGROUND_LOCATION` added to the manifest. `WeatherAlertWorker`'s location resolution
  now mirrors `WeatherViewModel.load()`'s exact existing foreground pattern: a selected place
  wins if set (an explicit choice to watch a specific city should never be silently overridden by
  wherever the device physically is); otherwise falls back to `LocationProvider`'s live device
  location, **only if** the background permission is granted — without it, "current location"
  mode still gets no background checks, exactly like before this change, so nothing regresses for
  a user who doesn't grant it.
- New Settings card, "Background Location" — status text plus an "Allow background location"
  button (API 29+ only; below that, ordinary foreground location already covers background
  access, so no separate grant exists to request). Requested as its own permission, never bundled
  with the ordinary foreground request, per Android's own requirement since API 30.
- **Explicitly scoped to sideload/personal testing for now.** Shipping this to Production would
  need Play Console's background-location policy declaration (historically including a
  justification video) — a real review step this app has never needed before across any of its
  15 shipped versionCodes. That's a separate decision from "does the feature work," deliberately
  not made here.

## v1.4 — dropped the selected-place fallback entirely, always current location

Same-day follow-up: user found the "selected place wins if set" priority unnecessary —
background notifications should reflect wherever the user actually is, not whichever city was
last browsed in-app. `WeatherAlertWorker` no longer reads `PreferencesStore.getSelected()` at
all; it always resolves live current location via `LocationProvider`, gated only on
`ACCESS_BACKGROUND_LOCATION`. This is a real behavior change, not just a simplification:
**Background Location is now a hard requirement**, not an enhancement — without it, both
notification features are enabled-but-inert, since there's no fallback left. Settings reordered
to put the Background Location card first, with copy updated to say "required" rather than
"optional," and both notification cards' descriptions now say "Needs Background Location above"
directly.

---

## What's already built, and what's genuinely missing

### Already there (no new data-fetching work needed)

- **Severe weather alerts** — `WeatherRepository.mapAlerts()` already fetches, dedups
  (same-event/same-area collapse), and severity-scores NWS alerts (including the Air Quality
  Alert "Code Red/Orange/Purple" inference). This is the single highest-value notification
  trigger and it's 100% already computed.
- **Alert resolution tracking** — `WeatherViewModel.trackAlertChanges()` already diffs each
  fetch's alert IDs against `PreferencesStore.getTrackedAlerts()` and detects when a previously
  active alert clears. Today this only feeds an in-app `ResolvedAlertCard`; the diff logic
  itself is exactly what a background worker would need too.
- **Daily forecast data** — high/low, `buildTips()`'s practical advice, precipitation
  probability — everything needed for a "Today: high 75°, rain likely around 3 PM" digest
  notification already exists in `WeatherData`/`DayEntry`, no new fetching required.

### Genuinely missing

- **No background execution outside the foreground app.** `WeatherViewModel`'s 30-minute refresh
  loop is a `viewModelScope.launch` coroutine — it dies the instant the ViewModel does (app
  backgrounded and eventually killed). The widget's Glance updates are OS-scheduled but only
  exist *if a widget is placed* — which, per the premise of this doc, most users won't do.
- **No `WorkManager` dependency.** Not present anywhere in `build.gradle.kts` today.
- **No notification channel, no `POST_NOTIFICATIONS` permission flow, no Settings toggle for
  it.** The last part is a small, well-precedented addition — this app already has an identical
  on/off `OptionPill` pattern for Haptic Feedback and Widget Background in `SettingsScreen`.

---

## The core decision: polling (WorkManager) vs. push (FCM)

### Polling — a periodic `WorkManager` job

Reuses `WeatherRepository.getWeather()` directly. A `PeriodicWorkRequest` wakes up on an
interval, fetches, diffs alerts against the same tracked-alert logic `WeatherViewModel` already
has, and posts a system notification instead of (or in addition to) updating a `StateFlow`. No
backend, no new vendor relationship — consistent with every architectural decision this app has
made so far.

**The real cost: Android actively fights this for infrequently-opened apps.**
- `PeriodicWorkRequest` has a **hard 15-minute minimum interval**, but that's the request, not
  the guarantee — Android's Doze mode and, more importantly, **App Standby Buckets** (since
  Android 9, tightened in later releases) throttle how often background jobs actually get to run
  based on how often the OS thinks the user opens the app. An app bucketed as "rare" — which,
  again, is precisely the population this feature exists to reach — can be restricted to as
  little as **once per day** of background job execution, not once every 15–30 minutes.
- Practically: a "severe alert within a few minutes of NWS issuing it" guarantee is not
  realistic with this approach for the users who'd benefit most. A more honest framing is
  "usually within 30–60 minutes for a reasonably active user, degrading to much coarser for a
  user who never opens the app at all" — which is a real, worth-stating irony: the delivery
  mechanism gets worse exactly as the target user gets more dependent on it.
- A foreground service would guarantee timely execution but requires a permanent, visible
  notification the whole time it runs — an intrusive, battery-costly trade most users would find
  worse than the problem it solves. Not recommended for this.

### Push — Firebase Cloud Messaging

Would solve the timing/battery problem — a push arrives near-instantly regardless of how often
the user opens the app, since the OS wakes the app specifically for it. But this requires a
**server component that doesn't exist today**: something has to poll NWS on behalf of every
subscribed user/location, detect new alerts, and fan out pushes via FCM's API. That's not a
client-side Android feature — it's a new backend service, hosting, and an ongoing operational
responsibility this solo-dev, no-backend app has never taken on. This is a materially bigger
commitment than anything in `PLAYSTORE_LAUNCH.md`'s "$25 total to ship" cost profile.

### Recommendation

Start with polling. It's the only option that doesn't require standing up a backend, and the
degraded-timing tradeoff for rarely-opened apps, while real, still beats the status quo (zero
notifications at all) for the moderately-engaged users in between "widget power user" and
"never opens the app." If real usage later shows the timing gap is unacceptable, push is the
documented escalation path — but it's a distinct, much bigger project, not a first step.

---

## Notification types, roughly in build order

### 1. Severe/extreme weather alert (Easy-Medium)
Trigger: a new NWS alert appears with `AlertSeverity.EXTREME` or `SEVERE` that wasn't in the
last-tracked set. Reuses `WeatherRepository.mapAlerts()` + the existing dedup/severity logic
directly. **US-only**, same limitation as the existing in-app feature (NWS has no non-US
coverage). Highest value, least new logic — this is the one to build first.

### 2. Alert resolved (Easy)
Trigger: a previously-tracked alert no longer appears — the exact condition
`WeatherViewModel.trackAlertChanges()` already detects. Lower urgency, but closes the same loop
`ResolvedAlertCard` already closes in-app, now for users who'd never see that card because they
don't open the app.

### 3. Daily digest (Medium)
One notification per day (user-configurable time, e.g. 7 AM) with the day's high/low and the
most relevant `WeatherTip`. Not urgent, more of an engagement/utility feature than an alert —
worth a design decision on whether this belongs in the same feature or ships separately, since
its value proposition (retention/glanceability) is different from the safety-driven urgency of
#1/#2.

### Out of scope for v1

**Hyper-local "rain starting in 15 minutes" push.** This needs genuine minute-level
precipitation nowcasting, which Open-Meteo's standard forecast (hourly granularity) doesn't
provide — the same wall `AI_ROADMAP_NEXT_VERSION.md`/`premium_widget_strategy.md` already hit
for the "hyper-local proactive alerts" proposal. It also needs push-grade timing precision (a
15-minute warning delivered 40 minutes late via a throttled background job is useless), which
per the analysis above means FCM, which means a backend. Two compounding reasons this is a
different, harder project — not a v1 candidate.

---

## Design considerations

- **Notification channels, one per type.** Android notification channels let a user mute the
  daily digest while keeping severe alerts on (or vice versa) without an in-app settings screen
  doing the work — this is the right native mechanism rather than a single on/off toggle.
- **Avoid the background-location review trap.** A background job should prefer the last
  *selected* place (`PreferencesStore.getSelected()`) or a cached last-known device location
  over requesting a fresh location fix from a background context. Android increasingly gates
  active/continuous background location behind `ACCESS_BACKGROUND_LOCATION` — a separate runtime
  permission with its own dedicated Play Console review flow (a "prominent disclosure" screen
  and, in some cases, a review video) that this app has never needed and shouldn't reach for
  here. Reading a cached last-known location from `FusedLocationProviderClient` in a background
  job generally does not trigger this requirement; actively requesting a fresh fix does.
  Worth confirming this precisely once real implementation starts, not assuming.
- **Redundancy with the in-app UI.** If the app happens to be foregrounded right when the worker
  fires, a system notification for something already visible as an `AlertBannerList` strip would
  be redundant. Low-priority to solve for v1 given the polling interval is 30+ minutes (a narrow
  window for this to actually collide), but worth a simple foreground-check guard
  (`ProcessLifecycleOwner`) if it turns out to matter in practice.
- **`POST_NOTIFICATIONS` is API 33+ only** — below that, posting to a channel the user hasn't
  disabled needs no runtime prompt at all, so the permission flow only needs to branch, not
  gate the whole feature, on `minSdk 26`.
- **Play Store impact.** `POST_NOTIFICATIONS` itself isn't a personal-data type, so it's a
  smaller Data Safety update than location was — mainly the "App content" declarations and the
  Store description picking up a new feature. Genuinely new review surface only appears if
  background location ends up being needed (see above), which the "prefer last-known location"
  approach is meant to avoid entirely.

---

## Suggested sequencing (not a commitment)

1. Add `WorkManager`, a notification channel, the `POST_NOTIFICATIONS` permission flow, and a
   Settings toggle (following the existing haptics-toggle pattern) — pure infrastructure, no
   user-visible notification yet.
2. Ship severe/extreme alert notifications (#1 above) — the highest-value, lowest-new-logic
   type, reusing existing dedup/severity code almost as-is.
3. Ship alert-resolved notifications (#2) — small addition once #2's plumbing exists.
4. Decide separately whether the daily digest (#3) belongs in this same effort or as its own
   follow-up, given its different (engagement, not safety) value proposition.
5. Revisit hyper-local nowcasting push only if/when the data-source and backend questions this
   doc defers are separately decided — not before.

---

## Open questions for the next conversation

1. Is the degraded background-execution timing for rarely-opened apps (the core tension of the
   whole polling approach) an acceptable trade for a v1, or does it undermine the feature enough
   that FCM/backend investment should be reconsidered sooner rather than later?
2. Should the daily digest ship alongside severe alerts, or later as its own decision — they
   have genuinely different goals (safety vs. engagement) and maybe different opt-in defaults?
3. What's the actual default polling interval worth targeting — the existing 30-minute forecast
   cache TTL is a natural anchor, but is that too coarse for a "severe alert" use case even
   before Doze throttling makes it worse?
4. Worth a lightweight real-device experiment first — schedule a `PeriodicWorkRequest` at 15–30
   min and just log actual fire times over a day or two on a real phone under normal use — to
   get real numbers instead of reasoning from Android's documented (but not always representative)
   Doze/Standby-Bucket behavior?

## v1.5 — toggle race fix + proactive missing-permission prompt

Two more real-device-reported issues, same day:

- **Toggle race condition.** Both Settings toggles gated `setXEnabled(true)` behind the async
  `POST_NOTIFICATIONS` permission launcher's callback (via a shared `pendingNotificationEnable`
  closure) — user-reported that enabling "Weather Status Notification" sometimes silently did
  nothing on the first tap, only working after an off/on retry. Fixed by decoupling entirely:
  tapping "On" now calls `setXEnabled(true)` immediately and unconditionally, and independently
  fires the permission request only if not already granted — the toggle's visible state no longer
  depends on that request's result. `WeatherNotifier` already no-ops posting until permission is
  actually granted, so "enabled but not yet permitted" is an honest, harmless intermediate state
  rather than something to prevent.
- **Silent "enabled but broken" gap.** Since v1.4 dropped the saved-place fallback, both
  notification features now hard-require `ACCESS_BACKGROUND_LOCATION`. A user can flip a toggle
  on in Settings, skip (or later revoke) that separate permission grant, and the feature just goes
  quiet — no crash, no error, nothing, since Settings' own explanatory copy only reaches someone
  who happens to scroll back there. Not a legacy-migration concern (this app has never shipped
  notifications in a published release, so there's no existing install base with a toggle already
  on) — just a general "enabled but not actually working" catch. Fixed with a dialog on
  `WeatherScreen` (the main/landing screen, reachable without ever revisiting Settings),
  re-evaluated on every resume rather than shown once: detects "a notification toggle is on AND
  Background Location isn't granted" and offers a direct "Grant Access" button. Re-checking every
  resume (not just once per session) also catches a user who revokes the permission later via
  system Settings, not just first-time setup.

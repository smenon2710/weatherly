# SkySpeak — Next Version: Premium Visuals, AI Micro-Interactions & Proactive Alerts (Discussion Doc)

> Not a decision, not a plan — a starting point for a conversation. Ten proposals were raised in one batch (generative UI/mesh gradients, particle physics + gyroscope, fluid shared-element transitions, adaptive haptics, an ambient "glanceable AI" ring, a voice interface, per-user lifestyle context, hyper-local proactive alerts, multi-source data, and home/lock-screen widgets). Each was checked against what's actually already in the codebase before being triaged, the same way `AI_ROADMAP_NEXT_VERSION.md` checked the WeatherNext proposal against what Open-Meteo actually offers rather than reasoning from the pitch alone.

---

## TL;DR

- **Several of these are already substantially built.** The home-screen widget, the live per-condition particle background, and the "proactive one-sentence summary" text are all real, shipped systems (`WeatherWidget`, `WeatherBackground.kt`'s scene renderers, `WeatherRepository.buildUpcomingHeadline()`). Framing them as "add this feature" undersells the work already done and overstates the lift to extend them.
- **One proposal (glassmorphism/frosted cards) was already tried and reverted, for a documented reason.** `CLAUDE.md`'s `GlassCard` section explains a translucent card fill over the animated background produced visibly patchy, seamed cards — confirmed via a real device screenshot, worse in light mode. What wasn't tried is *true* `RenderEffect` blur (API 31+) rather than plain alpha-blending — that's a real, open, unverified alternative, not a repeat of the same failure, but it needs its own spike before assuming it'll fare better.
- **Two proposals (hyper-local proactive alerts, multi-source data) hit the same underlying wall:** genuine minute-level nowcasting and a second forecast provider both need a paid/non-Open-Meteo data source, which reopens the exact licensing conversation already logged in `premium_widget_strategy.md` — "if the app becomes paid, Open-Meteo's free non-commercial tier no longer applies." These aren't just engineering tickets; they're data-sourcing and monetization decisions first.
- **`minSdk = 26` is a recurring constraint**, not a one-off: AGSL runtime shaders need API 33+, `RenderEffect` blur needs API 31+, and on-device speech recognition needs API 31+. Every item that touches these needs an explicit fallback story for API 26–30/32, not just a "does it compile" check.
- **Semantic Lifestyle Context (proposal #7) was considered and dropped** — see "Considered and dropped" below. Not a technical concern; a liability one, consistent with a guardrail the app already enforces elsewhere.

---

## Already covered — reframe before building

These three don't need new engineering so much as a decision about scope, because the substance already exists.

### Home-screen widgets (part of proposal #10)

**What's proposed:** "Beautiful, Material You-themed widgets... immediate weather status, severe warning flags, and a quick-action prompt to launch the AI."

**What's already there:** `WeatherWidget` (Jetpack Glance) already ships all of this — six responsive breakpoints (`SMALL`/`MEDIUM`/`TALL`/`WIDE`/`LARGE`/`XLARGE`), chrono-dynamic content (morning/daytime/night focus), Material You dynamic colors via `resolveWidgetColors()`, an `AlertIndicator` for active NWS alerts, real vector glyph icons, stale-while-revalidate rendering, and a manual refresh button. This is one of the most mature subsystems in the app (see `CLAUDE.md`'s "Widget" section — multiple full release cycles of real-device bug fixes already went into it). There's genuinely very little left to add here beyond a "launch the AI chat" quick-action tap target, which is a small, bounded addition to an existing, working system.

### Lock-screen widgets (the other half of proposal #10)

**Reality check:** Android phones don't have a general public "lock-screen widget" surface — that capability was removed after Android 4.x and never returned (Wear OS has lock-screen-style complications; phones don't). This isn't a build-it-later item, it's a platform gap. The closest phone-side equivalent is a persistent/ongoing `Notification` showing current conditions, which is a different feature with its own permission (`POST_NOTIFICATIONS` on API 33+) and UX implications (an always-there notification is a much more invasive ask than a widget the user chose to place). Worth clarifying what's actually wanted before scoping anything under this name.

### Multi-source data — the NWS/NOAA half (part of proposal #9)

**What's proposed:** "Integrate additional APIs like Apple WeatherKit or NOAA alongside the existing Open-Meteo foundation."

**What's already there:** NOAA is *already* integrated, twice over — NWS active weather alerts (`api.weather.gov`, `WeatherRepository.mapAlerts()`) and NOAA CO-OPS tide predictions (`api.tidesandcurrents.noaa.gov`, `TideStations`/`mapTide()`), both free, both US-government APIs, both already shipped and documented at length in `CLAUDE.md`. Apple WeatherKit specifically is a poor fit for an Android app — it's Apple's own consumer-platform API, requires an Apple Developer account ($99/yr), and calling it cross-platform from Android is an unusual, awkward integration rather than a natural one. The part of this proposal that's genuinely new is covered under **Complex** below, because it's really a different provider (like Tomorrow.io) plus a real abstraction layer, not "add WeatherKit."

---

## Considered and dropped

### Semantic Lifestyle Context (proposal #7) — dropped, 2026-09-04

**What was proposed:** the AI learns personal routines — "perfect running weather," "good gardening conditions" — and proactively tells the user their custom conditions are met.

**Why dropped:** a liability call, not a technical one. This app already has an explicit guardrail against exactly this kind of confident, unsolicited safety/suitability claim — `ChatRepository.systemPrompt()`'s safety/liability rule instructs the model to avoid definitive-sounding statements for driving/hiking-type questions, framing things as "no active advisories, but conditions can change" rather than an outright "it's safe" (see `CLAUDE.md`'s "AI chat: local-first routing, guardrails, and usage cap" section). A proactive "good running weather right now" notification is a *more* confident, *more* prominent version of the exact claim that rule exists to soften — surfaced without being asked, not tucked into a chat reply the user requested. If a user relies on it and gets caught in conditions the app's coarse per-activity thresholds didn't anticipate (a storm cell it didn't model, an individual health sensitivity it can't know about), that's a real trust and liability exposure this app has deliberately avoided taking on elsewhere. Not worth reopening that principle for one feature.

This doesn't affect the **Glanceable AI ring (#5 below)** — it stands on its own using only the existing generic anomaly/headline logic (`buildUpcomingHeadline()`), with no per-activity personalization or endorsement language involved.

---

## Easy — small, self-contained, low risk

### 1. Adaptive Haptic Feedback (proposal #4) — ✅ IMPLEMENTED (2026-09-04)

**Shipped.** See `IMPROVEMENTS.md`'s "Completed — Adaptive Haptic Feedback (2026-09-04)" entry and `CLAUDE.md`'s "Haptic feedback" section for full detail. Summary: `util/WeatherHaptics.kt` fires one restrained pulse on a foreground forecast load for a severe alert, thunderstorm, or heavy rain/snow (reusing `wmoText()`'s existing WMO categorization) — never on a silent background/periodic refresh. Toggleable in Settings → Haptic Feedback, default on. `assembleDebug`/`test` (38/38)/`lint` all pass; **not yet verified on a real device** — pattern timings are a first pass, worth a real on-device feel-check before calling this fully done.

Left below as the original plan/rationale.

Native `Vibrator`/`VibratorManager` APIs, `VibrationEffect.createWaveform()` for an irregular "rumble" pattern mapped to alert severity, or a light repeating click pattern mapped to precipitation intensity. No new dependency; only needs the existing `VIBRATE` permission (normal, no runtime prompt). Should respect the system's haptic-feedback toggle rather than always firing. Genuinely small and self-contained — a good first pick if the goal is a quick, low-risk win before the bigger items.

---

## Medium — real, scoped feature work

### 3. Gyroscope-Responsive Particles (the new half of proposal #2)

**What's proposed:** rain/snow particles that drift based on how the user tilts the phone.

**What's already there:** the live particle simulation itself is not new — `WeatherBackground.kt` already runs a full per-condition particle system (`drawRain`, `drawSnow`, `drawSleet`, `drawHail`, `drawWindStreaks`, and more, 26 `Scene` values in total) driven by real wind/precipitation data on a shared `timeMs` clock (`CLAUDE.md`'s `WeatherBackground` section covers this in detail). The genuinely new piece is a `SensorManager` listener (accelerometer or gyroscope) feeding a tilt value into the existing per-particle position math, lifecycle-scoped so the listener registers/unregisters correctly with the composable. This is a scoped addition on top of a mature system, not a rebuild — the risk is mostly in getting sensor lifecycle and battery impact right, not in the visual effect itself.

### 4. Glanceable AI Ring + Proactive Summary (proposal #5)

**What's proposed:** an ambient, pulsing AI indicator around the current temperature that expands into a one-sentence proactive summary when it detects something worth flagging.

**What's already there:** the text-generation half of this already exists. `WeatherRepository.buildUpcomingHeadline()` already scans the next 12 hours of real hourly data for significant condition changes and produces exactly this style of sentence (e.g. the existing lookahead pill under the hero — see `CLAUDE.md`'s "Lookahead pill" section). The genuinely new work is UI: a pulsing ring affordance around the temperature, and an expand-to-bottom-sheet interaction on tap. This is real, visible feature work — new animation, a new interaction pattern — but it's UI/animation work layered on an existing data pipeline, not a new intelligence system. "Forecast anomaly" detection (e.g. a sudden pressure drop) would need a small addition to that pipeline (pressure-delta check), which is a bounded, well-scoped addition given `WeatherData.pressure` and the day/hour blocks already exist.

**Scope note:** this stands alone without any per-activity personalization — see "Considered and dropped" above for why lifestyle-context thresholds specifically were ruled out. Keep this to generic, data-backed observations ("wind picking up after 3 PM," "rain holding off until this evening") rather than any suitability/safety-flavored phrasing, consistent with the same guardrail principle.

### 5. Fluid Layout Transitions (proposal #3)

**What's proposed:** tapping a day in the 7-day forecast expands that row into a full detail view via a physically continuous transition, instead of a sheet snapping open.

**Current state:** tapping a `DailyCard` row today opens `DetailSheet.Day` as a `ModalBottomSheet` (see `CLAUDE.md`'s "Detail sheet system" section) — not a shared-element expansion. Compose's `SharedTransitionLayout` is a real, usable API in current Compose releases and would let that specific row expand in place rather than a sheet sliding up. This is a genuine, bounded redesign of one interaction (the day-row → detail-day path), not an app-wide navigation overhaul — the other detail sheets (metrics, alerts) aren't in scope for this unless a decision is made to extend the same treatment everywhere.

### 6. Conversational Voice Interface (proposal #6)

**What's proposed:** hands-free voice queries, leaning into the "Speak" half of the SkySpeak name.

**Why Medium, not Easy:** the plumbing is all native (`SpeechRecognizer`/`RecognizerIntent` for speech-to-text, `TextToSpeech` for spoken replies), and recognized text can feed directly into the existing `ChatViewModel.send()` pipeline — no new chat architecture needed. But three real design points push it past "easy": (a) on-device speech recognition needs API 31+ (`SpeechRecognizer.createOnDeviceSpeechRecognizer()`), so a fallback path — or an explicit "needs internet" caveat — is needed below that; (b) TTS wants a complete sentence to speak, which doesn't pair naturally with the existing token-by-token SSE streaming in `ChatRepository.askStreaming()` — the simplest fix is buffering the full reply before speaking, a real (if small) design compromise on the existing streaming UX; (c) `RECORD_AUDIO` is a new, user-visible privacy surface requiring an update to the Data Safety form and `docs/privacy.html`, not just a manifest permission.

---

## Complex — needs a spike or a decision before scoping real work

### 7. Generative UI: Mesh Gradients (AGSL) + Glassmorphism Cards (proposal #1)

These two are bundled in the original proposal and share the same underlying surface (`WeatherBackground` + `GlassCard`), but they carry very different risk:

- **AGSL mesh-gradient shaders** — `RuntimeShader` requires API 33+; below that there's no equivalent, so this needs an explicit "static/simpler gradient on older devices" fallback, not just a compile-time guard. This is real, novel GPU-shader work layered on top of the *existing* `conditionGradient`/`WeatherBackground` system (which already does real-data-driven color and scene selection) — it's an enhancement of a working system, but a nontrivial one given the API floor.
- **Glassmorphism cards — already tried, already reverted, for a documented reason.** `CLAUDE.md`'s `GlassCard` section is explicit: a translucent `surface`-alpha card fill over the animated background was tried and reverted after producing visibly patchy, seamed cards on a real device — worse in light mode. **What was tried was alpha-blending, not true `RenderEffect` blur** (API 31+, real Gaussian blur rather than transparency) — that's a genuinely different, unverified approach, not a repeat of the same failed experiment. But given there's already a documented on-device failure here, this specifically needs a quick real-device spike with actual `RenderEffect` before any real investment, rather than assuming a second attempt will succeed.

### 8. Data Source Expansion: Multi-Source Integration + Hyper-Local Proactive Alerts (proposals #8 + #9's non-NOAA half)

These two are combined here because they hit the identical wall:

- **Hyper-local "rain in 15 minutes" alerts (#8)** need genuine minute-level precipitation nowcasting. Open-Meteo's standard forecast is hourly, not minute-by-minute — there's no free, non-commercial path to true 15-minute-granularity nowcasting with the app's current data source. This is the same paid-provider question `premium_widget_strategy.md` already raised for its Tomorrow.io "premium pipeline" idea.
- **A second/alternate forecast provider for accuracy or user choice (#9, non-NOAA half)** — building a real provider-abstraction layer is legitimate architecture work (`WeatherRepository` is currently tightly coupled to Open-Meteo's response models throughout), but every realistic second source (Tomorrow.io, a paid nowcasting API) is not free at the volume/granularity this would need, which **reopens the Open-Meteo licensing question already on record**: "if the app becomes paid or subscription-based, Open-Meteo's free tier terms no longer apply." So this isn't purely an engineering decision — it's a monetization/business-model decision first (does the app start charging, or license Open-Meteo commercially at $29/month, or accept a much coarser hourly-only version of "hyper-local"), and the engineering only makes sense once that's answered.

Separately, #8 also needs infrastructure the app doesn't have at all today: **no `WorkManager` dependency exists in this project** — the widget's periodic updates run through Glance's own OS-scheduled update mechanism, not WorkManager — and there's no local-notification system either. Both are real, non-trivial additions (Doze-mode/battery-life tradeoffs for background work, a new `POST_NOTIFICATIONS` permission and Data Safety disclosure for notifications) independent of the data-source question above.

**Recommendation:** treat both of these as a business decision to make first (does hyper-local/multi-source pursuit justify moving off free Open-Meteo), not as features to scope directly — the engineering path is straightforward once that's answered, and premature otherwise.

---

## Suggested sequencing (not a commitment)

1. ~~**Easy tier first** (#4 haptics)~~ — done, see above.
2. **Medium tier next**, roughly in this order: #3 gyroscope tilt (smallest, extends a system that already works) → #4 (this doc's) AI ring (visible, builds on existing headline logic) → #5 fluid transitions (bounded UI redesign) → #6 voice (most design decisions to resolve first).
3. **Complex tier — spike before committing**: a short on-device experiment with real `RenderEffect` blur (not alpha-blending) for glassmorphism, and a short AGSL shader prototype gated to API 33+, before deciding whether #1 is worth the fallback-path investment. For #8/#9, the open question is the licensing/monetization decision, not a technical spike — that conversation should happen before any code.

---

## Open questions for the next conversation

1. For the lock-screen half of #10 — is a persistent/ongoing notification actually what's wanted, given phones don't have a real lock-screen-widget surface? Worth confirming before it's scoped as anything.
2. For #1's glassmorphism half — worth a quick real-device spike with `RenderEffect` blur specifically, or is this shelved given the prior documented failure?
3. For #8/#9 — is moving off Open-Meteo's free non-commercial tier (to get real minute-level nowcasting or a second provider) actually on the table? This is the same monetization conversation `premium_widget_strategy.md` and `AI_ROADMAP_NEXT_VERSION.md` both already opened and left unresolved — worth deciding once rather than three separate times across three docs.
4. Does #6 (voice) still make sense to prioritize alongside `AI_ROADMAP_NEXT_VERSION.md`'s open question about whether free-form AI chat should be *scaled back* rather than expanded? Building a voice interface for a chat feature whose future is itself undecided is worth sequencing deliberately.
5. Any of the Medium-tier items you'd want scoped into an actual implementation plan first, or do you want to sit with this grouping for a bit before picking?

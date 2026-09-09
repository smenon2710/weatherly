# SkySpeak — User Feedback, 2026-09-09 (To Discuss)

> Raw feedback received via email, captured here for a later discussion. Not a decision, not
> started — just a record plus a quick fact-check against what's actually already in the
> codebase, so the discussion starts from current reality rather than assumptions either way.

---

## Feedback (verbatim)

> Subject: Feedback on SkySpeak — Loving the ad-free UI!
>
> To help improve future iterations, I wanted to share a few constructive thoughts:
>
> **Offline Caching:** Consider caching the main 7-day view so the app remains readable when
> users lose cellular service.
>
> **Chat UI Shortcuts:** Adding rapid "tap-to-ask" buttons (like "Should I pack a coat?") would
> make the AI assistant much faster to use while walking.
>
> **Safety Guardrails:** Ensuring the AI triggers explicit weather safety warnings during extreme
> UV or freeze indexes would protect users from model text hallucinations.

---

## Quick fact-check against the current codebase

### 1. Offline Caching — appears to already be built

`ForecastCache` (`data/prefs/ForecastCache.kt`) persists the last successful `WeatherData` —
which includes the full `daily: List<DayEntry>` 7-day forecast, not just the current-conditions
hero — as JSON in `SharedPreferences`. `WeatherViewModel.init` loads it synchronously on cold
start, so the app never opens to a blank screen offline, and the 7-day list specifically renders
from this same cached object like everything else on the screen. Worth clarifying with the
reporter what they actually saw — a first-ever launch with no successful fetch yet has nothing to
cache, or something screen-specific broke — rather than assuming the general capability is
missing, since it reads as already present.

### 2. Chat UI Shortcuts — appears to already be built

`ChatScreen` already has six always-visible quick-suggestion chips (Umbrella, Jacket, Walking,
Driving, Hiking, Clothing), answered instantly on-device via `WeatherAdvisor` — no network call,
no cost, no latency. The feedback's own example ("Should I pack a coat?") maps directly onto the
existing "Jacket" chip. Same open question as above: worth finding out whether the reporter
tested an older build, missed the chips in the UI, or hit some other specific gap — because as
described, this already exists close to verbatim.

### 3. Safety Guardrails — genuinely not built as described

This one's real. Today, `ChatRepository.systemPrompt()`'s existing safety rule actually points
the *opposite* direction: it tells the model to avoid definitive-sounding safety claims and
frame things as "no active advisories, but conditions can change" — a guardrail against
overconfidence, not a trigger for explicit warnings. Nothing currently forces a deterministic,
model-independent warning at extreme UV or cold thresholds; the closest existing signal is soft
(`WeatherAdvisor`'s `hiking()`/`walking()` mentioning UV ≥ 8, `buildDayOutlookTips()`'s `veryHot`
tier) — advisory copy, not a guaranteed, hallucination-proof safety banner.

Worth noting before discussing further: `PREMIUM_UX_ROADMAP.md` already has a "Considered and
dropped" entry for a related idea (proactive, personalized "good running weather" claims),
rejected specifically over liability/overconfidence risk. This feedback's ask is arguably a
different shape — a deterministic, rule-based (non-LLM) warning at genuinely extreme readings,
which is *more* conservative than what was dropped, not a repeat of it — but that precedent is
directly relevant context for the discussion, not a reason to dismiss this one out of hand.

**Addressed, 2026-09-09.** Extended `WeatherRepository.buildDayInsights()` — the deterministic,
non-LLM insight generator behind the hero pill/`DetailSheet.Forecast` shipped for the "day
summary" request — with two new safety-tier checks, ranked above the softer whole-day context
(ordinary "very high" UV, air quality, temperature swing) already in that list:
- **Extreme UV** (uvNow ≥ 11, `uvLabel()`'s own "Extreme" band, distinct from the existing "Very
  High" ≥ 8 check it supersedes rather than duplicating): "UV is at an extreme level right now —
  limit sun exposure, seek shade, and wear sunscreen and protective clothing."
- **Freeze warning** (current temp or ~6h-out temp ≤ 0°C/32°F — a real Freeze-Warning-style
  absolute threshold, distinct from `buildTips()`'s softer relative "cold, wear a jacket" tip):
  "Freezing temperatures — dress warmly, watch for icy surfaces, and limit time outside if you
  can."

Both are plain, deterministic sentences from real thresholds — not LLM output — directly
answering the original ask ("protect users from model text hallucinations"). Ranked below an
actually-arriving storm/high-wind event (the more immediate hazard) but above everything else in
the list. Built and installed for testing.

**Verification, 2026-09-09:**
- **Freeze warning — confirmed live.** Ushuaia, Argentina (currently -1.2°C) triggers it
  immediately, no waiting on a specific hour needed.
- **Extreme UV — not verified; logic reviewed, no real-world trigger found today.** Checked
  ~20 locations worldwide — the best real candidates for UV 11+ (equatorial + very high altitude:
  Quito, Cayambe, Chacaltaya, Sajama, El Tatio, Mount Kenya, Kampala, Bujumbura, and more),
  including each location's *clear-sky theoretical maximum* (removing cloud interference
  entirely) via Open-Meteo's `uv_index_clear_sky` field, not just actual cloud-affected readings.
  Every one topped out around 10.0–10.25 today, genuinely short of 11 — not a search failure or
  bad luck, but a real seasonal fact: today (near the September equinox) just isn't the right time
  of year for any location on Earth to hit "Extreme" UV, even at the best-known high-altitude
  equatorial sites. Sajama, Bolivia (9.8, 0% cloud) was closest. Worth retrying in a few months
  once a Southern Hemisphere high-altitude site like Sajama is closer to its December solstice.

---

## Open questions for the discussion

1. For #1/#2: confirm with the reporter what build/scenario they actually hit, since both read
   as already-shipped features on inspection — worth ruling out "already fixed" before treating
   either as new work.
2. For #3: does a deterministic, threshold-triggered safety banner (bypassing the LLM entirely
   for genuinely extreme UV/cold readings) fit this app's existing risk posture, or does it reopen
   the same liability conversation `PREMIUM_UX_ROADMAP.md` already had for a related but distinct
   idea?
3. If #3 moves forward, where would the threshold logic live — extend `WeatherAdvisor`'s existing
   UV/cold checks into an explicit non-chat surface (e.g., a hero banner, like `TipBanner` but for
   safety), or scope it to the chat context specifically per the original ask?

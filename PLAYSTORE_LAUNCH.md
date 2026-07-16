# Weatherly — Play Store Launch Guide

> Solo developer, cost-efficient path.

---

## Cost Summary

| Item | Cost |
|---|---|
| Google Play Console registration | **$25 one-time** |
| Open-Meteo (weather data) | **Free** — non-commercial only (see Monetization section) |
| Privacy policy hosting (GitHub Pages) | **Free** |
| OpenRouter API | **Free tier** — user-provided key, costs you $0 |
| Backend / server | **None needed** — pure client app |

**Minimum to ship: $25.**

---

## Blocking — Must Fix Before Google Accepts the App

### 1. Change the Application ID

`com.example.*` is flagged by Google as a test/example package and will be rejected.

In `app/build.gradle.kts`:
```kotlin
applicationId = "com.sujith.weatherly"   // or io.github.yourhandle.weatherly
```

> **Warning:** Once published you can never change the application ID. Pick something that represents you.

---

### 2. Privacy Policy (mandatory — app uses location)

`ACCESS_FINE_LOCATION` + data sent to Open-Meteo and optionally OpenRouter requires a hosted privacy policy. Google blocks the listing without one.

**Free approach:**
1. Generate a policy at [privacypolicygenerator.info](https://www.privacypolicygenerator.info).
2. Create `docs/privacy.html` in your repo, enable GitHub Pages → becomes `https://yourusername.github.io/weatherly/privacy`. Free.
3. Paste the URL into Play Console → Store listing → Privacy policy.

**Constraint discovered 2026-07-13: this repo must stay public.** GitHub Pages does not serve sites from private repositories on the Free plan — briefly making this repo private (for pre-launch confidentiality) took the privacy policy URL offline while it was live in the Play Console closed-testing listing, and the Pages configuration itself was cleared (not just paused), requiring a manual re-creation via the API (`POST /repos/{owner}/{repo}/pages`) after reverting back to public. **Do not flip this repo to private again while the privacy policy URL is referenced in Play Console**, unless one of these is done first: (a) split `docs/privacy.html` into its own small dedicated public repo and update the Play Console privacy policy URL to point there, or (b) upgrade to GitHub Pro (~$4/month), which allows Pages to serve from private repos.

---

### 3. Release Signing Keystore

> **Critical:** Back up the `.jks` file and passwords somewhere safe (e.g. a password manager). If you lose the keystore you can never push updates to the same listing.

**Generate keystore:**
```bash
keytool -genkey -v -keystore weatherly-release.jks \
  -alias weatherly -keyalg RSA -keysize 2048 -validity 10000
```

**Wire into `app/build.gradle.kts`:**
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../weatherly-release.jks")
        storePassword = localProps.getProperty("STORE_PASSWORD")
        keyAlias = "weatherly"
        keyPassword = localProps.getProperty("KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

Add passwords to `local.properties` (already gitignored):
```
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

---

### 4. Data Safety Form (Play Console)

**⚠️ Drifted from the live Console submission (found 2026-07-16, not yet re-submitted):** the checklist below marks this form as already filled out, but that submission predates the NWS weather-alerts feature (`api.weather.gov`), which also now receives location coordinates on every fetch. The table below and `docs/privacy.html` have been corrected to reflect actual current app behavior — **the live Play Console Data Safety form itself still needs to be manually updated to match** (this file and the hosted policy are documentation, not the actual submission; only re-editing the form in Console fixes the real compliance gap).

Fill out in Play Console → Store listing → Data safety. Summary (see the detailed draft answers below for the full form flow):

| Data type | Collected | Shared with third parties | Purpose |
|---|---|---|---|
| Approximate location | Yes | Yes (Open-Meteo, to fetch the forecast; National Weather Service, to check for active weather alerts — U.S. locations only) | App functionality |
| Precise location | Yes | Yes (Open-Meteo, to fetch the forecast; National Weather Service, to check for active weather alerts — U.S. locations only) | App functionality |
| User queries (chat) | Optional | Yes (OpenRouter), only when a key is configured | App functionality |

*(Corrected from an earlier draft: location **is** shared with Open-Meteo — the app has no backend, so coordinates go directly from device to Open-Meteo's API. This matches `docs/privacy.html`. Updated again 2026-07-16 to add the National Weather Service as a third recipient of location data, added when the weather-alerts feature shipped.)*

<details>
<summary>Detailed draft answers, matching Play Console's actual form flow (expand)</summary>

**Note:** Google's exact wording/categories shift between Console versions — treat this as a starting draft to copy from, not a guarantee it matches pixel-for-pixel. Cross-check against the live form before submitting.

**Top-level questions:**
- *Does your app collect or share any of the required user data types?* → **Yes**
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (every API call — Open-Meteo, OpenRouter, RainViewer/OSM tiles, National Weather Service — goes over HTTPS)
- *Do you provide a way for users to request that their data be deleted?* → **No account exists, so there's no server-side data tied to a user identity to delete.** All app data (cache, preferences, on-device API key) lives in local app storage and is removed on uninstall. If Console requires an affirmative answer here, note in the form that data deletion is handled via app uninstall since no account/backend exists.

**Per data type:**

| Data type | Collected? | Shared? | Purpose | Optional? | Ephemeral? |
|---|---|---|---|---|---|
| Approximate location | Yes | Yes — Open-Meteo, and National Weather Service (U.S. locations only, for active weather alerts) | App functionality | No (core feature; app is unusable without it unless a manual city is picked) | Not claimed — don't check "ephemeral" unless you've confirmed Open-Meteo's/NWS's own retention policy |
| Precise location | Yes | Yes — Open-Meteo, and National Weather Service (U.S. locations only, for active weather alerts) | App functionality | No | Same as above |
| Other user-generated content *(free-form chat text)* | Yes, only if the user opens AI chat and a key is configured | Yes — OpenRouter (and whichever model OpenRouter routes to) | App functionality | Yes — the quick-suggest chips work without any chat text ever being sent | No |

**Data types with no data to declare (leave unchecked):** Personal info (name/email/etc. — never collected), Financial info (the donation link opens an external Razorpay page in the browser; the app itself never collects or processes payment details), Health & fitness, Photos/videos, Audio, Contacts, Calendar, Messages (SMS/email), Web browsing history, Device/other IDs, App info & performance (no analytics or crash-reporting SDK in the codebase).

</details>

---

### 4a. Remaining "App content" Declarations (Play Console)

The rest of the items under Play Console → Policy → App content, beyond Data safety and Content rating above. Same caveat as the Data Safety draft: Google's exact wording shifts between Console versions — cross-check against the live form.

| Section | Answer |
|---|---|
| Set privacy policy | `https://smenon2710.github.io/weatherly/privacy.html` |
| Sign-in details | No — app doesn't require sign-in (no account system exists anywhere in the codebase) |
| Ads | No — app doesn't contain ads (no ad SDK anywhere in the project) |
| Target audience | 18 and over only; explicitly **not** designed for or targeting children under 13 — keeps the app out of Google Play Families Policy, which this app isn't built to comply with (no COPPA handling, collects location) |
| Government apps | No |
| Financial features | No — the donation link opens an external Razorpay page in the device browser; the app itself never handles payment details, loans, crypto, or in-app transactions |
| Health | No |
| Category | Weather |
| Contact details | Email: developer's contact address (see account details); phone/website optional |

**Content rating questionnaire** (separate flow, IARC-based):
- Category: Utility/Productivity/Reference — not Games
- Violence, sexual content, profanity, controlled substances, gambling: None
- User-generated content shared with other users: No — chat text is private per-user, sent only to OpenRouter for processing, never shown to or shared with other app users
- Shares user's location: Yes — with third parties (Open-Meteo; National Weather Service for U.S. locations) for app functionality, not for social/advertising purposes
- Expected outcome: **Everyone**

---

## Store Listing Content (Draft)

Character counts verified — safe to paste directly into Play Console.

**App title** (30 char limit): `SkySpeak: Premium Weather Chat` — exactly 30 characters.

**Short description** (80 char limit, 78 used):
```
Ad-free forecasts, live radar, and an AI weather assistant. No account needed.
```

**Full description** (4000 char limit, ~2230 used):
```
SkySpeak is a clean, ad-free weather app built for people who just want accurate forecasts without the clutter — plus an AI assistant for the planning questions a forecast alone can't answer.

WHAT YOU GET
• Current conditions, next 24 hours, and a 7-day forecast in one glance
• A hero display that visually shifts with the sky — clear blue, rain slate, thunder indigo, snow white-blue — so conditions read at a glance
• Live precipitation radar with play/pause and a frame scrubber
• Air quality, pressure, visibility, wind, humidity, sunrise/sunset, and moon phase — all in one screen
• Official National Weather Service advisories — severe warnings, watches, and air quality alerts — shown clearly at a glance (U.S. locations)
• A home-screen widget that adapts its layout to size and time of day
• Works offline: your last forecast is cached, so the app never opens to a blank screen

AI WEATHER ASSISTANT
Quick-tap questions like "Do I need an umbrella?" or "Should I wear a jacket?" are answered instantly, on-device, using your actual forecast — no account, no network call, no cost. For more open-ended planning questions — "Best day this week for a long run?", "What should I pack for a weekend trip?" — the assistant uses your forecast as context to give a real answer, not just raw numbers.

PRIVACY BY DESIGN
• No account or sign-up required
• No ads, no ad SDK, no tracking for advertising purposes
• No analytics or crash-reporting SDKs
• Your location is used only to fetch your forecast — never sold or shared for marketing
• Full privacy policy available in-app and on our website

ABOUT THE DATA
Forecasts come from Open-Meteo, a free and open weather data provider, attributed at the bottom of the weather screen. The precipitation radar uses OpenStreetMap tiles with a RainViewer overlay. Weather advisories come from the National Weather Service, the official U.S. government forecasting agency.

SkySpeak is free to use with no paywalled features. If you find it useful, an optional "Support the developer" link is available — entirely optional, never required.

Whether you're deciding what to wear this morning or planning a weekend outdoors, SkySpeak gives you the forecast and the judgment to go with it.
```

**Category:** Weather

---

## Small Fixes Before Submitting

### 5. Move Logging Interceptor to Debug-Only

`logging-interceptor` should not be in release builds — it can leak network traffic and keys.

In `app/build.gradle.kts`:
```kotlin
// before
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// after
debugImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

**Important:** a runtime `if (BuildConfig.DEBUG) { ... }` guard around the interceptor setup is *not* enough — the `okhttp3.logging.HttpLoggingInterceptor` import itself still needs to resolve at compile time, and it won't exist on the release classpath once the dependency is `debugImplementation`-only. This breaks `bundleRelease`/`assembleRelease` with an "Unresolved reference" error. The actual fix is a `src/debug` / `src/release` source-set split — see `NetworkModule`'s `addDebugLogging()` extension in `data/remote/DebugLogging.kt` (debug: adds the interceptor; release: no-op, never imports the class at all).

---

### 6. Build as AAB, Not APK

Google requires Android App Bundles (`.aab`) for all new app submissions.

In Android Studio: **Build → Generate Signed Bundle / APK → Android App Bundle → select release signing config → Finish.**

---

## Store Listing Assets (one-time effort)

| Asset | Spec | Free tool |
|---|---|---|
| App icon | 512×512 PNG | ✅ Done — `store_assets/ic_launcher_512.png`, rendered from the same vector design as the adaptive launcher icon |
| Feature graphic | 1024×500 PNG | ✅ Done — `store_assets/feature_graphic.png` |
| Phone screenshots | 2–8, min 1080px tall, ≤2:1 aspect ratio, no alpha | ✅ Done — `store_assets/screenshot-0{1,2,3}-*.png`, 1280×2560 (exact 2:1), alpha flattened |
| Short description | ≤80 characters | — |
| Full description | ≤4000 characters | — |
| Category | Select "Weather" | — |
| Content rating | Questionnaire in Console | Will be "Everyone" |

**Screenshot tip:** Run on a Pixel 6 emulator, take screenshots via the emulator toolbar, pull with `adb pull /sdcard/Pictures/`.

---

## Monetization & Open-Meteo Licensing (Critical)

Open-Meteo's free tier is **non-commercial use only**. If you charge for the app, run ads, or accept in-app purchases, you must buy a commercial API plan starting at **$29/month**.

**Decision: Option A (donation model).** The app stays free with no paywalled features, so it remains within Open-Meteo's non-commercial terms. A "☕ Support the developer" link was added to `AttributionFooter` (`ui/components/WeatherComponents.kt`), pointing to a hosted Razorpay Payment Page rather than the in-app Checkout SDK — no backend or API keys were introduced into the client. The destination link itself is not duplicated in documentation; it lives only in the app source.

### Option A — Donation model (stay free, $0/month)
List the app for free, add a donation link (Ko-Fi, Buy Me a Coffee, or a Razorpay Payment Page) in the app. Because no features are paywalled and the app remains free, you stay within Open-Meteo's non-commercial terms. Recommended for a solo launch.

### Option B — Paid download ($0.99–$1.49 one-time)
Charge a one-time fee. You must buy the $29/month Open-Meteo commercial plan. After Google's 15% cut you need roughly **30–40 new downloads per month just to break even** — hard to sustain without marketing.

### Option C — Switch weather API
Replace Open-Meteo with a weather API that permits commercial use on a free tier (e.g. WeatherAPI, OpenWeatherMap free plan). Trade-off: these require API keys, have lower rate limits, and return fewer parameters than Open-Meteo.

---

## Zero-Cost AI Assistant

- **Quick-suggest chips** (jacket, umbrella, etc.) run entirely locally via `WeatherAdvisor.kt` — cost $0.
- **Free-form chat** uses the user's own OpenRouter key stored on-device. You bear no token costs.
- Keep this design as-is — it's the right model for a solo app.

---

## Security Reminder — OpenRouter API Key

`OPENROUTER_API_KEY` is injected into `BuildConfig` at build time. APKs can be decompiled.

- The current default is empty — users supply their own key at runtime. This is safe.
- Never put a real production key in `local.properties` before a release build.

---

## Release Track Requirements (New Developer Accounts)

Since late 2023, Google requires **new personal developer accounts** to run a **closed test with at least 20 opted-in testers for 14 continuous days** before Production access is granted. This account was just created and identity-verified, so it almost certainly applies — Console will not offer a direct path to Production until this gate is cleared, regardless of how complete the store listing is.

**Practical sequence:**
1. **"Finish setting up your app"** checklist on the app dashboard — bundles Data safety, content rating, app content declarations (ads, target audience, news/government apps), and store listing. Do this first; it gates everything else.
2. **Internal testing track** — upload the signed AAB here first. No review wait, instant availability, good for a final sanity check (e.g. your own email) before wider eyes see it.
3. **Closed testing track** — same AAB, add a tester list of ≥20 opted-in emails (a Google Group works), let it run 14 continuous days. This is the actual gate for Production.
4. **Production** — once Console shows production access unlocked, promote the tested release and submit for review.

This means realistic public availability is **~2+ weeks out**, not a few days, once the store listing itself is finished — budget for the mandatory closed-testing window.

---

## Launch Checklist

- [x] Change `applicationId` away from `com.example.*` (`io.github.smenon2710.skyspeak`)
- [x] Add `isShrinkResources = true` to release build type
- [x] Move `logging-interceptor` to `debugImplementation`
- [x] Generate release keystore, back it up securely, wire into Gradle
- [x] Write and host privacy policy (GitHub Pages — https://smenon2710.github.io/weatherly/privacy.html)
- [x] Decide on monetization model (donation model — Razorpay Payment Page link added)
- [x] Build signed AAB (not APK)
- [x] Create Play Console account ($25, paid, identity verification complete)
- [x] Create app entry in Play Console (package name `io.github.smenon2710.skyspeak`) — now on "Finish setting up your app" dashboard
- [x] Fill out Data Safety form (completed via "Finish setting up your app" flow) — **⚠️ re-open and update 2026-07-16: needs the National Weather Service added as a location-sharing third party, see section 4 above**
- [x] Complete content rating questionnaire (completed via "Finish setting up your app" flow — outcome: Everyone)
- [x] Store listing assets ready in `store_assets/` — 3 screenshots (1280×2560, 2:1, no alpha), 512×512 icon, 1024×500 feature graphic — verified against Play Console upload spec (upload to Play Console still needed)
- [x] Set category to Weather, write short + full description (completed via "Finish setting up your app" flow)
- [x] Finish remaining "Finish setting up your app" declarations — privacy policy URL, sign-in details, ads, target audience, government apps, financial features, health, contact details
- [x] Upload signed AAB to Internal testing track, sanity-check install — caught and fixed two real bugs this way (B4 in `IMPROVEMENTS.md`: silently-failing radar screen from a ProGuard/Moshi interaction; a stale Razorpay payment link)
- [ ] **In progress:** Closed testing started 2026-07-07 via track with Google Group `skyspeak-testers` as the tester list. 20-tester threshold cleared — 25 testers opted in as of 2026-07-13, continuously enrolled for 6 days. 14-continuous-day clock is running; ETA for completion ~2026-07-21 (see Release Track Requirements above)
- [x] versionCode 6 / 1.0.5 uploaded to the Closed testing track — surfaced the edge-to-edge and native-debug-symbol Play Console warnings, both since fixed (B5 in `IMPROVEMENTS.md`)
- [x] versionCode 7 / 1.0.6 published to the Closed testing track (2026-07-13) — bundles the Settings screen (items 25/35/36), a security fix for the OpenRouter key field (B9), and four layout bugs found via tester feedback and on-device testing (B6–B8, plus two header-crowding rounds)
- [x] versionCode 8 / 1.0.7 published to the Closed testing track (2026-07-16) — current live version under testing. Bundles the NWS weather-advisories feature (severity-colored cards, detail sheets, AI-chat awareness, resolution tracking), the `docs/privacy.html`/Data Safety disclosure update for the new National Weather Service data recipient (see section 4 above), and a design-audit pass (B10–B13 in `IMPROVEMENTS.md`: unit-inconsistent temp-bar colors, alert-card placement/notification-confusion fixes, and same-event/same-area alert deduplication). Does not reset the 14-day closed-testing clock (same as the versionCode 6→7 update)
- [ ] Apply for / confirm Production access unlocked
- [ ] Promote to Production and submit for review (typically 1–3 days)

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

**✅ Re-submitted in Console (2026-07-16):** the live Data Safety form was updated and verified against the Store Listing preview — Location (Approximate + Precise) shows Shared/App functionality and Collected/Optional/App functionality, matching the table below. Note that Play Console's form has no field to name individual third-party recipients (Open-Meteo, NWS) — it only captures data type/shared/purpose, so checking "Shared: Yes, App functionality" for Location is the complete disclosure Console requires; the specific recipient names live in `docs/privacy.html` instead.

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
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (every API call — Open-Meteo, OpenRouter, National Weather Service — goes over HTTPS)
- *Do you provide a way for users to request that their data be deleted?* → **No account exists, so there's no server-side data tied to a user identity to delete.** All app data (cache, preferences, on-device API key) lives in local app storage and is removed on uninstall. If Console requires an affirmative answer here, note in the form that data deletion is handled via app uninstall since no account/backend exists.

**Per data type:**

| Data type | Collected? | Shared? | Purpose | Optional? | Ephemeral? |
|---|---|---|---|---|---|
| Approximate location | Yes | Yes — Open-Meteo, and National Weather Service (U.S. locations only, for active weather alerts) | App functionality | Yes — a manual city search works without ever granting location permission | Not claimed — don't check "ephemeral" unless you've confirmed Open-Meteo's/NWS's own retention policy |
| Precise location | Yes | Yes — Open-Meteo, and National Weather Service (U.S. locations only, for active weather alerts) | App functionality | Yes — same as above | Same as above |
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

**Short description** (80 char limit, 79 used):
```
Ad-free forecasts, a live weather view, and an AI assistant. No account needed.
```

**Full description** (4000 char limit, ~2227 used):
```
SkySpeak is a clean, ad-free weather app built for people who just want accurate forecasts without the clutter — plus an AI assistant for the planning questions a forecast alone can't answer.

WHAT YOU GET
• Current conditions, next 24 hours, and a 7-day forecast in one glance
• A full-screen animated weather view — rain, snow, fog, clouds, and more — so conditions read at a glance instead of buried in numbers
• Rain and snow shown as what they actually are: real, separate amounts — not one vague "precipitation" figure that could quietly mean either
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
Forecasts come from Open-Meteo, a free and open weather data provider, attributed at the bottom of the weather screen. Weather advisories come from the National Weather Service, the official U.S. government forecasting agency.

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
| Phone screenshots | 2–8, min 1080px tall, ≤2:1 aspect ratio, no alpha | ✅ Re-shot 2026-07-17 against the current build (animated background, both themes) — `store_assets/screenshot-0{1..5}-*.png`: weather-dark, weather-light, chat-dark, chat-light, settings-light. Raw captures were 1280×2856 (2.23:1, over the ≤2:1 limit) with transparent rounded corners (device-frame artifact) — cropped to 1280×2560 (exact 2:1, top-anchored) and alpha-flattened onto black before saving. Old `screenshot-01-weather.png`/`screenshot-02-chat.png`/`screenshot-03-radar.png` removed (`git rm`, recoverable from history). Still need uploading to Play Console — see the Launch Checklist. |
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

## Android 16 (API 36) Target SDK Requirement (surfaced 2026-07-21)

While the Production access application (see below) was still pending review, Play Console's Policy status page separately flagged: **"App must target Android 16 (API level 36) or higher."** This is unrelated to the Production access gate — it's Google's annual target-API-level policy (every app must target within one year of the latest Android release) — but it has its own hard deadline: **action by 2026-08-31, or the app loses the ability to publish *any* update** (existing Closed Testing/Production listings stay live and installable; only new releases are blocked). It does not retroactively affect the pending Production access review.

**Fix:** Bumped `compileSdk` 35 → 36 and `targetSdk` 35 → 36 in `app/build.gradle.kts`, shipped as **versionCode 11 / 1.0.10**. Low-risk change for this codebase specifically — edge-to-edge (the main behavioral change API 36 enforces) was already handled correctly (`enableEdgeToEdge()` in `MainActivity`, fixed for the API 35 edge-to-edge requirement back in B5), and the app has no NDK/native code, no foreground services, and no other component types affected by Android 16 behavior changes.

Verified before committing, same bar as every prior release:
- `assembleDebug`, `bundleRelease` (R8 + resource shrink + signing), and `lint` all `BUILD SUCCESSFUL` with `compileSdk`/`targetSdk` 36 — no new errors, same 33 pre-existing lint warnings as before the bump.
- `jarsigner -verify` on the signed `app-release.aab`: `jar verified`, cert valid through 2053 (same keystore, unaffected by the SDK bump).
- **Real on-device sanity install** on a freshly-booted **API 36 emulator** (`Medium_Phone_API_36.0` AVD) — the first release verified on the actual target API level rather than just an older emulator/device: fresh install, granted location, confirmed `target_sdk_version=36` in `nativeloader` logs, a real forecast loaded and the animated background rendered correctly, navigated Weather → Chat → Settings (confirmed Settings shows "Version 1.0.10"), zero `FATAL EXCEPTION`/crashes in logcat across the full session. Uninstalled and emulator shut down after.

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
- [x] Fill out Data Safety form (completed via "Finish setting up your app" flow) — re-submitted 2026-07-16 to reflect the National Weather Service as a location-sharing third party (see section 4 above)
- [x] Complete content rating questionnaire (completed via "Finish setting up your app" flow — outcome: Everyone)
- [x] Store listing assets ready in `store_assets/` — 3 screenshots (1280×2560, 2:1, no alpha), 512×512 icon, 1024×500 feature graphic — verified against Play Console upload spec (upload to Play Console still needed)
- [x] Set category to Weather, write short + full description (completed via "Finish setting up your app" flow)
- [x] Finish remaining "Finish setting up your app" declarations — privacy policy URL, sign-in details, ads, target audience, government apps, financial features, health, contact details
- [x] Upload signed AAB to Internal testing track, sanity-check install — caught and fixed two real bugs this way (B4 in `IMPROVEMENTS.md`: silently-failing radar screen from a ProGuard/Moshi interaction; a stale Razorpay payment link)
- [ ] **In progress:** Closed testing started 2026-07-07 via track with Google Group `skyspeak-testers` as the tester list. 20-tester threshold cleared — 25 testers opted in as of 2026-07-13, continuously enrolled for 6 days. 14-continuous-day clock is running; ETA for completion ~2026-07-21 (see Release Track Requirements above)
- [x] versionCode 6 / 1.0.5 uploaded to the Closed testing track — surfaced the edge-to-edge and native-debug-symbol Play Console warnings, both since fixed (B5 in `IMPROVEMENTS.md`)
- [x] versionCode 7 / 1.0.6 published to the Closed testing track (2026-07-13) — bundles the Settings screen (items 25/35/36), a security fix for the OpenRouter key field (B9), and four layout bugs found via tester feedback and on-device testing (B6–B8, plus two header-crowding rounds)
- [x] versionCode 8 / 1.0.7 published to the Closed testing track (2026-07-16) — bundled the NWS weather-advisories feature (severity-colored cards, detail sheets, AI-chat awareness, resolution tracking), the `docs/privacy.html`/Data Safety disclosure update for the new National Weather Service data recipient (see section 4 above), and a design-audit pass (B10–B13 in `IMPROVEMENTS.md`: unit-inconsistent temp-bar colors, alert-card placement/notification-confusion fixes, and same-event/same-area alert deduplication). Did not reset the 14-day closed-testing clock (same as the versionCode 6→7 update)
- [x] **versionCode 9 / 1.0.8 — built, verified, and published to the Closed Testing track (2026-07-17).** Removes the radar map (low perceived value, plus its `osmdroid` dependency/permission/ProGuard rule) and replaces it with a full-screen animated weather background (26 real-data-driven conditions). Also fixes real rain/snow-accuracy bugs: `WeatherAdvisor.umbrella()` could recommend an umbrella on snow days; the Precipitation tile/chat context/detail chart didn't distinguish rain from snow at all; `SparklineTile` auto-scaled percentage charts so a 10–25% day and an 80–100% day looked visually identical. Two more bugs found via on-device testing of this same build, both fixed (B16/B17 in `IMPROVEMENTS.md`): a translucent "frosted glass" card fill produced visibly patchy seams (reverted to opaque cards); the animated background's rain/snow/sleet/hail/wind particles were drawn in white, invisible against light mode's pale gradient (now theme-aware).
  - [x] Signed `app-release.aab` built locally (Gradle 9.4.1 + Android Studio's bundled JBR + the project's own `weatherly-release.jks`, since this repo has no checked-in `gradlew`) and verified with `jarsigner -verify` (cert valid through 2053, matching the keystore's original validity)
  - [x] Sanity-installed the release-signed build on a running emulator (not just the AAB build succeeding) — fresh install, granted location, confirmed a real forecast loaded (South Brunswick Township, NJ), the animated background rendered correctly for the live condition, an active NWS Air Quality Alert card displayed correctly, hourly/7-day cards were fully opaque with no bleed-through, no radar icon in the header, and a clean logcat with zero crashes — the same kind of check that originally caught B4
  - [x] Screenshots re-shot 2026-07-17 against the current build — see the Store Listing Assets table above
  - [ ] **In progress:** rollout to the `skyspeak-testers` Closed Testing track submitted in Play Console — does not reset the 14-day tester clock (same as the versionCode 6→7→8 updates)
  - [x] **Confirmed stale and fixed, 2026-07-27 (post-launch):** the live Store listing's full description was still the pre-radar-removal, pre-NWS-alerts text — verified by pasting the actual live "About this app" copy and diffing it against this doc's draft. It described a "Live precipitation radar with play/pause and a frame scrubber" (removed entirely 2026-07-17) and the old static hero gradient, while omitting the NWS severe-weather-alerts feature (shipped 2026-07-16) and the rain-vs-snow accuracy work entirely. Corrected description pasted into Play Console → Store presence → Main store listing and submitted for review. Screenshot count confirmed correct (5, matching `store_assets/`) but individual screenshot *content* wasn't independently verified beyond the count. Data safety summary card on the live listing showed only "Location" (not chat data) — not confirmed as an actual form gap vs. just an abbreviated public-facing summary; worth a direct check of the Data Safety form in Console next time it's open
- [x] **versionCode 10 / 1.0.9 — built, verified, and published to the Closed Testing track (2026-07-17).** Ships the Alert Display Redesign (B18 in `IMPROVEMENTS.md`): `AlertBannerList`/`ResolvedAlertCard` shrink to a single-line severity strip, `DetailSheet.AlertList` becomes a tappable summary-row chooser instead of stacking full alert content, and `DetailSheetContent` gains a `verticalScroll` safety net so no sheet type can silently crop content again.
  - [x] Signed `app-release.aab` built locally (same Gradle 9.4.1 + Android Studio JBR + `weatherly-release.jks` toolchain as versionCode 9) and verified with `jarsigner -verify` (`jar verified`, cert valid through 2053)
  - [x] Sanity-installed a matching signed release APK on a running emulator — fresh install, granted location, confirmed a real forecast loaded (South Brunswick Township, NJ). This location happened to be carrying two live simultaneous NWS alerts (a Severe Flood Watch + an Air Quality Alert) at build time, which made this an unusually strong sanity check: exercised the exact new alert-redesign code path end to end — the compact strip with its two severity dots, tapping through to the new `DetailSheet.AlertList` chooser, and drilling into the full `AlertDetailContent` for the Flood Watch — with zero crashes/exceptions in logcat throughout
  - [x] No store screenshot re-shoot needed — checked all 5 current screenshots in `store_assets/`; none show an active NWS alert (the weather screenshots use a non-US location, outside NWS coverage), so none are made stale by the alert UI change
  - [x] Uploaded `app-release.aab` to Play Console → Closed Testing (`skyspeak-testers` track) — does not reset the 14-day tester clock (same as versionCode 6→7→8→9)
- [x] Applied for Production access — submitted 2026-07-20, 21:50 (Play Console). Status: "We have your application for production access — we're reviewing your application form. We'll email the account owner with an update. This usually takes seven days or less, but may occasionally take longer."
- [x] **Google Play production access granted — confirmed in Play Console dashboard 2026-07-27** ("Congratulations! Your app has been granted Google Play production access"). Cleared in ~7 days, right at Google's own ETA.
- [x] **versionCode 11 / 1.0.10 promoted to Production and PUBLISHED — 2026-07-27, 6:47 PM** (Submission ID 14, promoted directly from the existing Closed Testing release rather than re-uploading the AAB, release name left as the auto-picked "11 (1.0.10)"). Play Console's Submission activity log shows status **Published** — this is SkySpeak's first-ever public Play Store release.
- [x] **Confirmed live via Play Console dashboard, ~10 minutes post-publish:** rollout at **100%** ("Latest production release · 100% · Phones and tablets, +2 more"), "You have no unpublished changes," 1 install / 100% install base, `View on Play` link active on the app header. Dashboard banner: "App update published. Users should see changes immediately, but this may take longer."
- [ ] **Open — needs re-check, don't assume resolved:** the dashboard's "Monitor and improve" panel still shows **"Action by 31 Aug: Update your target API level by 31 August 2026 to release updates to your app"** even with versionCode 11 (targetSdk 36) live. Most likely just Play Console's policy re-scan lagging behind a 10-minute-old release, but not yet confirmed — check **Policy → App content / Policy status** directly for the target SDK level the live release is being evaluated against, and don't consider this requirement cleared until that page confirms it.
- [ ] Ongoing: watch **Monitor and improve → Android vitals** (crash rate, ANR rate, average rating — all showing "–" as of publish, expected with only 1 install so far) over the next 1–2 weeks as real installs accumulate. This is the only crash/ANR signal available, since the app has no crash-reporting SDK of its own.
- [ ] **Android vitals flagged 5 "recommended actions" (Technical quality / User experience) within 10 minutes of the Production release going live** — none are policy violations or block publishing, but see the full investigation and fix plan in `IMPROVEMENTS.md`'s "Open — Android Vitals Findings, First Production Release (2026-07-27)" section (V1–V5). Short version: 4 of the 5 (stale `androidx.fragment` transitive version, an edge-to-edge display warning, deprecated edge-to-edge APIs, missing bitmap downsampling) all trace back to this app's AndroidX/Compose dependency stack (`compose-bom`, `core-ktx`, `activity-compose`, `glance-appwidget`, `play-services-location`) being well over a year stale — none of the flagged deprecated calls exist in this app's own source, confirmed via grep. The 5th (R8 "optimised resource shrinking") needs the correct current AGP flag confirmed before touching `build.gradle.kts`, since a wrong guess risks silently breaking the signed release build. Not yet implemented as of this checklist entry — read `IMPROVEMENTS.md` before starting that work.
- [x] **versionCode 11 / 1.0.10 — built, verified, and ready to upload (2026-07-21).** Bundles three things beyond versionCode 10: the Android 16 (API 36) target SDK compliance fix (`compileSdk`/`targetSdk` 35 → 36, see the section above), and two light-theme hero-legibility bug fixes (B20/B21/B22 in `IMPROVEMENTS.md`) — hero text color contrast against `WeatherBackground`'s animated scene, hero font weight, and a follow-up contrast fix for night+cloudy conditions specifically, the last one caught by this release's own sanity-install process rather than user report.
  - [x] Signed `app-release.aab` built via `bundleRelease` (Gradle 9.5.0 + AGP 9.3.0 + Android Studio JBR + `weatherly-release.jks`) and verified with `jarsigner -verify` (`jar verified`, cert valid through 2053)
  - [x] Sanity-installed the actual signed release APK (`assembleRelease`, same signing/R8/minification as the AAB) on a fresh API 36 emulator — Franklin Park, NJ. First pass caught B22 live: at this location's real (nighttime) local time, Overcast text was still using the light-backdrop dark-text pair against what `conditionGradient` actually paints as a dark navy sky, pixel-sampled at ~2.3:1 contrast. Fixed, rebuilt, reinstalled, reverified on the identical repro (~4.68:1 after). Then navigated Weather → Chat → Settings (Settings correctly showed "Version 1.0.10") with zero crashes throughout
  - [x] Uploaded `app-release.aab` to Play Console → Closed Testing (`skyspeak-testers` track), 2026-07-22 — does not reset the 14-day tester clock (same as versionCode 6→7→8→9→10). Uploaded ahead of Production access being granted, as a real-tester safety net for the hero-fix UI changes before this becomes the first-ever Production release — not a hard requirement, since the closed-testing tenure gate was already cleared before the Production access application, and Google's own Android 16 notice treats testing tracks as optional ("you can test your app using internal, closed, or open testing" before publishing to production)
  - [ ] Confirm in Play Console that the "App must target Android 16" policy issue clears once this build is live on the track
- [x] **Discovered 2026-08-03 — the target-API warning was still live a full week after versionCode 11's Production publish**, now with specific text: "Your highest non-compliant target API level is Android 15 (API level 35)." Specific enough to rule out re-scan lag. Root cause, confirmed via Play Console's "Latest releases and bundles" screen: the **Internal Testing track had never been updated past versionCode 4 (1.0.3, targetSdk 35)** — still "Available to internal testers," 0% install base, but an active release. Google's target-API policy check evidently scans every active track, not just Production, so a forgotten Internal Testing release kept tripping the warning even with versionCode 11 (targetSdk 36) at 100% on Closed Testing and Production.
- [x] Attempted to fix by promoting versionCode 11 to Internal Testing directly. Blocked two ways: uploading the same AAB as a new artifact failed ("Version code 11 has already been used" — Play Console requires versionCodes unique app-wide, not per-track), and "Promote release" from Closed Testing/Production didn't list Internal Testing as a destination (promotion only goes "up" the ladder). **Process fix, applies to every release from here on: upload to Internal Testing first, then promote up through Closed Testing → Production** — the reverse of how versionCode 6 through 11 were all actually shipped (uploaded straight to Closed Testing, Internal Testing left stale).
- [x] **versionCode 12 / 1.0.11 — built and verified 2026-08-03.** Bundles the fix for three of the five Android Vitals findings (V1–V3): dependency bump across `compose-bom`, `core-ktx`, `activity-compose`, `lifecycle-runtime/viewmodel-compose`, `play-services-location`, plus AGP 9.3.0 → 9.3.1 (see `IMPROVEMENTS.md`'s "Dependency Refresh" section for exact versions and why the very latest `core-ktx`/`lifecycle` releases had to be rejected — both now require `compileSdk 37`). `assembleDebug`/`lint`/`bundleRelease` all `BUILD SUCCESSFUL`, `jarsigner -verify` → jar verified (cert valid to 2053-11-16), `assembleRelease` produced a signed installable APK. On-device sanity install performed by the user directly (no device/emulator available in the agent's own environment for this release): confirmed version string, clean Weather/Chat/Settings navigation, location permission flow, theme/unit toggles, edge-to-edge rendering, and chat streaming, no crashes. One pre-existing widget layout bug found during the sanity pass (B24 in `IMPROVEMENTS.md`) — confirmed unrelated to this release's changes, deferred to the next (widget-redesign) release.
- [x] **Uploaded to Internal Testing, 2026-08-03** — `app-release.aab` (versionCode 12) rolled out successfully, per the corrected sequencing above (Internal Testing first this time). Console flagged one new non-blocking warning during upload — "This App Bundle contains native code, and you've not uploaded debug symbols" — investigated and confirmed benign before proceeding; see `IMPROVEMENTS.md`'s Dependency Refresh section for the root cause. Next: confirm Internal Testing shows versionCode 12 (not the old versionCode 4) in the releases overview, then promote to Closed Testing, then promote to Production.

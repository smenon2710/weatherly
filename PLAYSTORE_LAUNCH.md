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

Fill out in Play Console → Store listing → Data safety:

| Data type | Collected | Shared with third parties | Purpose |
|---|---|---|---|
| Approximate location | Yes | No | Core feature — weather for current location |
| Precise location | Yes | No | Core feature |
| User queries (chat) | Optional | Yes (OpenRouter) | AI assistant, only when user provides a key |

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

Also guard the interceptor setup in `NetworkModule` with `if (BuildConfig.DEBUG) { ... }`.

---

### 6. Build as AAB, Not APK

Google requires Android App Bundles (`.aab`) for all new app submissions.

In Android Studio: **Build → Generate Signed Bundle / APK → Android App Bundle → select release signing config → Finish.**

---

## Store Listing Assets (one-time effort)

| Asset | Spec | Free tool |
|---|---|---|
| App icon | 512×512 PNG | Canva / draw.io, or screenshot from emulator |
| Feature graphic | 1024×500 PNG | Canva free tier |
| Phone screenshots | 2–8, min 1080px tall | Android emulator camera button |
| Short description | ≤80 characters | — |
| Full description | ≤4000 characters | — |
| Category | Select "Weather" | — |
| Content rating | Questionnaire in Console | Will be "Everyone" |

**Screenshot tip:** Run on a Pixel 6 emulator, take screenshots via the emulator toolbar, pull with `adb pull /sdcard/Pictures/`.

---

## Monetization & Open-Meteo Licensing (Critical)

Open-Meteo's free tier is **non-commercial use only**. If you charge for the app, run ads, or accept in-app purchases, you must buy a commercial API plan starting at **$29/month**.

### Option A — Donation model (stay free, $0/month)
List the app for free, add a donation link (Ko-Fi or Buy Me a Coffee) in the Settings screen. Because no features are paywalled and the app remains free, you stay within Open-Meteo's non-commercial terms. Recommended for a solo launch.

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

## Launch Checklist

- [x] Change `applicationId` away from `com.example.*` (`io.github.smenon2710.skyspeak`)
- [x] Add `isShrinkResources = true` to release build type
- [x] Move `logging-interceptor` to `debugImplementation`
- [ ] Generate release keystore, back it up securely, wire into Gradle
- [ ] Write and host privacy policy (GitHub Pages)
- [ ] Decide on monetization model (donation / paid / free) before submitting
- [ ] Build signed AAB (not APK)
- [ ] Create Play Console account ($25)
- [ ] Fill out Data Safety form
- [ ] Complete content rating questionnaire
- [ ] Upload store listing assets (icon, feature graphic, screenshots)
- [ ] Set category to Weather, write short + full description
- [ ] Submit for review (typically 1–3 days for a first submission)

# Play Store Launch Guide — Weatherly

> Generated with Claude. Solo developer, cost-efficient path.

---

## Cost Summary (up front)

| Item | Cost |
|---|---|
| Google Play Console registration | **$25 one-time** |
| Open-Meteo (weather data) | **Free** (CC BY 4.0) |
| Privacy policy hosting (GitHub Pages) | **Free** |
| OpenRouter API | **Free tier** (user-provided or google/gemma free model) |
| Backend / server | **None needed** — pure client app |

**Total to ship: $25.**

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

`ACCESS_FINE_LOCATION` + data sent to Open-Meteo and optionally OpenRouter requires a hosted privacy policy. Google blocks listing without one.

**Free approach:**
1. Generate a policy at [privacypolicygenerator.info](https://www.privacypolicygenerator.info) — free.
2. Host it via GitHub Pages: create a `docs/privacy.html` in your repo, enable Pages in repo settings → it becomes `https://yourusername.github.io/weatherly/privacy`. Completely free.
3. Paste the URL into Play Console → Store listing → Privacy policy.

---

### 3. Release Signing Keystore

You need a keystore to sign the release build.

> **Critical:** Back up the `.jks` file and passwords somewhere safe (e.g. a password manager). If you lose the keystore, you can never push updates to the same listing.

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
        isShrinkResources = true   // add this alongside minify
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

Fill this out in Play Console → Store listing → Data safety. Declare:

| Data type | Collected | Shared with third parties | Purpose |
|---|---|---|---|
| Approximate location | Yes | No | Core app feature (weather for current location) |
| Precise location | Yes | No | Core app feature |
| User queries (chat) | Optional — only if OpenRouter key is active | Yes (OpenRouter) | AI assistant feature |

---

## Small Fixes Before Submitting

### 5. Remove Logging Interceptor from Release Builds

`logging-interceptor` currently logs all HTTP traffic in release too. Move it to debug-only.

In `app/build.gradle.kts`, change:
```kotlin
// before
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// after
debugImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

Guard the interceptor setup in `NetworkModule` with `if (BuildConfig.DEBUG) { ... }`.

---

### 6. Build as AAB, Not APK

Google requires Android App Bundles (`.aab`) for all new app submissions.

In Android Studio: **Build → Generate Signed Bundle / APK → Android App Bundle → select release signing config → Finish.**

---

## Store Listing Assets (one-time effort)

| Asset | Spec | Free tool |
|---|---|---|
| App icon | 512×512 PNG | Export/screenshot from emulator, or draw.io / Canva |
| Feature graphic | 1024×500 PNG | Canva free tier |
| Phone screenshots | 2–8 screenshots, min 1080px tall | Android emulator (any size), then crop |
| Short description | ≤80 characters | — |
| Full description | ≤4000 characters | — |
| Category | Select "Weather" | — |
| Content rating | Fill questionnaire in Console | Will be rated "Everyone" |

**Screenshot tip:** Run the app on a Pixel 6 emulator (standard Play Store phone size), take screenshots via the emulator's camera button, pull them with `adb pull /sdcard/Pictures/`.

---

## Security Reminder — OpenRouter API Key

Your `OPENROUTER_API_KEY` is injected into `BuildConfig` at build time. APKs can be decompiled, exposing any key baked in.

- **Current design is safe:** the default in `local.properties` is empty; users supply their own key at runtime.
- **Never put a real production key in `local.properties` before a release build.**
- The runtime-entry + `SharedPreferences` storage flow is the right model for a solo app — keep it.

---

## Launch Checklist

- [ ] Change `applicationId` away from `com.example.*`
- [ ] Add `isShrinkResources = true` to release build type
- [ ] Move `logging-interceptor` to `debugImplementation`
- [ ] Generate release keystore, back it up, wire into Gradle
- [ ] Write and host privacy policy (GitHub Pages)
- [ ] Build signed AAB (not APK)
- [ ] Create Play Console account ($25)
- [ ] Fill out Data Safety form
- [ ] Complete content rating questionnaire
- [ ] Upload store listing assets (icon, feature graphic, screenshots)
- [ ] Set category to Weather
- [ ] Write short description (≤80 chars) and full description
- [ ] Submit for review (typically 1–3 days for first submission)

import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Read optional AI settings from local.properties (never committed to git).
// Both are optional — if the key is blank the app will prompt for it at runtime
// in the AI chat screen and store it on-device.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val openRouterApiKey: String = localProps.getProperty("OPENROUTER_API_KEY", "")
val openRouterModel: String =
    localProps.getProperty("OPENROUTER_MODEL", "google/gemma-4-26b-a4b-it:free")

android {
    namespace = "com.example.weatherly"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.smenon2710.skyspeak"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.0.11"

        // Surfaced to code via BuildConfig.OPENROUTER_API_KEY / OPENROUTER_MODEL.
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        buildConfigField("String", "OPENROUTER_MODEL", "\"$openRouterModel\"")
    }

    // Release keystore lives at the project root (weatherly-release.jks), gitignored.
    // STORE_PASSWORD / KEY_PASSWORD come from local.properties — never committed.
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("weatherly-release.jks")
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
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose (versions managed by the BOM)
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")

    // Location (FusedLocationProvider)
    implementation("com.google.android.gms:play-services-location:21.4.0")

    // Home-screen widget (Jetpack Glance)
    implementation("androidx.glance:glance-appwidget:1.1.1")
}

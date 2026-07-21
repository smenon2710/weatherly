// Top-level build file. Versions are declared here and applied per-module.
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    // Compose compiler is bundled with Kotlin 2.0+; this plugin enables it.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

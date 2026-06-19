// Top-level build file. Versions are declared here and applied per-module.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Compose compiler is bundled with Kotlin 2.0+; this plugin enables it.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

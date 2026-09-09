# Keep Moshi-generated and reflective model classes.
-keep class com.example.weatherly.data.model.** { *; }
-keepclassmembers class com.example.weatherly.data.model.** { *; }
# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
# WorkManager instantiates Worker subclasses via reflection (Class.forName + a
# (Context, WorkerParameters) constructor lookup), using the class name persisted in its own
# database at enqueue time. Consumer ProGuard rules bundled in the work-runtime AAR cover
# WorkManager's own internal classes, but not necessarily app-authored Worker subclasses —
# keep this app's explicitly so R8 can never strip/rename it or its constructor out from under
# WorkManager in a release build (untested by any debug-build run, since minification is release-
# build-only).
-keep class com.example.weatherly.notifications.** { *; }

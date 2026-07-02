# OSMDroid
-keep class org.osmdroid.** { *; }
# Keep Moshi-generated and reflective model classes.
-keep class com.example.weatherly.data.model.** { *; }
-keepclassmembers class com.example.weatherly.data.model.** { *; }
# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
# Keep every @JsonClass model regardless of package — the data.model.** rule above
# only covers that one package and missed RadarScreen.kt's RainViewer models,
# which silently broke radar parsing in every release build (R8 stripped the
# classes KotlinJsonAdapterFactory's reflective adapter needs at runtime).
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }

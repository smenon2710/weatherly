# OSMDroid
-keep class org.osmdroid.** { *; }
# Keep Moshi-generated and reflective model classes.
-keep class com.example.weatherly.data.model.** { *; }
-keepclassmembers class com.example.weatherly.data.model.** { *; }
# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
# Keep DTOs
-keep class com.openclaude.weather.data.remote.** { *; }
# osmdroid
-keep class org.osmdroid.** { *; }

# Moshi reflective adapters — keep generated/data model classes intact.
-keep class com.dockerdroid.app.data.api.models.** { *; }
-keepclassmembers class com.dockerdroid.app.compose.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# libsu uses reflection for its root service binding.
-keep class com.topjohnwu.superuser.** { *; }

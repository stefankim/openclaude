# Moshi reflective adapters — keep generated/data model classes intact.
-keep class com.dockerdroid.app.data.api.models.** { *; }
-keepclassmembers class com.dockerdroid.app.compose.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# libsu uses reflection for its root service binding.
-keep class com.topjohnwu.superuser.** { *; }

# JSch loads cipher/kex/mac implementations reflectively by class name.
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Tink (backing EncryptedSharedPreferences) ships its own consumer rules; silence
# optional dependencies it references.
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.**

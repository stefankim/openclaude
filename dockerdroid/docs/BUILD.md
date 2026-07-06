# Build & Release

## Prerequisites

- JDK 17
- Android SDK (compileSdk 35, build-tools 35)
- Gradle 8.10 (or use the wrapper once generated)

## First-time setup

The Gradle wrapper jar is not committed. Generate it once:

```bash
cd dockerdroid
gradle wrapper --gradle-version 8.10
```

Point Gradle at your SDK via `local.properties` (auto-created by Android Studio) or
the `ANDROID_HOME` environment variable.

## Common tasks

```bash
./gradlew assembleDebug          # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # build + install onto a connected device
./gradlew testDebugUnitTest      # JVM unit tests (parser, compatibility logic)
./gradlew connectedAndroidTest   # instrumented Compose tests (needs a device/emulator)
./gradlew lint                   # Android lint
```

## Release APK (signed)

Signing is already wired in `app/build.gradle.kts`: it reads `keystore.properties`
(or `DOCKERDROID_*` env vars) and signs the release build when they're present,
falling back to unsigned/debug otherwise.

**Local:**
1. Generate a keystore and print the values to set:
   ```bash
   dockerdroid/scripts/generate-keystore.sh
   ```
2. Create `dockerdroid/keystore.properties` (git-ignored) with:
   ```
   DOCKERDROID_STORE_FILE=/abs/path/dockerdroid-release.jks
   DOCKERDROID_STORE_PASSWORD=…
   DOCKERDROID_KEY_ALIAS=dockerdroid
   DOCKERDROID_KEY_PASSWORD=…
   ```
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

**In CI:** add the four secrets printed by the script
(`DOCKERDROID_KEYSTORE_BASE64`, `DOCKERDROID_STORE_PASSWORD`,
`DOCKERDROID_KEY_ALIAS`, `DOCKERDROID_KEY_PASSWORD`) as repository secrets. The
release workflow then decodes the keystore and publishes a **signed** APK; without
them it publishes the debug-signed APK.

## Installing on device

The device must be **rooted (Magisk)**. Side-load the APK, open DockerDroid, grant
root when prompted, run the compatibility check, then tap **Install**. The app
downloads and SHA-256-verifies the Docker static bundle into `/data/local/docker`.

## CI

`.github/workflows/dockerdroid-android.yml` runs lint + unit tests + `assembleDebug`
on every push touching `dockerdroid/**`, uploads the debug APK, and builds a release
APK for tags matching `dockerdroid-v*`.

## Binary checksum pinning

`BinarySource.kt` carries `REPLACED_AT_RELEASE` placeholders. The release job
substitutes the real SHA-256 from docker.com's published checksums so the on-device
installer verifies every download before extraction.

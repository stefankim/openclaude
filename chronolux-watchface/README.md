# ChronoLux — Premium Watch Face for Samsung Galaxy Watch

ChronoLux is a complete, original Wear OS watch face application targeting
Samsung Galaxy Watch 4 / 5 / 6 / 7 / 8 / Ultra (Wear OS 3+, API 30+). It is
inspired by the *premium watch face* category that dominates the paid charts
on the Galaxy Store and Google Play, but all code, artwork and branding in
this project are original.

## Features

- **Three layout modes** — Analog, Digital, and Hybrid (analog hands over a
  compact digital readout), switchable from the on-watch editor.
- **Four color themes** — Midnight Gold, Ocean, Crimson, Forest. Each theme
  styles the hands, tick ring, digital text and the radial background
  gradient.
- **Accent color override** — keep the theme's accent or pick from eight
  fixed swatches (Gold, Coral, Sky, Mint, Violet, Rose, Snow) independently
  of the theme.
- **Time & date formatting** — 12-hour / 24-hour clock and three date
  formats, plus a toggle for the sweeping second hand and the minute ticks.
- **Always-on (ambient) mode** — pure-black background with thin outlined
  hands to minimize AMOLED power draw and burn-in.
- **Automatic battery-saver** — when the watch reports a low, non-charging
  battery, the face drops the per-frame radial gradient and the sweeping
  second hand to conserve power.
- **Three complication slots** — left (defaults to step count), right
  (day & date) and bottom (watch battery). All slots accept ranged-value,
  short-text and image complications and are user-reassignable (e.g. to a
  heart-rate provider).
- **On-watch configuration editor** — built with Compose for Wear OS using
  the `androidx.wear.watchface.editor` `EditorSession`, so changes persist
  through the system like any first-party face.

## Quality & tooling

- **Unit tests** (`./gradlew test`) cover the pure style logic — colors are
  stored as packed ints so the enums run under plain JUnit.
- **detekt** static analysis (`./gradlew detekt`) with a tuned config.
- **Gradle version catalog** (`gradle/libs.versions.toml`) centralizes all
  dependency versions.
- **GitHub Actions CI** (`.github/workflows/chronolux-build.yml`) runs the
  tests + detekt and publishes the debug APK as a downloadable artifact on
  every push.

## Project structure

```
chronolux-watchface/
├── build.gradle.kts                  # Root build (plugins via version catalog + detekt)
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml            # Version catalog (all dependency versions)
│   └── wrapper/                      # Committed Gradle wrapper
├── config/detekt/detekt.yml          # Static-analysis config
├── dist/                             # Prebuilt debug + release APK and AAB
└── app/
    ├── build.gradle.kts              # Wear OS app module (+ release signing)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml   # Watch face service + editor activity
        │   ├── java/com/chronolux/watchface/
        │   │   ├── ChronoLuxWatchFaceService.kt   # System entry point
        │   │   ├── ChronoLuxRenderer.kt           # Canvas renderer (+ battery-saver)
        │   │   ├── style/
        │   │   │   ├── ColorTheme.kt              # The four palettes (pure ints)
        │   │   │   ├── StyleOptions.kt            # Time/date/accent option enums
        │   │   │   └── UserStyleConstants.kt      # UserStyleSchema definition
        │   │   ├── complications/
        │   │   │   └── ComplicationSetup.kt       # The three complication slots
        │   │   └── editor/
        │   │       ├── WatchFaceConfigActivity.kt     # Compose editor UI
        │   │       └── WatchFaceConfigStateHolder.kt  # EditorSession state
        │   └── res/                  # Strings, colors, vector preview & icon
        └── test/                     # JUnit tests for the style logic
```

## Prebuilt artifacts (use straight away)

The `dist/` folder contains ready-to-use builds:

| File | Use |
|---|---|
| `ChronoLux-1.0.0-debug.apk` | Sideload onto a watch/emulator for testing |
| `ChronoLux-1.0.0-release.apk` | Minified, release-signed APK for sideloading |
| `ChronoLux-1.0.0-release.aab` | App Bundle for Google Play upload |

For a quick install, use the debug APK — it installs with no extra signing
steps on a Galaxy Watch (or Wear OS emulator) over ADB:

```
# Pair the watch: Settings > Developer options > ADB debugging + Wireless debugging
adb connect <watch-ip>:<port>
adb install dist/ChronoLux-1.0.0-debug.apk
```

Then open the watch face picker, long-press the current face, tap **Add**
(or **Customize**), and select **ChronoLux**. Tap **Customize** to open the
on-watch editor for themes, layout and complications.

> Note: this is a *debug* APK intended for sideloading and testing. For
> store distribution, build a release variant signed with your own keystore
> (see *Publishing notes* below).

## Building from source

1. Open the `chronolux-watchface` folder in **Android Studio** (Koala or
   newer), or build from the command line with the included wrapper.
2. Let Gradle sync, then build:
   ```
   ./gradlew :app:assembleDebug
   ```
3. The APK lands in `app/build/outputs/apk/debug/`.

This project was verified to build with **Gradle 8.14.3**, **AGP 8.5.2**,
**Kotlin 1.9.24**, **JDK 17+**, and Android **SDK 34** / **build-tools
34.0.0**. The Gradle wrapper is committed, so `./gradlew` works without a
preinstalled Gradle.

## Running on a watch or emulator

- **Emulator**: in Android Studio, create a *Wear OS Large Round* device
  (API 30+), run the `app` configuration, then long-press the watch face on
  the emulator and pick **ChronoLux**.
- **Physical Galaxy Watch**: enable Developer Options and ADB debugging on
  the watch, connect over Wi-Fi (`adb connect <watch-ip>:5555`), then
  `./gradlew :app:installDebug`. Select ChronoLux from the watch face
  picker; tap **Customize** to open the editor.

## Release builds & signing

The release variant is minified (R8) and signed from a keystore that is kept
**out of version control**. To build a signed release:

1. Create a keystore (once):
   ```
   keytool -genkeypair -v -keystore chronolux-release.keystore \
     -alias chronolux -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Add `keystore.properties` at the project root (gitignored):
   ```
   storeFile=chronolux-release.keystore
   storePassword=••••••
   keyAlias=chronolux
   keyPassword=••••••
   ```
3. Build:
   ```
   ./gradlew assembleRelease   # dist-ready APK
   ./gradlew bundleRelease     # .aab for Google Play
   ```

If `keystore.properties` is absent, the release variant still builds but is
left unsigned. The bundled `dist/ChronoLux-1.0.0-release.*` artifacts are
signed with a demo keystore — generate your own before publishing.

## Publishing notes

To ship on the Galaxy Store / Google Play you will need your own keystore,
a `versionCode` bump strategy, store screenshots and a privacy policy. Note
that Google now steers *new* watch faces toward the declarative **Watch Face
Format**; this Canvas/Kotlin implementation is ideal for sideloading and
learning — confirm current Play policy before submitting a new listing.

## License

MIT — see [LICENSE](LICENSE).

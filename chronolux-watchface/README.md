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
- **Always-on (ambient) mode** — pure-black background with thin outlined
  hands to minimize AMOLED power draw and burn-in.
- **Three complication slots** — left (defaults to heart rate), right
  (daily steps) and bottom (watch battery). All slots accept ranged-value,
  short-text and image complications and are user-reassignable.
- **On-watch configuration editor** — built with Compose for Wear OS using
  the `androidx.wear.watchface.editor` `EditorSession`, so changes persist
  through the system like any first-party face.

## Project structure

```
chronolux-watchface/
├── build.gradle.kts                  # Root build script (AGP 8.5, Kotlin 1.9)
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/                   # Wrapper config (Gradle 8.7)
└── app/
    ├── build.gradle.kts              # Wear OS app module
    └── src/main/
        ├── AndroidManifest.xml       # Watch face service + editor activity
        ├── java/com/chronolux/watchface/
        │   ├── ChronoLuxWatchFaceService.kt   # System entry point
        │   ├── ChronoLuxRenderer.kt           # Canvas renderer (interactive + ambient)
        │   ├── style/
        │   │   ├── ColorTheme.kt              # The four palettes
        │   │   └── UserStyleConstants.kt      # UserStyleSchema definition
        │   ├── complications/
        │   │   └── ComplicationSetup.kt       # The three complication slots
        │   └── editor/
        │       ├── WatchFaceConfigActivity.kt     # Compose editor UI
        │       └── WatchFaceConfigStateHolder.kt  # EditorSession state
        └── res/                      # Strings, colors, vector preview & icon
```

## Building

1. Open the `chronolux-watchface` folder in **Android Studio** (Koala or
   newer). Android Studio will offer to download the Gradle 8.7 wrapper
   declared in `gradle/wrapper/gradle-wrapper.properties`.
2. Let Gradle sync, then build:
   ```
   ./gradlew :app:assembleDebug
   ```
3. The APK lands in `app/build/outputs/apk/debug/`.

## Running on a watch or emulator

- **Emulator**: in Android Studio, create a *Wear OS Large Round* device
  (API 30+), run the `app` configuration, then long-press the watch face on
  the emulator and pick **ChronoLux**.
- **Physical Galaxy Watch**: enable Developer Options and ADB debugging on
  the watch, connect over Wi-Fi (`adb connect <watch-ip>:5555`), then
  `./gradlew :app:installDebug`. Select ChronoLux from the watch face
  picker; tap **Customize** to open the editor.

## Publishing notes

To ship on the Galaxy Store / Google Play you will need to add your own
signing config, a `versionCode` bump strategy, store screenshots and a
privacy policy. Heart-rate complications use the system data source, so no
extra runtime permission is requested by this app itself.

## License

MIT — see [LICENSE](LICENSE).

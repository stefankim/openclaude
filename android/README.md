# Weather & Radar (Android)

A modern, native Android weather app inspired by *WetterOnline – Weather & Radar*.
Built with **Kotlin + Jetpack Compose (Material 3)**.

## Features

- **Up to 10 saved locations** — search any city (Slovakia or worldwide), pin it, and
  it's remembered across launches (DataStore). Select, switch, and remove from the
  **Places** tab.
- **Today** — a fully animated, Compose-drawn weather scene (rotating sun, drifting
  clouds, falling rain/snow, lightning, twinkling stars, fog) with the current
  temperature, "feels like", an hourly strip for the next 24 h, and a details grid
  (wind, humidity, pressure, UV, sunrise/sunset).
- **Week** — 7-day forecast with gradient min→max temperature bars.
- **Extended** — up to 14 days with wind, precipitation totals, UV and sunrise.
- **Radar** — animated **storm & rain radar** (RainViewer) overlaid on an
  OpenStreetMap map (osmdroid, no API key), with play/pause and a time scrubber across
  past + nowcast (forecast) frames, centred on your selected location.
- **Home-screen widget** — a resizable widget that shows the selected location's live,
  continuously **animated** weather scene plus temperature and condition. Frames are
  rendered procedurally to a Bitmap and cycled by a lightweight service; data refreshes
  every 30 min via WorkManager (and on tap).
- Modern translucent "glass" UI with a full-screen gradient that follows the current
  weather and time of day.

## Tabs / screens

`Today · Week · Extended · Radar · Places` — switchable from the bottom navigation bar.

## Weather data

The app reads forecasts from **[Open-Meteo](https://open-meteo.com)** (free, no API
key). Open-Meteo resolves Slovak locations against the same numerical-weather-prediction
grids that **SHMÚ** (Slovenský hydrometeorologický ústav) publishes against, so coverage
for Slovakia is detailed. The data layer is isolated behind `WeatherRepository`, so a
dedicated SHMÚ endpoint can be dropped in later without touching the UI.

Radar tiles come from **[RainViewer](https://www.rainviewer.com/api.html)** (free, no key).

## Build & run

Requires Android Studio (Koala+), JDK 17, Android SDK 34.

```bash
cd android
./gradlew assembleDebug          # build the APK
./gradlew installDebug           # install on a connected device/emulator
```

Or open the `android/` folder in Android Studio and press Run. On first sync Android
Studio will create `local.properties` pointing at your SDK.

- **minSdk** 24, **targetSdk** 34
- No API keys required.

## Adding the widget

Long-press the home screen → **Widgets** → *Weather & Radar* → drag to place. The widget
animates while present and opens the app when tapped. (Save at least one location first.)

## Project layout

```
app/src/main/java/com/openclaude/weather/
  data/remote      Open-Meteo + geocoding + RainViewer APIs (Retrofit/Moshi)
  data/local       DataStore-backed saved-location store (max 10)
  data/repository   DTO → domain mapping, in-memory cache
  domain            Weather models + animation "scene" buckets
  ui/today          Animated current-conditions screen
  ui/daily          Week + Extended forecast screens
  ui/radar          osmdroid map + animated RainViewer overlay
  ui/locations      Search / add / select / remove places
  ui/components      Compose weather animations, icons, glass cards
  ui/navigation      Bottom-tab scaffold + weather-driven background
  widget            Home-screen app widget with live Bitmap animation
util               WMO weather-code mapping, formatting
```

## Notes

- The widget animation runs at ~8 fps from a background service while at least one
  widget is on screen; Android may pause it under aggressive battery saving. Data still
  refreshes periodically via WorkManager.
- Open-Meteo unix timestamps are already shifted to each location's timezone, so times
  are formatted in UTC to display correct local clock values.

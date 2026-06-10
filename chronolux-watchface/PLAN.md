# ChronoLux — Project Plan

## Goal

Build a complete Samsung Galaxy Watch application matching the functionality
of the top-paid app category on the watch app stores.

## Research summary

There is no single public "top paid" chart, but coverage of the Galaxy
Store and Play Store watch sections consistently shows the paid charts are
dominated by **premium watch faces** (e.g. WatchMaker Premium, Facer's paid
faces) plus a few utilities. Cloning a specific paid app verbatim is not
possible — its code, artwork and branding are copyrighted — so this project
implements an original app with equivalent functionality in that category.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Platform | Wear OS 3+ (API 30+) | All Galaxy Watch models since Watch4 run Wear OS; Tizen is deprecated |
| Language | Kotlin | Standard for Wear OS |
| Watch face stack | `androidx.wear.watchface` (service-based) | Full programmatic control: animations, ambient mode, complications |
| Editor UI | Compose for Wear OS + `EditorSession` | Modern, persists style through the system |
| Branding | Original ("ChronoLux") | Avoids trademark/copyright issues |

## Implementation steps

1. Gradle project scaffolding (root + `app` module, wrapper config).
2. Style system: `ColorTheme` palettes + `UserStyleSchema` (theme, layout
   mode, tick toggle).
3. Complication slots: left / right / bottom with system default sources
   (heart rate, steps, battery).
4. Canvas renderer: radial-gradient background, tick ring, analog hands
   with smooth sweep, digital/hybrid layouts, ambient mode with burn-in
   protection, highlight layer for the editor.
5. Watch face service wiring schema + slots + renderer.
6. On-watch config editor (Compose) backed by `EditorSession`.
7. Resources: strings, colors, complication style, vector preview, adaptive
   launcher icon.
8. Docs (README, this plan), commit, push, package zip.

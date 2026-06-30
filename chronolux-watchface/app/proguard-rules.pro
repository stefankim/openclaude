# Keep the watch face service so the system can bind to it.
-keep class com.chronolux.watchface.ChronoLuxWatchFaceService { *; }

# Keep the on-watch editor activity (launched via an implicit intent action).
-keep class com.chronolux.watchface.editor.WatchFaceConfigActivity { *; }

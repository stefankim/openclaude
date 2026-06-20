package com.openclaude.weather

import android.app.Application
import android.content.Context
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.repository.WeatherRepository
import org.osmdroid.config.Configuration

/** Holds app-wide singletons. Lightweight manual DI — no Hilt needed. */
class AppContainer(context: Context) {
    val repository: WeatherRepository = WeatherRepository()
    val locationStore: LocationStore = LocationStore(context.applicationContext)
}

class WeatherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        INSTANCE = this

        // osmdroid must have a valid User-Agent before any MapView is created, otherwise
        // the OpenStreetMap tile servers reject requests (HTTP 418) and the map stays blank.
        Configuration.getInstance().apply {
            load(this@WeatherApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }

    companion object {
        @Volatile private var INSTANCE: WeatherApp? = null
        fun container(): AppContainer = requireNotNull(INSTANCE).container
    }
}

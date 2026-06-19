package com.openclaude.weather

import android.app.Application
import android.content.Context
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.repository.WeatherRepository

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
    }

    companion object {
        @Volatile private var INSTANCE: WeatherApp? = null
        fun container(): AppContainer = requireNotNull(INSTANCE).container
    }
}

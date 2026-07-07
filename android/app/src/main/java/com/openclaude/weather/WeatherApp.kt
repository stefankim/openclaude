package com.openclaude.weather

import android.app.Application
import android.content.Context
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.repository.WeatherRepository
import com.openclaude.weather.notifications.ConditionsWorker
import com.openclaude.weather.notifications.MorningWorker
import org.osmdroid.config.Configuration
import java.io.File

/** Holds app-wide singletons. Lightweight manual DI — no Hilt needed. */
class AppContainer(context: Context) {
    val repository: WeatherRepository = WeatherRepository(context.applicationContext)
    val locationStore: LocationStore = LocationStore(context.applicationContext)
}

class WeatherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        INSTANCE = this

        // osmdroid must be configured before any MapView is created:
        // - a valid User-Agent, or tile servers reject requests;
        // - cache paths on INTERNAL storage. With scoped storage, osmdroid's default
        //   cache location can be unwritable; its tile writer then fails to initialise
        //   and every downloaded tile is silently dropped (blank placeholder grid).
        Configuration.getInstance().apply {
            load(this@WeatherApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid").apply { mkdirs() }
            osmdroidTileCache = File(filesDir, "osmdroid/tiles").apply { mkdirs() }
            save(this@WeatherApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }

        // Background workers check the user's toggles at run time and no-op when disabled.
        ConditionsWorker.schedule(this)
        MorningWorker.schedule(this)
    }

    companion object {
        @Volatile private var INSTANCE: WeatherApp? = null
        fun container(): AppContainer = requireNotNull(INSTANCE).container
    }
}

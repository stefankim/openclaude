package com.dockerdroid.app

import android.app.Application

/** Process entry point; owns the singleton [AppContainer]. */
class DockerDroidApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

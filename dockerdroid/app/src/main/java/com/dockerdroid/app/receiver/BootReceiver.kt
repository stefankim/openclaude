package com.dockerdroid.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dockerdroid.app.DockerDroidApplication
import com.dockerdroid.app.core.service.DockerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Launches the Docker foreground service after boot when the user opted in via
 * Settings → "Start Docker on Boot".
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        val app = context.applicationContext as DockerDroidApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app.container.settingsRepository.startOnBootOnce()) {
                    DockerForegroundService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

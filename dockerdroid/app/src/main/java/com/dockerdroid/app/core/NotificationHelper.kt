package com.dockerdroid.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/** Posts user-facing notifications (container crashes, finished pulls, VM ready). */
class NotificationHelper(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Docker events", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Container lifecycle and build/pull events."
                },
            )
        }
    }

    fun notify(title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(title.hashCode(), n) } // POST_NOTIFICATIONS may be denied
    }

    private companion object {
        const val CHANNEL = "docker_events"
    }
}

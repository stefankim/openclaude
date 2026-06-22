package com.dockerdroid.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.dockerdroid.app.DockerDroidApplication
import com.dockerdroid.app.MainActivity
import com.dockerdroid.app.R
import kotlinx.coroutines.launch

/**
 * Keeps the daemon alive while the app is backgrounded. Declared with the
 * `specialUse` foreground-service type (Android 14+), the only category that fits
 * "supervise a long-running native daemon".
 */
class DockerForegroundService : LifecycleService() {

    private val manager: DockerServiceManager
        get() = (application as DockerDroidApplication).container.serviceManager

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Reflect daemon state in the ongoing notification.
        lifecycleScope.launch {
            manager.state.collect { state -> updateNotification(state) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                lifecycleScope.launch {
                    manager.stop()
                    stopSelf()
                }
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification(DaemonState.Starting))
                lifecycleScope.launch { manager.start() }
            }
        }
        return START_STICKY
    }

    private fun updateNotification(state: DaemonState) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(state))
    }

    private fun buildNotification(state: DaemonState): Notification {
        val title = when (state) {
            is DaemonState.Running -> getString(R.string.notif_running)
            is DaemonState.Starting -> getString(R.string.notif_starting)
            is DaemonState.Error -> state.message
            DaemonState.Stopped -> getString(R.string.notif_stopped)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, DockerForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(state.isActive)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_stop), stop)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.service_channel_description) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "docker_engine"
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.dockerdroid.app.STOP"

        fun start(context: Context) {
            val intent = Intent(context, DockerForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, DockerForegroundService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}

package com.openclaude.weather.widget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Drives the widget's frame-by-frame animation while at least one widget exists, and
 * services on-demand data refreshes. Ticks at ~8 fps to keep battery use modest.
 */
class WeatherWidgetService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var phase = 0f
    private var ticking = false

    private val tick = object : Runnable {
        override fun run() {
            val manager = AppWidgetManager.getInstance(this@WeatherWidgetService)
            val ids = manager.getAppWidgetIds(
                ComponentName(this@WeatherWidgetService, WeatherWidgetProvider::class.java)
            )
            if (ids.isEmpty()) {
                stopSelf()
                return
            }
            WeatherWidgetProvider.renderAll(this@WeatherWidgetService, manager, ids, phase)
            phase = (phase + STEP) % 1f
            handler.postDelayed(this, FRAME_MS)
        }
    }

    // Pause the animation while the screen is off — no one can see it, so don't burn battery.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacks(tick)
                    ticking = false
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (!ticking) {
                        ticking = true
                        handler.post(tick)
                    }
                }
            }
        }
    }
    private var receiverRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!receiverRegistered) {
            registerReceiver(screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            })
            receiverRegistered = true
        }
        when (intent?.action) {
            ACTION_REFRESH -> scope.launch { runCatching { WidgetUpdater.refresh(applicationContext) } }
        }
        if (!ticking) {
            ticking = true
            handler.post(tick)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        handler.removeCallbacks(tick)
        scope.cancel()
        ticking = false
        super.onDestroy()
    }

    companion object {
        private const val FRAME_MS = 125L      // ~8 fps
        private const val STEP = 0.02f         // full animation cycle ~6.25s
        private const val ACTION_REFRESH = "com.openclaude.weather.widget.REFRESH"

        fun start(context: Context) {
            context.startService(Intent(context, WeatherWidgetService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WeatherWidgetService::class.java))
        }

        fun refreshData(context: Context) {
            context.startService(
                Intent(context, WeatherWidgetService::class.java).setAction(ACTION_REFRESH)
            )
        }
    }
}

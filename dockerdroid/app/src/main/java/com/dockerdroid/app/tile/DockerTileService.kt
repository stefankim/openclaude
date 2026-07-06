package com.dockerdroid.app.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.dockerdroid.app.DockerDroidApplication
import com.dockerdroid.app.core.service.DaemonState
import com.dockerdroid.app.core.service.DockerForegroundService

/**
 * Quick Settings tile to start/stop the on-device Docker daemon without opening the
 * app. Reflects the current [DaemonState].
 */
class DockerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val running = container().serviceManager.state.value is DaemonState.Running
        if (running) DockerForegroundService.stop(this) else DockerForegroundService.start(this)
        // Optimistic UI; the next onStartListening reconciles with real state.
        qsTile?.apply {
            state = if (running) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun updateTile() {
        val active = container().serviceManager.state.value.isActive
        qsTile?.apply {
            label = "Docker"
            icon = Icon.createWithResource(this@DockerTileService, android.R.drawable.stat_notify_sync)
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    private fun container() = (application as DockerDroidApplication).container
}

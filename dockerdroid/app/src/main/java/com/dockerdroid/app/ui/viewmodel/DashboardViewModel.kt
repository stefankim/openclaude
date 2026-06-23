package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.core.service.DaemonState
import com.dockerdroid.app.data.remote.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    data class Counts(val containers: Int = 0, val running: Int = 0, val images: Int = 0)

    val daemonState: StateFlow<DaemonState> = container.serviceManager.state

    /** Local-daemon controls only apply when not connected to a remote host. */
    val connection: StateFlow<Connection> = container.connectionManager.connection

    private val _counts = MutableStateFlow(Counts())
    val counts: StateFlow<Counts> = _counts.asStateFlow()

    fun startDaemon() = viewModelScope.launch { container.serviceManager.start() }
    fun stopDaemon() = viewModelScope.launch { container.serviceManager.stop() }

    fun refresh() {
        viewModelScope.launch {
            if (!container.api.ping()) return@launch
            val containers = container.api.listContainers(all = true)
            val images = container.api.listImages()
            _counts.value = Counts(
                containers = containers.size,
                running = containers.count { it.state == "running" },
                images = images.size,
            )
        }
    }
}

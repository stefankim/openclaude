package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.vm.VmState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VmViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<VmState> = container.vmManager.state
    val log: StateFlow<List<String>> = container.vmManager.log

    /** Boot the VM, and once its Docker daemon is up, route the app's client to it. */
    fun start() = viewModelScope.launch {
        container.vmManager.start()
        if (container.vmManager.state.value is VmState.Running) {
            container.connectionManager.useVm()
        }
    }

    fun stop() {
        container.vmManager.stop()
        container.connectionManager.useLocal()
    }
}

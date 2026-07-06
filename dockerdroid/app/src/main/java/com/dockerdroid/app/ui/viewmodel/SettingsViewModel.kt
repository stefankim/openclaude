package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.remote.Connection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val startOnBoot: StateFlow<Boolean> = container.settingsRepository.startOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoRestart: StateFlow<Boolean> = container.settingsRepository.autoRestart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val requireBiometric: StateFlow<Boolean> = container.settingsRepository.requireBiometric
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val connection: StateFlow<Connection> = container.connectionManager.connection

    fun setRequireBiometric(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setRequireBiometric(enabled)
    }

    /** Export all saved stacks as a JSON string (written to a file by the UI). */
    suspend fun exportBackup(): String = container.composeRepository.exportBackup()

    /** Import stacks from a backup JSON string. Returns count imported. */
    suspend fun importBackup(json: String): Int = container.composeRepository.importBackup(json)

    /** Drop the remote connection and talk to the on-device daemon again. */
    fun useLocal() = container.connectionManager.useLocal()

    /** Erase the saved remote host and its stored credentials. */
    fun forgetRemote() = container.connectionManager.forgetRemote()

    fun setStartOnBoot(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setStartOnBoot(enabled)
    }

    fun setAutoRestart(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutoRestart(enabled)
        container.serviceManager.autoRestart = enabled
    }
}

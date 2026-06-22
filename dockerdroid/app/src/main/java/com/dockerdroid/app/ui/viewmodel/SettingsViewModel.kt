package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val startOnBoot: StateFlow<Boolean> = container.settingsRepository.startOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoRestart: StateFlow<Boolean> = container.settingsRepository.autoRestart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setStartOnBoot(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setStartOnBoot(enabled)
    }

    fun setAutoRestart(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutoRestart(enabled)
        container.serviceManager.autoRestart = enabled
    }
}

package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.api.models.ApiNetwork
import com.dockerdroid.app.data.api.models.ApiVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworksViewModel(private val container: AppContainer) : ViewModel() {
    private val _items = MutableStateFlow<List<ApiNetwork>>(emptyList())
    val items: StateFlow<List<ApiNetwork>> = _items.asStateFlow()

    fun refresh() = viewModelScope.launch {
        runCatching { container.api.listNetworks() }.onSuccess { _items.value = it }
    }

    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) container.api.createNetwork(name.trim())
        refresh()
    }

    fun remove(id: String) = viewModelScope.launch {
        container.api.removeNetwork(id)
        refresh()
    }
}

class VolumesViewModel(private val container: AppContainer) : ViewModel() {
    private val _items = MutableStateFlow<List<ApiVolume>>(emptyList())
    val items: StateFlow<List<ApiVolume>> = _items.asStateFlow()

    fun refresh() = viewModelScope.launch {
        runCatching { container.api.listVolumes() }.onSuccess { _items.value = it }
    }

    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) container.api.createVolume(name.trim())
        refresh()
    }

    fun remove(name: String) = viewModelScope.launch {
        container.api.removeVolume(name)
        refresh()
    }
}

package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.api.models.ApiContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContainersViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val containers: List<ApiContainer> = emptyList(),
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { container.api.listContainers(all = true) }
                .onSuccess { _state.value = UiState(containers = it) }
                .onFailure { _state.value = UiState(error = it.message) }
        }
    }

    fun start(id: String) = act { container.api.startContainer(id) }
    fun stop(id: String) = act { container.api.stopContainer(id) }
    fun restart(id: String) = act { container.api.restartContainer(id) }
    fun remove(id: String) = act { container.api.removeContainer(id) }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
            refresh()
        }
    }
}

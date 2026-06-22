package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.api.models.ApiImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImagesViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val images: List<ApiImage> = emptyList(),
        val pulling: Boolean = false,
        val pullStatus: String = "",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            runCatching { container.api.listImages() }
                .onSuccess { _state.value = _state.value.copy(images = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun pull(reference: String) {
        _state.value = _state.value.copy(pulling = true, pullStatus = "Starting…")
        viewModelScope.launch {
            runCatching {
                container.api.pullImage(reference).collect { line ->
                    _state.value = _state.value.copy(pullStatus = line)
                }
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(pulling = false)
            refresh()
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            runCatching { container.api.removeImage(id, force = true) }
            refresh()
        }
    }
}

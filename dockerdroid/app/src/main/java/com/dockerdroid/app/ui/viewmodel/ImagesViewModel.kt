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
                    _state.value = _state.value.copy(pullStatus = summarize(line))
                }
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(pulling = false, pullStatus = "")
            container.notifications.notify("Pull finished", reference)
            refresh()
        }
    }

    /** Turn a raw pull-progress JSON event into a compact human status. */
    private fun summarize(json: String): String {
        val status = Regex("\"status\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
        val id = Regex("\"id\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
        val progress = Regex("\"progress\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
        return listOfNotNull(status, id?.let { "[$it]" }, progress).joinToString(" ").ifBlank { json }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            runCatching { container.api.removeImage(id, force = true) }
            refresh()
        }
    }
}

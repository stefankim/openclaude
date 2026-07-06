package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.api.TarWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Builds an image on the active daemon from a pasted Dockerfile. */
class ImageBuildViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val tag: String = "myimage:latest",
        val dockerfile: String = "FROM alpine:3\nRUN echo hello > /hello\nCMD [\"cat\", \"/hello\"]",
        val building: Boolean = false,
        val output: List<String> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setTag(v: String) { _state.value = _state.value.copy(tag = v) }
    fun setDockerfile(v: String) { _state.value = _state.value.copy(dockerfile = v) }

    fun build() {
        val s = _state.value
        if (s.building || s.dockerfile.isBlank()) return
        _state.value = s.copy(building = true, output = emptyList())
        viewModelScope.launch {
            val tar = TarWriter.archive(mapOf("Dockerfile" to s.dockerfile.toByteArray()))
            runCatching {
                container.api.buildImage(tar, s.tag.trim().ifBlank { "myimage:latest" }).collect { line ->
                    _state.value = _state.value.copy(output = (_state.value.output + line).takeLast(300))
                }
            }.onFailure {
                _state.value = _state.value.copy(output = _state.value.output + "error: ${it.message}")
            }
            _state.value = _state.value.copy(building = false)
        }
    }
}

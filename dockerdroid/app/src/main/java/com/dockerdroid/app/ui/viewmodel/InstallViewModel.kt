package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.core.install.CompatibilityReport
import com.dockerdroid.app.core.install.InstallProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstallViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val checking: Boolean = false,
        val report: CompatibilityReport? = null,
        val installing: Boolean = false,
        val progress: InstallProgress? = null,
        val installed: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun runCompatibilityCheck() {
        _state.value = _state.value.copy(checking = true, error = null)
        viewModelScope.launch {
            val report = container.compatibilityChecker.run()
            _state.value = _state.value.copy(
                checking = false,
                report = report,
                installed = container.installManager.isInstalled(),
            )
        }
    }

    fun install() {
        val runtime = _state.value.report?.recommendedRuntime ?: CompatibilityReport.Runtime.DOCKER
        if (runtime == CompatibilityReport.Runtime.UNSUPPORTED) {
            _state.value = _state.value.copy(error = "This device cannot run containers.")
            return
        }
        _state.value = _state.value.copy(installing = true, error = null)
        viewModelScope.launch {
            container.installManager.install(runtime).collect { progress ->
                _state.value = _state.value.copy(progress = progress)
                when (progress) {
                    is InstallProgress.Done ->
                        _state.value = _state.value.copy(installing = false, installed = true)
                    is InstallProgress.Failed ->
                        _state.value = _state.value.copy(installing = false, error = progress.message)
                    else -> Unit
                }
            }
        }
    }
}

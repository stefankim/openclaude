package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.api.models.ApiContainerInspect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Detail view for one container: inspect info, live logs, and live stats. */
class ContainerDetailViewModel(private val container: AppContainer) : ViewModel() {

    data class Stat(val cpuPercent: Double = 0.0, val memUsedMb: Long = 0, val memLimitMb: Long = 0,
                    val rxKb: Long = 0, val txKb: Long = 0)

    private val _inspect = MutableStateFlow<ApiContainerInspect?>(null)
    val inspect: StateFlow<ApiContainerInspect?> = _inspect.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _stat = MutableStateFlow(Stat())
    val stat: StateFlow<Stat> = _stat.asStateFlow()

    private val _execOutput = MutableStateFlow<List<String>>(emptyList())
    val execOutput: StateFlow<List<String>> = _execOutput.asStateFlow()

    private var containerId: String? = null
    private var started = false

    /** Run a one-shot command inside the container (works in root/SSH/VM modes). */
    fun exec(command: String) {
        val id = containerId ?: return
        if (command.isBlank()) return
        _execOutput.value = _execOutput.value + "$ $command"
        viewModelScope.launch {
            val result = runCatching { container.api.exec(id, listOf("sh", "-c", command)) }
                .getOrElse { "error: ${it.message}" }
            _execOutput.value = (_execOutput.value + result.lines()).takeLast(400)
        }
    }

    fun start(id: String) {
        if (started) return
        started = true
        containerId = id

        viewModelScope.launch {
            runCatching { container.api.inspectContainer(id) }.onSuccess { _inspect.value = it }
        }
        // Tail logs (bounded).
        viewModelScope.launch {
            runCatching {
                container.api.containerLogs(id, follow = true).collect { line ->
                    _logs.value = (_logs.value + line).takeLast(MAX_LOG_LINES)
                }
            }
        }
        // Poll stats.
        viewModelScope.launch {
            while (isActive) {
                runCatching { container.api.stats(id) }.onSuccess { s ->
                    val cpu = s.cpu; val pre = s.preCpu; val mem = s.memory
                    val cpuDelta = (cpu?.usage?.total ?: 0) - (pre?.usage?.total ?: 0)
                    val sysDelta = (cpu?.systemUsage ?: 0) - (pre?.systemUsage ?: 0)
                    val cores = cpu?.onlineCpus?.takeIf { it > 0 } ?: 1
                    val cpuPct = if (sysDelta > 0) cpuDelta.toDouble() / sysDelta * cores * 100 else 0.0
                    val net = s.networks?.values
                    _stat.value = Stat(
                        cpuPercent = cpuPct,
                        memUsedMb = (mem?.usage ?: 0) / (1024 * 1024),
                        memLimitMb = (mem?.limit ?: 0) / (1024 * 1024),
                        rxKb = (net?.sumOf { it.rxBytes } ?: 0) / 1024,
                        txKb = (net?.sumOf { it.txBytes } ?: 0) / 1024,
                    )
                }
                delay(POLL_MS)
            }
        }
    }

    private companion object {
        const val MAX_LOG_LINES = 400
        const val POLL_MS = 3_000L
    }
}

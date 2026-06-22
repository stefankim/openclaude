package com.dockerdroid.app.core.service

import com.dockerdroid.app.core.install.InstallManager.Paths
import com.dockerdroid.app.core.shell.RootShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the `dockerd` process lifecycle. The [DockerForegroundService] holds an
 * instance and mirrors [state] into its notification; ViewModels observe the same
 * [StateFlow].
 *
 * dockerd is launched detached (`setsid … &`) so it survives the transient root
 * shell, with its pid written to a file we poll for liveness and crash recovery.
 */
class DockerServiceManager(
    private val shell: RootShellManager,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<DaemonState>(DaemonState.Stopped)
    val state: StateFlow<DaemonState> = _state.asStateFlow()

    var autoRestart: Boolean = true

    private val pidFile = "${Paths.ROOT}/dockerd.pid"
    private val logFile = "${Paths.ROOT}/dockerd.log"

    /** Start dockerd if not already running. Idempotent. */
    suspend fun start() {
        if (_state.value.isActive) return
        _state.value = DaemonState.Starting

        val launch = shell.execScript(
            listOf(
                // Detach so the daemon outlives this su invocation.
                "setsid ${Paths.BIN}/dockerd " +
                    "--config-file ${Paths.DAEMON_JSON} " +
                    "--host unix://${Paths.SOCKET} " +
                    "--data-root ${Paths.DATA} " +
                    "> $logFile 2>&1 &",
                "echo \$! > $pidFile",
            ),
        )
        if (!launch.isSuccess) {
            _state.value = DaemonState.Error("Failed to launch dockerd: ${launch.err}")
            return
        }
        awaitSocketReady()
        startSupervisor()
    }

    /** Gracefully stop dockerd, escalating to SIGKILL if it ignores SIGTERM. */
    suspend fun stop() {
        autoRestart = false
        val pid = currentPid()
        if (pid != null) {
            shell.exec("kill -TERM $pid")
            repeat(GRACEFUL_STOP_TRIES) {
                if (!isProcessAlive(pid)) return@repeat
                delay(500)
            }
            if (isProcessAlive(pid)) shell.exec("kill -9 $pid") // fallback
        }
        shell.exec("rm -f $pidFile ${Paths.SOCKET}")
        _state.value = DaemonState.Stopped
    }

    /** Block until the API socket accepts connections or we give up. */
    private suspend fun awaitSocketReady() {
        repeat(SOCKET_WAIT_TRIES) {
            val pid = currentPid()
            if (pid == null || !isProcessAlive(pid)) {
                _state.value = DaemonState.Error("dockerd exited during startup. See log.")
                return
            }
            if (shell.exec("test -S ${Paths.SOCKET} && echo ok").out.contains("ok")) {
                _state.value = DaemonState.Running(pid)
                return
            }
            delay(1_000)
        }
        _state.value = DaemonState.Error("Timed out waiting for ${Paths.SOCKET}")
    }

    /** Poll liveness and auto-restart on crash. */
    private fun startSupervisor() {
        scope.launch {
            while (isActive) {
                delay(SUPERVISE_INTERVAL_MS)
                val current = _state.value
                if (current !is DaemonState.Running) continue
                if (!isProcessAlive(current.pid)) {
                    if (autoRestart) {
                        _state.value = DaemonState.Starting
                        start()
                    } else {
                        _state.value = DaemonState.Stopped
                    }
                    return@launch
                }
            }
        }
    }

    private suspend fun currentPid(): Int? =
        shell.exec("cat $pidFile 2>/dev/null").out.trim().toIntOrNull()

    private suspend fun isProcessAlive(pid: Int): Boolean =
        shell.exec("kill -0 $pid 2>/dev/null && echo alive").out.contains("alive")

    companion object {
        private const val SOCKET_WAIT_TRIES = 30
        private const val GRACEFUL_STOP_TRIES = 20
        private const val SUPERVISE_INTERVAL_MS = 5_000L
    }
}

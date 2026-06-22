package com.dockerdroid.app.core.service

/** Lifecycle of the Docker daemon, surfaced as a Flow to the UI. */
sealed interface DaemonState {
    data object Stopped : DaemonState
    data object Starting : DaemonState
    data class Running(val pid: Int) : DaemonState
    data class Error(val message: String) : DaemonState

    val isActive: Boolean get() = this is Running || this is Starting
}

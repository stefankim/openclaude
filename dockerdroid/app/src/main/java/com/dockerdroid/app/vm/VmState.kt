package com.dockerdroid.app.vm

/** Lifecycle of the on-device Docker VM. */
sealed interface VmState {
    data object Stopped : VmState
    data class Provisioning(val message: String) : VmState
    data object Booting : VmState
    data object Running : VmState
    data class Error(val message: String) : VmState

    val isActive: Boolean get() = this is Provisioning || this is Booting || this is Running
}

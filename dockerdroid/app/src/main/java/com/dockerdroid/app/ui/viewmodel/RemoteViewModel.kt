package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.remote.RemoteHost
import com.dockerdroid.app.data.remote.SshAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(private val container: AppContainer) : ViewModel() {

    enum class AuthMethod { PASSWORD, KEY }

    data class UiState(
        val host: String = "",
        val port: String = "22",
        val username: String = "",
        val authMethod: AuthMethod = AuthMethod.PASSWORD,
        val secret: String = "", // password or private-key PEM
        val passphrase: String = "",
        val connecting: Boolean = false,
        val connected: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(prefill())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun prefill(): UiState {
        val saved = container.connectionManager.savedHost() ?: return UiState()
        return UiState(
            host = saved.host,
            port = saved.port.toString(),
            username = saved.username,
        )
    }

    fun update(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
    }

    fun connect() {
        val s = _state.value
        val port = s.port.toIntOrNull() ?: 22
        if (s.host.isBlank() || s.username.isBlank() || s.secret.isBlank()) {
            _state.value = s.copy(error = "Host, username and credentials are required.")
            return
        }
        val host = RemoteHost(
            label = "${s.username}@${s.host}",
            host = s.host.trim(),
            port = port,
            username = s.username.trim(),
        )
        val auth = when (s.authMethod) {
            AuthMethod.PASSWORD -> SshAuth.Password(s.secret)
            AuthMethod.KEY -> SshAuth.PrivateKey(s.secret, s.passphrase.ifBlank { null })
        }

        _state.value = s.copy(connecting = true, error = null)
        viewModelScope.launch {
            container.connectionManager.connectRemote(host, auth)
                .onSuccess { _state.value = _state.value.copy(connecting = false, connected = true) }
                .onFailure { _state.value = _state.value.copy(connecting = false, error = it.message ?: "Connection failed") }
        }
    }
}

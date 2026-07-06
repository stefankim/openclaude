package com.dockerdroid.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.remote.DiscoveredHost
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
        val dialCommand: String = RemoteHost.DEFAULT_DIAL,
        val authMethod: AuthMethod = AuthMethod.PASSWORD,
        val secret: String = "", // password or private-key PEM
        val passphrase: String = "",
        val connecting: Boolean = false,
        val connected: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(prefill())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Previously saved hosts, for one-tap reconnect. */
    val savedHosts: List<RemoteHost> get() = container.connectionManager.savedHosts()

    private val _discovered = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discovered: StateFlow<List<DiscoveredHost>> = _discovered.asStateFlow()

    init {
        // Passively discover SSH hosts on the LAN while the screen is open.
        viewModelScope.launch {
            runCatching {
                container.hostDiscovery.discover().collect { h ->
                    if (_discovered.value.none { it.host == h.host }) {
                        _discovered.value = _discovered.value + h
                    }
                }
            }
        }
    }

    fun useDiscovered(h: DiscoveredHost) {
        _state.value = _state.value.copy(host = h.host, port = h.port.toString())
    }

    private fun prefill(): UiState {
        val saved = container.connectionManager.savedHost() ?: return UiState()
        return UiState(
            host = saved.host,
            port = saved.port.toString(),
            username = saved.username,
            dialCommand = saved.dialCommand,
        )
    }

    fun update(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
    }

    /**
     * Populate the form from a pairing QR/deep link of the form
     * `dockerdroid://<host>[:port]?user=<u>&dial=<cmd>`. The secret is still entered
     * by hand (we never encode credentials in a QR).
     */
    fun applyPairingUri(raw: String) {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return
        if (!uri.scheme.equals("dockerdroid", ignoreCase = true)) {
            _state.value = _state.value.copy(error = "Not a DockerDroid pairing code.")
            return
        }
        _state.value = _state.value.copy(
            host = uri.host ?: _state.value.host,
            port = (uri.port.takeIf { it > 0 } ?: 22).toString(),
            username = uri.getQueryParameter("user") ?: _state.value.username,
            dialCommand = uri.getQueryParameter("dial") ?: RemoteHost.DEFAULT_DIAL,
            error = null,
        )
    }

    fun connectSaved(host: RemoteHost) {
        _state.value = _state.value.copy(connecting = true, error = null)
        viewModelScope.launch {
            container.connectionManager.connectSaved(host.id)
                .onSuccess { _state.value = _state.value.copy(connecting = false, connected = true) }
                .onFailure { _state.value = _state.value.copy(connecting = false, error = it.message ?: "Connection failed") }
        }
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
            dialCommand = s.dialCommand.ifBlank { RemoteHost.DEFAULT_DIAL },
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

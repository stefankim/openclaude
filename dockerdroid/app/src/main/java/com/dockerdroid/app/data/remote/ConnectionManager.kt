package com.dockerdroid.app.data.remote

import android.content.Context
import com.dockerdroid.app.data.api.DockerApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Whether the active [DockerApiClient] targets this device or a remote host. */
sealed interface Connection {
    data object Local : Connection
    data class Remote(val host: RemoteHost) : Connection
}

/**
 * Single source of truth for which Docker daemon the app is talking to. Holds the
 * active [DockerApiClient] and lets the UI switch between the on-device daemon and a
 * saved SSH host. Repositories and ViewModels read [client] on each call so a switch
 * takes effect immediately.
 */
class ConnectionManager(context: Context) {

    private val store = RemoteHostStore(context)

    @Volatile
    private var sshConnection: SshDockerConnection? = null

    @Volatile
    var client: DockerApiClient = DockerApiClient.local()
        private set

    private val _connection = MutableStateFlow<Connection>(Connection.Local)
    val connection: StateFlow<Connection> = _connection.asStateFlow()

    /** A previously saved remote host, if any (credentials stay in the store). */
    fun savedHost(): RemoteHost? = store.load()?.first

    /**
     * Open an SSH connection, verify the Docker API answers, and make it active.
     * The host is persisted only after a successful ping.
     */
    suspend fun connectRemote(host: RemoteHost, auth: SshAuth): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = SshDockerConnection(host, auth)
                conn.connect()
                val candidate = DockerApiClient.remote(conn.newClient())
                check(candidate.ping()) { "Connected over SSH, but the Docker API did not respond." }

                sshConnection?.close()
                sshConnection = conn
                client = candidate
                store.save(host, auth)
                _connection.value = Connection.Remote(host)
            }
        }

    /** Reconnect to a previously saved host (e.g. on app launch). */
    suspend fun reconnectSaved(): Result<Unit> {
        val (host, auth) = store.load() ?: return Result.failure(IllegalStateException("No saved host"))
        return connectRemote(host, auth)
    }

    /** Switch back to the on-device daemon. */
    fun useLocal() {
        sshConnection?.close()
        sshConnection = null
        client = DockerApiClient.local()
        _connection.value = Connection.Local
    }

    /** Forget the saved remote host and its credentials. */
    fun forgetRemote() {
        store.clear()
        useLocal()
    }
}

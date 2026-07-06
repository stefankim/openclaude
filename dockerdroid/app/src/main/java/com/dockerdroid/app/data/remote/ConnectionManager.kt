package com.dockerdroid.app.data.remote

import android.content.Context
import com.dockerdroid.app.data.api.DockerApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Which Docker daemon the active [DockerApiClient] targets. */
sealed interface Connection {
    /** On-device daemon over the unix socket (requires root). */
    data object Local : Connection

    /** A remote daemon reached over SSH. */
    data class Remote(val host: RemoteHost) : Connection

    /** A real daemon running in the on-device QEMU VM, reached over loopback TCP. */
    data object Vm : Connection
}

/**
 * Single source of truth for which Docker daemon the app is talking to. Holds the
 * active [DockerApiClient] and switches between the on-device daemon, a saved SSH
 * host, and the on-device VM. Repositories/ViewModels read [client] on each call so
 * a switch takes effect immediately.
 */
class ConnectionManager(context: Context) {

    private val store = RemoteHostStore(context)
    private val knownHostsPath = File(context.filesDir, "known_hosts").absolutePath

    @Volatile
    private var sshConnection: SshDockerConnection? = null

    @Volatile
    var client: DockerApiClient = DockerApiClient.local()
        private set

    private val _connection = MutableStateFlow<Connection>(Connection.Local)
    val connection: StateFlow<Connection> = _connection.asStateFlow()

    /** All saved remote hosts (credentials stay in the encrypted store). */
    fun savedHosts(): List<RemoteHost> = store.listHosts()

    /** The most recently used remote host, if any. */
    fun savedHost(): RemoteHost? = store.loadLastUsed()?.first

    /**
     * Open an SSH connection, verify the Docker API answers, and make it active.
     * The host is persisted only after a successful ping.
     */
    suspend fun connectRemote(host: RemoteHost, auth: SshAuth): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = SshDockerConnection(host, auth, knownHostsPath)
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

    /** Connect to a previously saved host by id. */
    suspend fun connectSaved(id: String): Result<Unit> {
        val host = store.loadHost(id) ?: return Result.failure(IllegalStateException("Unknown host"))
        val auth = store.loadAuth(id) ?: return Result.failure(IllegalStateException("No credentials"))
        return connectRemote(host, auth)
    }

    /** Reconnect to the most recently used host (e.g. on app launch). */
    suspend fun reconnectSaved(): Result<Unit> {
        val (host, auth) = store.loadLastUsed()
            ?: return Result.failure(IllegalStateException("No saved host"))
        return connectRemote(host, auth)
    }

    /**
     * Point the active client at the on-device VM's Docker daemon (port-forwarded to
     * loopback by QEMU). The VM lifecycle itself is owned by QemuVmManager.
     */
    fun useVm(port: Int = VM_DOCKER_PORT) {
        sshConnection?.close()
        sshConnection = null
        client = DockerApiClient.tcp("127.0.0.1", port)
        _connection.value = Connection.Vm
    }

    /** Switch back to the on-device daemon. */
    fun useLocal() {
        sshConnection?.close()
        sshConnection = null
        client = DockerApiClient.local()
        _connection.value = Connection.Local
    }

    /** Forget a specific saved host (defaults to the active/most-recent one). */
    fun forgetRemote(id: String? = null) {
        val target = id ?: (_connection.value as? Connection.Remote)?.host?.id ?: store.loadLastUsed()?.first?.id
        if (target != null) store.delete(target)
        if (_connection.value is Connection.Remote) useLocal()
    }

    private companion object {
        // Mirrors VmImages.DOCKER_PORT; kept literal to avoid a vm→remote package dep.
        const val VM_DOCKER_PORT = 2375
    }
}

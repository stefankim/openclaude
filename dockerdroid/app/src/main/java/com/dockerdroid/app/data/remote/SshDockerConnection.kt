package com.dockerdroid.app.data.remote

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import okhttp3.OkHttpClient
import java.util.Properties

/**
 * Owns a single SSH [Session] to a [RemoteHost] and hands out an [OkHttpClient]
 * that dials the remote Docker socket through it.
 */
class SshDockerConnection(
    private val host: RemoteHost,
    private val auth: SshAuth,
) {
    private val jsch = JSch()

    @Volatile
    private var session: Session? = null

    /** Open (or reuse) the SSH session. Blocking; call off the main thread. */
    @Synchronized
    fun connect(): Session {
        session?.let { if (it.isConnected) return it }

        if (auth is SshAuth.PrivateKey) {
            jsch.addIdentity(
                "dockerdroid",
                auth.pem.toByteArray(),
                null,
                auth.passphrase?.toByteArray(),
            )
        }

        val s = jsch.getSession(host.username, host.host, host.port)
        if (auth is SshAuth.Password) s.setPassword(auth.password)
        s.setConfig(
            Properties().apply {
                // TODO: trust-on-first-use host-key pinning. For now traffic is
                // encrypted and the user is authenticated to the server, but the
                // server is not verified — acceptable on a trusted LAN only.
                this["StrictHostKeyChecking"] = "no"
                this["PreferredAuthentications"] = "publickey,password"
            },
        )
        s.connect(CONNECT_TIMEOUT_MS)
        session = s
        return s
    }

    /** OkHttp client routed through this connection's `dial-stdio` channels. */
    fun newClient(): OkHttpClient =
        OkHttpClient.Builder()
            .socketFactory(SshDialStdioSocketFactory { connect() to host.dialCommand })
            .retryOnConnectionFailure(false)
            .build()

    fun close() {
        session?.disconnect()
        session = null
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
    }
}

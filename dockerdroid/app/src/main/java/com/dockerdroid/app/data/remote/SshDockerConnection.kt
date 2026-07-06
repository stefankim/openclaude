package com.dockerdroid.app.data.remote

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import okhttp3.OkHttpClient
import java.util.Properties

/**
 * Owns a single SSH [Session] to a [RemoteHost] and hands out an [OkHttpClient]
 * that dials the remote Docker socket through it.
 *
 * Host keys are pinned trust-on-first-use: the first connection records the server's
 * key in [knownHostsPath]; later connections verify against it and **fail loudly** if
 * the key changed (possible man-in-the-middle), rather than silently trusting anything.
 */
class SshDockerConnection(
    private val host: RemoteHost,
    private val auth: SshAuth,
    private val knownHostsPath: String,
) {
    private val jsch = JSch()

    @Volatile
    private var session: Session? = null

    /** Open (or reuse) the SSH session. Blocking; call off the main thread. */
    @Synchronized
    fun connect(): Session {
        session?.let { if (it.isConnected) return it }

        jsch.setKnownHosts(knownHostsPath)

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
                // "ask" + our UserInfo = TOFU: unknown keys are accepted and stored;
                // a CHANGED key is rejected by JSch with a clear error.
                this["StrictHostKeyChecking"] = "ask"
                this["PreferredAuthentications"] = "publickey,password"
            },
        )
        s.userInfo = TofuUserInfo(auth)
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

    /**
     * Accepts a first-seen host key (so it gets pinned), supplies the password for
     * keyboard-interactive prompts, and never blindly answers other prompts.
     */
    private class TofuUserInfo(private val auth: SshAuth) : UserInfo {
        override fun getPassphrase(): String? = (auth as? SshAuth.PrivateKey)?.passphrase
        override fun getPassword(): String? = (auth as? SshAuth.Password)?.password
        override fun promptPassword(message: String?): Boolean = auth is SshAuth.Password
        override fun promptPassphrase(message: String?): Boolean = auth is SshAuth.PrivateKey
        override fun promptYesNo(message: String?): Boolean {
            // Only the "authenticity of host … can't be established" prompt (a new,
            // unknown key) reaches here under StrictHostKeyChecking=ask; a changed key
            // is rejected by JSch before prompting. Accepting pins the key.
            return message?.contains("authenticity", ignoreCase = true) == true
        }
        override fun showMessage(message: String?) = Unit
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
    }
}

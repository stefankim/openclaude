package com.dockerdroid.app.data.remote

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.Session
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory

/**
 * An OkHttp [SocketFactory] whose sockets are backed by an SSH `exec` channel
 * running the host's Docker dial command (`docker system dial-stdio`).
 *
 * Each "socket" opens a fresh channel on the shared [Session]; the channel's
 * stdin/stdout become the socket streams, and the remote command pipes them to the
 * Docker daemon socket. This is exactly how the Docker CLI implements
 * `DOCKER_HOST=ssh://…` — no TCP port or `socat` on the host is required.
 */
class SshDialStdioSocketFactory(
    private val provider: () -> Pair<Session, String>,
) : SocketFactory() {

    private inner class SshSocket : Socket() {
        private var channel: ChannelExec? = null
        private var input: InputStream? = null
        private var output: OutputStream? = null
        @Volatile private var closed = false

        override fun connect(endpoint: SocketAddress?) = connect(endpoint, 0)

        override fun connect(endpoint: SocketAddress?, timeout: Int) {
            val (session, command) = provider()
            val ch = session.openChannel("exec") as ChannelExec
            ch.setCommand(command)
            // Streams must be obtained before connect() in JSch.
            output = ch.outputStream
            input = ch.inputStream
            ch.connect(if (timeout > 0) timeout else DEFAULT_CONNECT_MS)
            channel = ch
        }

        override fun getInputStream(): InputStream =
            input ?: error("SSH socket not connected")

        override fun getOutputStream(): OutputStream =
            output ?: error("SSH socket not connected")

        override fun isConnected(): Boolean = channel?.isConnected == true
        override fun isClosed(): Boolean = closed
        override fun setSoTimeout(timeout: Int) { /* governed by the SSH session */ }
        override fun getSoTimeout(): Int = 0
        override fun shutdownInput() { /* no-op: channel teardown handles this */ }
        override fun shutdownOutput() { /* no-op */ }
        override fun close() {
            closed = true
            channel?.disconnect()
        }
    }

    override fun createSocket(): Socket = SshSocket()
    override fun createSocket(host: String?, port: Int): Socket = SshSocket()
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = SshSocket()
    override fun createSocket(host: InetAddress?, port: Int): Socket = SshSocket()
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket = SshSocket()

    private companion object {
        const val DEFAULT_CONNECT_MS = 15_000
    }
}

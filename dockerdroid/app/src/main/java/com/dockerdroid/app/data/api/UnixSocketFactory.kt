package com.dockerdroid.app.data.api

import android.net.LocalSocket
import android.net.LocalSocketAddress
import okhttp3.OkHttpClient
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory

/**
 * Teaches OkHttp to dial a unix domain socket instead of TCP.
 *
 * The Docker Engine listens on `unix:///data/local/docker/docker.sock`. We expose
 * it to OkHttp as the bogus host `localhost`; every connection is transparently
 * redirected to the real socket path. Android's [java.net] does not ship
 * `UnixDomainSocketAddress`, so the transport is Android's own [LocalSocket] in the
 * filesystem namespace, wrapped in a thin [Socket] facade that OkHttp can drive.
 */
class UnixSocketFactory(private val socketPath: String) : SocketFactory() {

    /** A [Socket] backed by a filesystem-namespace [LocalSocket]. */
    private inner class UnixSocket : Socket() {
        private val local = LocalSocket(LocalSocket.SOCKET_STREAM)

        @Volatile private var closed = false

        override fun connect(endpoint: SocketAddress?) = connect(endpoint, 0)

        override fun connect(endpoint: SocketAddress?, timeout: Int) {
            // The TCP endpoint OkHttp passes is ignored; we always dial the socket file.
            local.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))
        }

        override fun getInputStream(): InputStream = local.inputStream
        override fun getOutputStream(): OutputStream = local.outputStream
        override fun setSoTimeout(timeout: Int) { local.soTimeout = timeout }
        override fun getSoTimeout(): Int = local.soTimeout
        override fun isConnected(): Boolean = local.isConnected
        override fun isClosed(): Boolean = closed
        override fun shutdownInput() = local.shutdownInput()
        override fun shutdownOutput() = local.shutdownOutput()
        override fun close() {
            closed = true
            local.close()
        }
    }

    override fun createSocket(): Socket = UnixSocket()
    override fun createSocket(host: String?, port: Int): Socket = UnixSocket()
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = UnixSocket()
    override fun createSocket(host: InetAddress?, port: Int): Socket = UnixSocket()
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket = UnixSocket()

    companion object {
        /** Builds an [OkHttpClient] that always speaks to [socketPath]. */
        fun clientFor(socketPath: String): OkHttpClient =
            OkHttpClient.Builder()
                .socketFactory(UnixSocketFactory(socketPath))
                .build()
    }
}

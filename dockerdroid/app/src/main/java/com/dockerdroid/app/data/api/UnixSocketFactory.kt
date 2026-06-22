package com.dockerdroid.app.data.api

import okhttp3.OkHttpClient
import java.io.File
import java.net.InetAddress
import java.net.Socket
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel
import javax.net.SocketFactory

/**
 * Teaches OkHttp to dial a unix domain socket instead of TCP.
 *
 * The Docker Engine listens on `unix:///data/local/docker/docker.sock`. We expose
 * it to OkHttp as the bogus host `localhost:2375`; every connection is transparently
 * redirected to the real socket path. Requires API 33+ ([UnixDomainSocketAddress]),
 * which is below our minSdk of 31 only for the JDK type — handled via NIO channel.
 */
class UnixSocketFactory(private val socketPath: String) : SocketFactory() {

    private fun newSocket(): Socket {
        val channel = SocketChannel.open(UnixDomainSocketAddress.of(File(socketPath).toPath()))
        return channel.socket()
    }

    override fun createSocket(): Socket = newSocket()
    override fun createSocket(host: String?, port: Int): Socket = newSocket()
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = newSocket()
    override fun createSocket(host: InetAddress?, port: Int): Socket = newSocket()
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket = newSocket()

    companion object {
        /** Builds an [OkHttpClient] that always speaks to [socketPath]. */
        fun clientFor(socketPath: String): OkHttpClient =
            OkHttpClient.Builder()
                .socketFactory(UnixSocketFactory(socketPath))
                // dns is irrelevant for unix sockets but OkHttp still resolves the
                // dummy host; map it to loopback to avoid a real lookup.
                .dns { listOf(InetAddress.getByName("127.0.0.1")) }
                .build()
    }
}

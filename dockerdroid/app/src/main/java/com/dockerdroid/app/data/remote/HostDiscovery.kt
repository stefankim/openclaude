package com.dockerdroid.app.data.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** A machine advertising SSH on the local network. */
data class DiscoveredHost(val name: String, val host: String, val port: Int)

/**
 * Discovers `_ssh._tcp` services on the LAN via Android's Network Service Discovery,
 * so the connect screen can offer "tap your Mac's name" instead of typing an IP.
 */
class HostDiscovery(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discover(): Flow<DiscoveredHost> = callbackFlow {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
            override fun onServiceResolved(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val addr = info.host?.hostAddress ?: return
                trySend(DiscoveredHost(info.serviceName ?: addr, addr, info.port))
            }
        }
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                runCatching { nsd.resolveService(info, resolveListener) }
            }
            override fun onServiceLost(info: NsdServiceInfo?) {}
        }

        runCatching {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
        awaitClose { runCatching { nsd.stopServiceDiscovery(discoveryListener) } }
    }

    private companion object {
        const val SERVICE_TYPE = "_ssh._tcp."
    }
}

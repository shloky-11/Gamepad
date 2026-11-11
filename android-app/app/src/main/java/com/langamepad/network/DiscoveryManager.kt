package com.langamepad.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.langamepad.model.DiscoveredServer
import com.langamepad.model.ProtocolConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DiscoveryManager(context: Context) {
    private val tag = "DiscoveryManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (nsdManager == null) {
            Log.w(tag, "NsdManager not available on this device")
            return
        }
        stopDiscovery()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "Discovery failed to start: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "Discovery failed to stop: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(tag, "mDNS Discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(tag, "mDNS Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.i(tag, "Found service: ${serviceInfo.serviceName}")
                try {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                            Log.w(tag, "Resolve failed for ${info?.serviceName}: $errorCode")
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                            if (resolvedInfo == null) return
                            val host = resolvedInfo.host?.hostAddress ?: return
                            val tcpPort = resolvedInfo.port
                            val name = resolvedInfo.serviceName ?: "LAN Gamepad PC"

                            val server = DiscoveredServer(
                                name = name,
                                ip = host,
                                tcpPort = tcpPort
                            )

                            _discoveredServers.update { current ->
                                val filtered = current.filter { it.ip != host }
                                filtered + server
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e(tag, "Error triggering service resolve", e)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                val serviceName = serviceInfo?.serviceName ?: return
                _discoveredServers.update { current ->
                    current.filter { it.name != serviceName }
                }
            }
        }

        try {
            nsdManager.discoverServices(
                ProtocolConstants.MDNS_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start mDNS discovery", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w(tag, "Error stopping discovery", e)
            }
            discoveryListener = null
        }
    }
}

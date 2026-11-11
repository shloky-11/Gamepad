package com.langamepad.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Searching : ConnectionState()
    data class Pairing(val serverName: String, val ip: String) : ConnectionState()
    data class Connected(
        val serverName: String,
        val ip: String,
        val packetRateHz: Float = 0f,
        val pingMs: Int = 0
    ) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class DiscoveredServer(
    val name: String,
    val ip: String,
    val tcpPort: Int = ProtocolConstants.DEFAULT_TCP_PORT,
    val udpPort: Int = ProtocolConstants.DEFAULT_UDP_PORT,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

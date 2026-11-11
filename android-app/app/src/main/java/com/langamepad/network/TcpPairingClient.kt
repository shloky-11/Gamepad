package com.langamepad.network

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpPairingClient {
    private val tag = "TcpPairingClient"

    data class PairingResult(
        val success: Boolean,
        val sessionId: String = "",
        val sessionKey: ByteArray? = null,
        val udpPort: Int = 42100,
        val errorMessage: String = ""
    )

    suspend fun pairWithServer(
        ip: String,
        tcpPort: Int,
        pin: String,
        deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    ): PairingResult = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(ip, tcpPort), 4000) // 4-sec timeout
            socket.soTimeout = 5000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val req = JSONObject().apply {
                put("type", "PAIR_REQUEST")
                put("pin", pin)
                put("device_name", deviceName)
                put("client_version", 1)
            }

            writer.println(req.toString())

            val responseLine = reader.readLine()
            if (responseLine == null) {
                return@withContext PairingResult(false, errorMessage = "Server closed connection without response")
            }

            val res = JSONObject(responseLine)
            val type = res.optString("type")

            if (type == "PAIR_SUCCESS") {
                val sessionId = res.getString("session_id")
                val sessionTokenHex = res.getString("session_token")
                val udpPort = res.optInt("udp_port", 42100)

                val sessionKey = hexStringToByteArray(sessionTokenHex)
                Log.i(tag, "Successfully paired with $ip! Session: $sessionId")
                PairingResult(
                    success = true,
                    sessionId = sessionId,
                    sessionKey = sessionKey,
                    udpPort = udpPort
                )
            } else {
                val reason = res.optString("reason", "Pairing rejected by server")
                PairingResult(false, errorMessage = "Pairing failed: $reason")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during TCP pairing with $ip", e)
            PairingResult(false, errorMessage = "Connection error: ${e.localizedMessage}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

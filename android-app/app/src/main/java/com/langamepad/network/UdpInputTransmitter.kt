package com.langamepad.network

import android.util.Log
import com.langamepad.model.GamepadState
import com.langamepad.model.PacketEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UdpInputTransmitter(
    private val targetIp: String,
    private val targetPort: Int,
    private val sessionKey: ByteArray?,
    private val targetHz: Int = 100
) {
    private val tag = "UdpTransmitter"
    private val encoder = PacketEncoder()
    private val sequenceCounter = AtomicInteger(0)
    private var socket: DatagramSocket? = null
    private var transmitJob: Job? = null
    private val immediateSendRequested = AtomicBoolean(false)

    var currentMeasuredHz: Float = 0f
        private set

    fun start(scope: CoroutineScope, gamepadState: GamepadState) {
        stop()

        transmitJob = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket()
                val targetAddress = InetAddress.getByName(targetIp)
                val intervalMs = (1000L / targetHz).coerceAtLeast(8L) // 8ms = ~125 Hz
                var packetsSentInSec = 0
                var lastSecTimestamp = System.currentTimeMillis()

                Log.i(tag, "UDP transmitter streaming to $targetIp:$targetPort @ $targetHz Hz")

                while (isActive) {
                    val seq = sequenceCounter.incrementAndGet()
                    val packetBytes = encoder.encode(gamepadState, seq, sessionKey)

                    val datagram = DatagramPacket(
                        packetBytes,
                        packetBytes.size,
                        targetAddress,
                        targetPort
                    )
                    socket?.send(datagram)
                    packetsSentInSec++

                    val now = System.currentTimeMillis()
                    if (now - lastSecTimestamp >= 1000L) {
                        currentMeasuredHz = (packetsSentInSec * 1000f) / (now - lastSecTimestamp)
                        packetsSentInSec = 0
                        lastSecTimestamp = now
                    }

                    // Sleep for frame interval, but wake immediately if user pressed a button
                    var waited = 0L
                    while (waited < intervalMs && !immediateSendRequested.getAndSet(false)) {
                        val chunk = 2L.coerceAtMost(intervalMs - waited)
                        kotlinx.coroutines.delay(chunk)
                        waited += chunk
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "UDP Transmission error", e)
            } finally {
                socket?.close()
                socket = null
            }
        }
    }

    fun requestImmediateSend() {
        immediateSendRequested.set(true)
    }

    fun stop() {
        transmitJob?.cancel()
        transmitJob = null
        socket?.close()
        socket = null
    }
}

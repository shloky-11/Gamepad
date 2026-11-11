package com.langamepad.model

import com.langamepad.network.CryptoUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance, zero-garbage binary packet encoder.
 * Writes directly into an existing byte array using big-endian byte order.
 */
class PacketEncoder {
    private val buffer: ByteBuffer = ByteBuffer.allocate(ProtocolConstants.PACKET_SIZE).order(ByteOrder.BIG_ENDIAN)
    private val prefixBytes = ByteArray(28)

    fun encode(state: GamepadState, sequence: Int, sessionKey: ByteArray?): ByteArray {
        buffer.clear()

        // 0..3: Magic
        buffer.putInt(ProtocolConstants.MAGIC)
        // 4: Version
        buffer.put(ProtocolConstants.VERSION)
        // 5: Flags
        buffer.put(0.toByte())
        // 6..7: Reserved
        buffer.putShort(0.toShort())
        // 8..11: Sequence
        buffer.putInt(sequence)
        // 12..15: Buttons bitmask
        buffer.putInt(state.getButtonsMask())
        // 16..17: Left Stick X
        buffer.putShort(state.stickLx)
        // 18..19: Left Stick Y
        buffer.putShort(state.stickLy)
        // 20..21: Right Stick X
        buffer.putShort(state.stickRx)
        // 22..23: Right Stick Y
        buffer.putShort(state.stickRy)
        // 24: Left Trigger
        buffer.put((state.triggerL and 0xFF).toByte())
        // 25: Right Trigger
        buffer.put((state.triggerR and 0xFF).toByte())
        // 26: Dpad X
        buffer.put(state.dpadX)
        // 27: Dpad Y
        buffer.put(state.dpadY)

        // Compute HMAC over first 28 bytes
        val array = buffer.array()
        System.arraycopy(array, 0, prefixBytes, 0, 28)
        val authTag = CryptoUtils.computeAuthTag(sessionKey, prefixBytes)

        // 28..31: Auth Tag
        buffer.putInt(authTag)

        return array.clone()
    }
}

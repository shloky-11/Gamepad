package com.langamepad

import com.langamepad.model.GamepadState
import com.langamepad.model.PacketEncoder
import com.langamepad.model.ProtocolConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PacketEncoderTest {

    @Test
    fun testPacketEncodingLengthAndHeaders() {
        val encoder = PacketEncoder()
        val state = GamepadState().apply {
            setButtonPressed(ProtocolConstants.BTN_A, true)
            setButtonPressed(ProtocolConstants.BTN_START, true)
            stickLx = 15000
            stickLy = -20000
            triggerL = 255
            dpadX = -1
            dpadY = 1
        }

        val sequence = 105
        val packet = encoder.encode(state, sequence, null)

        assertEquals("Encoded packet size must be 32 bytes", 32, packet.size)

        val buf = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        val magic = buf.int
        val version = buf.get()
        val flags = buf.get()
        val reserved = buf.short
        val seq = buf.int
        val btns = buf.int
        val lx = buf.short
        val ly = buf.short
        val rx = buf.short
        val ry = buf.short
        val tl = buf.get().toInt() and 0xFF
        val tr = buf.get().toInt() and 0xFF
        val dx = buf.get()
        val dy = buf.get()
        val authTag = buf.int

        assertEquals(ProtocolConstants.MAGIC, magic)
        assertEquals(ProtocolConstants.VERSION, version)
        assertEquals(0.toByte(), flags)
        assertEquals(0.toShort(), reserved)
        assertEquals(sequence, seq)
        assertEquals(ProtocolConstants.BTN_A or ProtocolConstants.BTN_START, btns)
        assertEquals(15000.toShort(), lx)
        assertEquals((-20000).toShort(), ly)
        assertEquals(0.toShort(), rx)
        assertEquals(0.toShort(), ry)
        assertEquals(255, tl)
        assertEquals(0, tr)
        assertEquals((-1).toByte(), dx)
        assertEquals(1.toByte(), dy)
    }
}

package com.langamepad.model

object ProtocolConstants {
    const val MAGIC: Int = 0x4C475044 // "LGPD"
    const val VERSION: Byte = 1
    const val PACKET_SIZE: Int = 32

    const val DEFAULT_UDP_PORT: Int = 42100
    const val DEFAULT_TCP_PORT: Int = 42101
    const val MDNS_SERVICE_TYPE: String = "_langamepad._tcp."

    // Button bitmasks matching Xbox 360 controller
    const val BTN_A: Int = 1 shl 0
    const val BTN_B: Int = 1 shl 1
    const val BTN_X: Int = 1 shl 2
    const val BTN_Y: Int = 1 shl 3
    const val BTN_LB: Int = 1 shl 4
    const val BTN_RB: Int = 1 shl 5
    const val BTN_BACK: Int = 1 shl 6
    const val BTN_START: Int = 1 shl 7
    const val BTN_GUIDE: Int = 1 shl 8
    const val BTN_L3: Int = 1 shl 9
    const val BTN_R3: Int = 1 shl 10
    const val BTN_DPAD_UP: Int = 1 shl 11
    const val BTN_DPAD_DOWN: Int = 1 shl 12
    const val BTN_DPAD_LEFT: Int = 1 shl 13
    const val BTN_DPAD_RIGHT: Int = 1 shl 14
}

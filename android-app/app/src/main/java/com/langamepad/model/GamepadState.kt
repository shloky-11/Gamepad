package com.langamepad.model

import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe controller state model designed for high-frequency (60-120 Hz)
 * zero-allocation input updates and network transmission.
 */
class GamepadState {
    private val buttonsMask = AtomicInteger(0)

    @Volatile var stickLx: Short = 0
    @Volatile var stickLy: Short = 0
    @Volatile var stickRx: Short = 0
    @Volatile var stickRy: Short = 0

    @Volatile var triggerL: Int = 0 // 0..255
    @Volatile var triggerR: Int = 0 // 0..255

    @Volatile var dpadX: Byte = 0 // -1, 0, 1
    @Volatile var dpadY: Byte = 0 // -1, 0, 1

    fun setButtonPressed(mask: Int, pressed: Boolean) {
        while (true) {
            val current = buttonsMask.get()
            val next = if (pressed) current or mask else current and mask.inv()
            if (buttonsMask.compareAndSet(current, next)) break
        }
    }

    fun isButtonPressed(mask: Int): Boolean {
        return (buttonsMask.get() and mask) != 0
    }

    fun getButtonsMask(): Int = buttonsMask.get()

    fun reset() {
        buttonsMask.set(0)
        stickLx = 0
        stickLy = 0
        stickRx = 0
        stickRy = 0
        triggerL = 0
        triggerR = 0
        dpadX = 0
        dpadY = 0
    }
}

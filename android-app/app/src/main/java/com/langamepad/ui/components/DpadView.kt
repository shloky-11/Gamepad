package com.langamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurfaceElevated
import kotlin.math.abs

@Composable
fun DpadView(
    size: Dp = 150.dp,
    opacity: Float = 0.85f,
    onDirectionChange: (dx: Byte, dy: Byte, up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit
) {
    var pressedDirection by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        val radius = size.toPx() / 2f
                        val center = Offset(radius, radius)

                        if (change.pressed) {
                            change.consume()
                            val delta = change.position - center
                            val deadzonePx = radius * 0.20f

                            if (abs(delta.x) < deadzonePx && abs(delta.y) < deadzonePx) {
                                pressedDirection = null
                                onDirectionChange(0, 0, false, false, false, false)
                            } else {
                                val isHorizontal = abs(delta.x) > abs(delta.y) * 0.7f
                                val isVertical = abs(delta.y) > abs(delta.x) * 0.7f

                                val dx: Byte = if (isHorizontal) (if (delta.x > 0) 1 else -1).toByte() else 0
                                val dy: Byte = if (isVertical) (if (delta.y < 0) 1 else -1).toByte() else 0 // Inverted Y

                                val up = dy > 0
                                val down = dy < 0
                                val left = dx < 0
                                val right = dx > 0

                                pressedDirection = when {
                                    up && right -> "UP_RIGHT"
                                    up && left -> "UP_LEFT"
                                    down && right -> "DOWN_RIGHT"
                                    down && left -> "DOWN_LEFT"
                                    up -> "UP"
                                    down -> "DOWN"
                                    left -> "LEFT"
                                    right -> "RIGHT"
                                    else -> null
                                }

                                onDirectionChange(dx, dy, up, down, left, right)
                            }
                        } else {
                            pressedDirection = null
                            onDirectionChange(0, 0, false, false, false, false)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val armWidth = w * 0.34f
            val armLength = w
            val cornerRadius = CornerRadius(12f, 12f)

            // Horizontal Arm
            drawRoundRect(
                color = XboxSurfaceElevated.copy(alpha = opacity),
                topLeft = Offset(0f, (w - armWidth) / 2f),
                size = Size(armLength, armWidth),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = XboxBorder.copy(alpha = opacity),
                topLeft = Offset(0f, (w - armWidth) / 2f),
                size = Size(armLength, armWidth),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2f)
            )

            // Vertical Arm
            drawRoundRect(
                color = XboxSurfaceElevated.copy(alpha = opacity),
                topLeft = Offset((w - armWidth) / 2f, 0f),
                size = Size(armWidth, armLength),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = XboxBorder.copy(alpha = opacity),
                topLeft = Offset((w - armWidth) / 2f, 0f),
                size = Size(armWidth, armLength),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2f)
            )

            // Center Ring
            drawCircle(
                color = XboxBorder.copy(alpha = 0.5f),
                radius = armWidth * 0.35f,
                center = Offset(w / 2f, w / 2f),
                style = Stroke(width = 1.5f)
            )

            // Active Highlight if pressed
            if (pressedDirection != null) {
                drawCircle(
                    color = XboxGreenLuminous.copy(alpha = 0.35f),
                    radius = w * 0.42f,
                    center = Offset(w / 2f, w / 2f)
                )
            }
        }
    }
}

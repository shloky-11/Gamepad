package com.langamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurfaceElevated
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun JoystickView(
    size: Dp = 160.dp,
    deadzone: Float = 0.10f,
    sensitivity: Float = 1.0f,
    opacity: Float = 0.85f,
    onValueChange: (x: Short, y: Short) -> Unit,
    onClickThumb: () -> Unit = {}
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(deadzone, sensitivity) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        val radius = size.toPx() / 2f
                        val center = Offset(radius, radius)

                        if (change.pressed) {
                            change.consume()
                            isDragging = true

                            val rawDelta = change.position - center
                            val distance = sqrt(rawDelta.x * rawDelta.x + rawDelta.y * rawDelta.y)
                            val maxRadius = radius * 0.75f

                            val clampedOffset = if (distance > maxRadius) {
                                val angle = atan2(rawDelta.y, rawDelta.x)
                                Offset(cos(angle) * maxRadius, sin(angle) * maxRadius)
                            } else {
                                rawDelta
                            }
                            thumbOffset = clampedOffset

                            // Normalize to -1.0 .. 1.0
                            val normalizedDistance = (distance / maxRadius).coerceIn(0f, 1f)
                            if (normalizedDistance < deadzone) {
                                onValueChange(0, 0)
                            } else {
                                val effectiveDist = ((normalizedDistance - deadzone) / (1f - deadzone)) * sensitivity
                                val clampedDist = effectiveDist.coerceIn(0f, 1f)
                                val angle = atan2(rawDelta.y, rawDelta.x)

                                val normX = cos(angle) * clampedDist
                                // Android screen Y goes down; Gamepad Y goes UP, so invert Y
                                val normY = -sin(angle) * clampedDist

                                val outX = (normX * 32767).toInt().coerceIn(-32768, 32767).toShort()
                                val outY = (normY * 32767).toInt().coerceIn(-32768, 32767).toShort()
                                onValueChange(outX, outY)
                            }
                        } else {
                            // Released: Snap back to center
                            isDragging = false
                            thumbOffset = Offset.Zero
                            onValueChange(0, 0)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() / 2f - 4f
            val thumbRadius = outerRadius * 0.42f

            // Outer Base Ring
            drawCircle(
                color = XboxSurfaceElevated.copy(alpha = opacity * 0.7f),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = if (isDragging) XboxGreenLuminous.copy(alpha = 0.8f) else XboxBorder.copy(alpha = opacity),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Deadzone Guide Ring (Subtle)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = outerRadius * deadzone * 0.75f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Movable Thumbstick Knob
            val currentThumbPos = center + thumbOffset
            drawCircle(
                color = if (isDragging) XboxGreenLuminous.copy(alpha = 0.25f) else Color.Transparent,
                radius = thumbRadius + 8f,
                center = currentThumbPos
            )
            drawCircle(
                color = XboxSurfaceElevated.copy(alpha = opacity),
                radius = thumbRadius,
                center = currentThumbPos
            )
            drawCircle(
                color = if (isDragging) XboxGreenLuminous else XboxBorder,
                radius = thumbRadius,
                center = currentThumbPos,
                style = Stroke(width = 3f)
            )
            // Thumb inner grip dimple
            drawCircle(
                color = if (isDragging) XboxGreenLuminous.copy(alpha = 0.8f) else XboxBorder.copy(alpha = 0.6f),
                radius = thumbRadius * 0.45f,
                center = currentThumbPos,
                style = Stroke(width = 2f)
            )
        }
    }
}

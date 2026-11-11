package com.langamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurfaceElevated
import com.langamepad.ui.theme.XboxTextMuted
import com.langamepad.ui.theme.XboxTextPrimary

@Composable
fun AnalogTrigger(
    label: String,
    modifier: Modifier = Modifier,
    opacity: Float = 0.85f,
    onPressureChange: (pressure: Int) -> Unit
) {
    var pressureValue by remember { mutableFloatStateOf(0f) } // 0.0 .. 1.0
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(48.dp)
            .width(130.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.pressed) {
                            change.consume()
                            isPressed = true
                            // Vertical or horizontal touch depth: pressing further down yields 100%
                            val progress = (change.position.y / size.height.toFloat()).coerceIn(0.2f, 1f)
                            pressureValue = progress
                            onPressureChange((progress * 255).toInt())
                        } else {
                            isPressed = false
                            pressureValue = 0f
                            onPressureChange(0)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val radius = CornerRadius(10f, 10f)

            // Background
            drawRoundRect(
                color = XboxSurfaceElevated.copy(alpha = opacity),
                cornerRadius = radius
            )

            // Fill meter according to analog pressure
            if (pressureValue > 0f) {
                drawRoundRect(
                    color = XboxGreenLuminous.copy(alpha = 0.35f + pressureValue * 0.4f),
                    size = Size(w * pressureValue, h),
                    cornerRadius = radius
                )
            }

            // Outline
            drawRoundRect(
                color = if (isPressed) XboxGreenLuminous else XboxBorder,
                cornerRadius = radius,
                style = Stroke(width = if (isPressed) 2.5f else 1.5f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (isPressed) XboxGreenLuminous else XboxTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (isPressed) {
                Text(
                    text = " ${(pressureValue * 100).toInt()}%",
                    color = XboxTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DigitalBumper(
    label: String,
    modifier: Modifier = Modifier,
    opacity: Float = 0.85f,
    onStateChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(48.dp)
            .width(110.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                onStateChange(true)
                            }
                        } else {
                            if (isPressed) {
                                isPressed = false
                                onStateChange(false)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = CornerRadius(10f, 10f)
            drawRoundRect(
                color = if (isPressed) XboxGreenLuminous.copy(alpha = 0.4f) else XboxSurfaceElevated.copy(alpha = opacity),
                cornerRadius = radius
            )
            drawRoundRect(
                color = if (isPressed) XboxGreenLuminous else XboxBorder,
                cornerRadius = radius,
                style = Stroke(width = if (isPressed) 2.5f else 1.5f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) XboxGreenLuminous else XboxTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TriggerBumperRow(
    opacity: Float = 0.85f,
    onLtChange: (value: Int) -> Unit,
    onLbChange: (pressed: Boolean) -> Unit,
    onRbChange: (pressed: Boolean) -> Unit,
    onRtChange: (value: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: LT & LB
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalogTrigger(label = "LT", opacity = opacity, onPressureChange = onLtChange)
            DigitalBumper(label = "LB", opacity = opacity, onStateChange = onLbChange)
        }

        // Right Side: RB & RT
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DigitalBumper(label = "RB", opacity = opacity, onStateChange = onRbChange)
            AnalogTrigger(label = "RT", opacity = opacity, onPressureChange = onRtChange)
        }
    }
}

package com.langamepad.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.langamepad.ui.theme.ColorBtnA
import com.langamepad.ui.theme.ColorBtnB
import com.langamepad.ui.theme.ColorBtnX
import com.langamepad.ui.theme.ColorBtnY
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxSurfaceElevated

@Composable
fun ActionButton(
    label: String,
    accentColor: Color,
    size: Dp = 60.dp,
    opacity: Float = 0.85f,
    onStateChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = size.toPx() / 2f - 3f

            // Glow aura when pressed
            if (isPressed) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.45f),
                    radius = radius + 4f,
                    center = center
                )
            }

            // Button body
            drawCircle(
                color = if (isPressed) accentColor else XboxSurfaceElevated.copy(alpha = opacity),
                radius = radius,
                center = center
            )

            // Outer metallic rim
            drawCircle(
                color = if (isPressed) Color.White.copy(alpha = 0.9f) else accentColor.copy(alpha = 0.8f),
                radius = radius,
                center = center,
                style = Stroke(width = if (isPressed) 3.5f else 2.5f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ActionButtonCluster(
    size: Dp = 190.dp,
    buttonSize: Dp = 56.dp,
    opacity: Float = 0.85f,
    onButtonChange: (button: String, pressed: Boolean) -> Unit
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Y - North
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ActionButton(label = "Y", accentColor = ColorBtnY, size = buttonSize, opacity = opacity) {
                onButtonChange("Y", it)
            }
        }
        // A - South
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ActionButton(label = "A", accentColor = ColorBtnA, size = buttonSize, opacity = opacity) {
                onButtonChange("A", it)
            }
        }
        // X - West
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            ActionButton(label = "X", accentColor = ColorBtnX, size = buttonSize, opacity = opacity) {
                onButtonChange("X", it)
            }
        }
        // B - East
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ActionButton(label = "B", accentColor = ColorBtnB, size = buttonSize, opacity = opacity) {
                onButtonChange("B", it)
            }
        }
    }
}

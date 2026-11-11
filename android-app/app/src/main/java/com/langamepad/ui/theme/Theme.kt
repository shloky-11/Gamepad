package com.langamepad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val XboxBlack = Color(0xFF090A0C)
val XboxSurface = Color(0xFF13151A)
val XboxSurfaceElevated = Color(0xFF1B1E26)
val XboxGreen = Color(0xFF107C10)
val XboxGreenLuminous = Color(0xFF22C55E)
val XboxBorder = Color(0xFF272C38)
val XboxTextPrimary = Color(0xFFF3F4F6)
val XboxTextMuted = Color(0xFF9CA3AF)

// Action button colors
val ColorBtnA = Color(0xFF16A34A) // Emerald Green
val ColorBtnB = Color(0xFFDC2626) // Crimson Red
val ColorBtnX = Color(0xFF2563EB) // Cobalt Blue
val ColorBtnY = Color(0xFFD97706) // Amber Yellow

private val DarkColorScheme = darkColorScheme(
    primary = XboxGreenLuminous,
    onPrimary = Color.White,
    secondary = XboxGreen,
    background = XboxBlack,
    surface = XboxSurface,
    onBackground = XboxTextPrimary,
    onSurface = XboxTextPrimary,
    outline = XboxBorder
)

@Composable
fun LANGamepadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

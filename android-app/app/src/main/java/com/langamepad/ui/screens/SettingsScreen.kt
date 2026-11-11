package com.langamepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.langamepad.data.GamepadPreferences
import com.langamepad.ui.theme.XboxBlack
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurface
import com.langamepad.ui.theme.XboxTextMuted
import com.langamepad.ui.theme.XboxTextPrimary

@Composable
fun SettingsScreen(
    preferences: GamepadPreferences,
    onUpdateDeadzone: (Float) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateRightStick: (Boolean) -> Unit,
    onUpdateLeftHanded: (Boolean) -> Unit,
    onUpdateHz: (Int) -> Unit,
    onUpdateVibration: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxBlack)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTROLLER SETTINGS",
                    color = XboxTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = XboxTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(XboxSurface)
                    .border(1.dp, XboxBorder, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Deadzone
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Analog Joystick Deadzone", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(preferences.deadzone * 100).toInt()}%", color = XboxGreenLuminous, fontSize = 14.sp)
                    }
                    Slider(
                        value = preferences.deadzone,
                        onValueChange = onUpdateDeadzone,
                        valueRange = 0.02f..0.30f,
                        colors = SliderDefaults.colors(
                            thumbColor = XboxGreenLuminous,
                            activeTrackColor = XboxGreenLuminous,
                            inactiveTrackColor = XboxBorder
                        )
                    )
                }

                // Sensitivity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Analog Sensitivity", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${String.format("%.1f", preferences.sensitivity)}x", color = XboxGreenLuminous, fontSize = 14.sp)
                    }
                    Slider(
                        value = preferences.sensitivity,
                        onValueChange = onUpdateSensitivity,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = XboxGreenLuminous,
                            activeTrackColor = XboxGreenLuminous,
                            inactiveTrackColor = XboxBorder
                        )
                    )
                }

                // Opacity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Button & Joystick Opacity", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(preferences.buttonOpacity * 100).toInt()}%", color = XboxGreenLuminous, fontSize = 14.sp)
                    }
                    Slider(
                        value = preferences.buttonOpacity,
                        onValueChange = onUpdateOpacity,
                        valueRange = 0.30f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = XboxGreenLuminous,
                            activeTrackColor = XboxGreenLuminous,
                            inactiveTrackColor = XboxBorder
                        )
                    )
                }

                // Left-handed layout toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Left-Handed Layout", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Swap analog stick and ABXY button clusters", color = XboxTextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = preferences.leftHandedLayout,
                        onCheckedChange = onUpdateLeftHanded,
                        colors = SwitchDefaults.colors(checkedThumbColor = XboxGreenLuminous)
                    )
                }

                // Enable Right Joystick
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Enable Right Analog Stick", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Show twin-stick right joystick for camera aim", color = XboxTextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = preferences.enableRightJoystick,
                        onCheckedChange = onUpdateRightStick,
                        colors = SwitchDefaults.colors(checkedThumbColor = XboxGreenLuminous)
                    )
                }

                // Vibration Haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Vibration Feedback", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Haptic feedback on button presses", color = XboxTextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = preferences.vibrationFeedback,
                        onCheckedChange = onUpdateVibration,
                        colors = SwitchDefaults.colors(checkedThumbColor = XboxGreenLuminous)
                    )
                }

                // Transmission rate selection
                Column {
                    Text(text = "Transmission Rate (Hz)", color = XboxTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Higher rates offer lower input latency over 5 GHz Wi-Fi", color = XboxTextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(60, 90, 120).forEach { hz ->
                            val selected = preferences.transmissionHz == hz
                            Button(
                                onClick = { onUpdateHz(hz) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) XboxGreenLuminous else XboxBlack
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (selected) XboxGreenLuminous else XboxBorder,
                                    RoundedCornerShape(6.dp)
                                )
                            ) {
                                Text(
                                    text = "$hz Hz",
                                    color = if (selected) Color.Black else XboxTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Disconnect
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Disconnect Controller", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

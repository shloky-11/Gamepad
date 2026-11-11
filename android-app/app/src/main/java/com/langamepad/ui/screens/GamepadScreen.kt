package com.langamepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.langamepad.data.GamepadPreferences
import com.langamepad.model.GamepadState
import com.langamepad.model.ProtocolConstants
import com.langamepad.network.UdpInputTransmitter
import com.langamepad.ui.components.ActionButtonCluster
import com.langamepad.ui.components.DpadView
import com.langamepad.ui.components.JoystickView
import com.langamepad.ui.components.SystemButtonRow
import com.langamepad.ui.components.TriggerBumperRow
import com.langamepad.ui.theme.XboxBlack

@Composable
fun GamepadScreen(
    gamepadState: GamepadState,
    transmitter: UdpInputTransmitter?,
    preferences: GamepadPreferences,
    serverIp: String,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Row: Shoulder Bumpers (LB, RB) & Triggers (LT, RT)
            TriggerBumperRow(
                opacity = preferences.buttonOpacity,
                onLtChange = {
                    gamepadState.triggerL = it
                    transmitter?.requestImmediateSend()
                },
                onLbChange = {
                    gamepadState.setButtonPressed(ProtocolConstants.BTN_LB, it)
                    transmitter?.requestImmediateSend()
                },
                onRbChange = {
                    gamepadState.setButtonPressed(ProtocolConstants.BTN_RB, it)
                    transmitter?.requestImmediateSend()
                },
                onRtChange = {
                    gamepadState.triggerR = it
                    transmitter?.requestImmediateSend()
                }
            )

            // Center Top: View, Menu, Guide & Telemetry
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SystemButtonRow(
                    serverIp = serverIp,
                    currentHz = transmitter?.currentMeasuredHz ?: preferences.transmissionHz.toFloat(),
                    onBackPress = {
                        gamepadState.setButtonPressed(ProtocolConstants.BTN_BACK, it)
                        transmitter?.requestImmediateSend()
                    },
                    onStartPress = {
                        gamepadState.setButtonPressed(ProtocolConstants.BTN_START, it)
                        transmitter?.requestImmediateSend()
                    },
                    onGuidePress = {
                        gamepadState.setButtonPressed(ProtocolConstants.BTN_GUIDE, it)
                        transmitter?.requestImmediateSend()
                    },
                    onOpenSettings = onOpenSettings
                )
            }

            // Main Lower Playing Field: Left controls & Right controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leftControls: @Composable () -> Unit = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Analog Stick (L3)
                        JoystickView(
                            size = 150.dp,
                            deadzone = preferences.deadzone,
                            sensitivity = preferences.sensitivity,
                            opacity = preferences.buttonOpacity,
                            onValueChange = { x, y ->
                                gamepadState.stickLx = x
                                gamepadState.stickLy = y
                                transmitter?.requestImmediateSend()
                            },
                            onClickThumb = {
                                gamepadState.setButtonPressed(ProtocolConstants.BTN_L3, true)
                                transmitter?.requestImmediateSend()
                            }
                        )

                        // 4-Way D-Pad
                        DpadView(
                            size = 140.dp,
                            opacity = preferences.buttonOpacity,
                            onDirectionChange = { dx, dy, up, down, left, right ->
                                gamepadState.dpadX = dx
                                gamepadState.dpadY = dy
                                gamepadState.setButtonPressed(ProtocolConstants.BTN_DPAD_UP, up)
                                gamepadState.setButtonPressed(ProtocolConstants.BTN_DPAD_DOWN, down)
                                gamepadState.setButtonPressed(ProtocolConstants.BTN_DPAD_LEFT, left)
                                gamepadState.setButtonPressed(ProtocolConstants.BTN_DPAD_RIGHT, right)
                                transmitter?.requestImmediateSend()
                            }
                        )
                    }
                }

                val rightControls: @Composable () -> Unit = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right Analog Stick (Optional in Settings)
                        if (preferences.enableRightJoystick) {
                            JoystickView(
                                size = 140.dp,
                                deadzone = preferences.deadzone,
                                sensitivity = preferences.sensitivity,
                                opacity = preferences.buttonOpacity,
                                onValueChange = { x, y ->
                                    gamepadState.stickRx = x
                                    gamepadState.stickRy = y
                                    transmitter?.requestImmediateSend()
                                },
                                onClickThumb = {
                                    gamepadState.setButtonPressed(ProtocolConstants.BTN_R3, true)
                                    transmitter?.requestImmediateSend()
                                }
                            )
                        }

                        // ABXY Action Button Cluster
                        ActionButtonCluster(
                            size = 175.dp,
                            buttonSize = 52.dp,
                            opacity = preferences.buttonOpacity,
                            onButtonChange = { btn, pressed ->
                                val mask = when (btn) {
                                    "A" -> ProtocolConstants.BTN_A
                                    "B" -> ProtocolConstants.BTN_B
                                    "X" -> ProtocolConstants.BTN_X
                                    "Y" -> ProtocolConstants.BTN_Y
                                    else -> 0
                                }
                                if (mask != 0) {
                                    gamepadState.setButtonPressed(mask, pressed)
                                    transmitter?.requestImmediateSend()
                                }
                            }
                        )
                    }
                }

                if (preferences.leftHandedLayout) {
                    rightControls()
                    leftControls()
                } else {
                    leftControls()
                    rightControls()
                }
            }
        }
    }
}

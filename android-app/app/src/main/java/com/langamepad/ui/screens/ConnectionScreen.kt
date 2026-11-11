package com.langamepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.langamepad.model.DiscoveredServer
import com.langamepad.ui.theme.XboxBlack
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurface
import com.langamepad.ui.theme.XboxSurfaceElevated
import com.langamepad.ui.theme.XboxTextMuted
import com.langamepad.ui.theme.XboxTextPrimary

@Composable
fun ConnectionScreen(
    discoveredServers: List<DiscoveredServer>,
    isConnecting: Boolean,
    errorMessage: String?,
    lastPairedIp: String,
    onRefreshDiscovery: () -> Unit,
    onConnect: (ip: String, tcpPort: Int, pin: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var manualIp by remember { mutableStateOf(lastPairedIp.ifEmpty { "192.168.1." }) }
    var selectedServerForPin by remember { mutableStateOf<DiscoveredServer?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(XboxGreenLuminous)
                    )
                    Text(
                        text = "LAN GAMEPAD",
                        color = XboxTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Local Wi-Fi Controller",
                        color = XboxTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRefreshDiscovery) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = XboxTextMuted)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = XboxTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B1212))
                        .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(text = errorMessage, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Discovered PCs
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(XboxSurface)
                        .border(1.dp, XboxBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "DISCOVERED PC SERVERS",
                        color = XboxTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (discoveredServers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = XboxGreenLuminous,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Scanning local Wi-Fi for PC companion...",
                                    color = XboxTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(discoveredServers) { server ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(XboxSurfaceElevated)
                                        .border(1.dp, XboxBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedServerForPin = server
                                            enteredPin = ""
                                            showPinDialog = true
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Computer,
                                            contentDescription = null,
                                            tint = XboxGreenLuminous,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = server.name,
                                                color = XboxTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "IP: ${server.ip} · Port ${server.tcpPort}",
                                                color = XboxTextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            selectedServerForPin = server
                                            enteredPin = ""
                                            showPinDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreenLuminous),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(text = "Connect", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Manual IP Connection
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(XboxSurface)
                        .border(1.dp, XboxBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MANUAL CONNECTION",
                        color = XboxTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "If mDNS is blocked by your router, enter the IP shown in the PC server window:",
                        color = XboxTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("PC IP Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XboxGreenLuminous,
                            unfocusedBorderColor = XboxBorder,
                            focusedTextColor = XboxTextPrimary,
                            unfocusedTextColor = XboxTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            selectedServerForPin = DiscoveredServer(
                                name = "Manual PC",
                                ip = manualIp.trim()
                            )
                            enteredPin = ""
                            showPinDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, XboxGreenLuminous, RoundedCornerShape(8.dp))
                    ) {
                        Text(text = "Pair via IP & PIN", color = XboxGreenLuminous, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6-Digit PIN Entry Modal
        if (showPinDialog && selectedServerForPin != null) {
            val server = selectedServerForPin!!
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = XboxSurface,
                title = {
                    Text(
                        text = "Pair with ${server.name}",
                        color = XboxTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the 6-digit Pairing PIN displayed in the PC terminal or Web Companion:",
                            color = XboxTextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { if (it.length <= 6) enteredPin = it },
                            label = { Text("6-Digit PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = XboxGreenLuminous,
                                unfocusedBorderColor = XboxBorder,
                                focusedTextColor = XboxTextPrimary,
                                unfocusedTextColor = XboxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPinDialog = false
                            onConnect(server.ip, server.tcpPort, enteredPin)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreenLuminous),
                        enabled = enteredPin.length == 6
                    ) {
                        Text("Confirm & Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel", color = XboxTextMuted)
                    }
                }
            )
        }
    }
}

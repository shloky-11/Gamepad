package com.langamepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.langamepad.ui.theme.XboxBorder
import com.langamepad.ui.theme.XboxGreenLuminous
import com.langamepad.ui.theme.XboxSurfaceElevated
import com.langamepad.ui.theme.XboxTextMuted
import com.langamepad.ui.theme.XboxTextPrimary

@Composable
fun SystemButtonRow(
    serverIp: String,
    currentHz: Float,
    onBackPress: (Boolean) -> Unit,
    onStartPress: (Boolean) -> Unit,
    onGuidePress: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back / View
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(XboxSurfaceElevated)
                .border(1.dp, XboxBorder, RoundedCornerShape(6.dp))
                .clickable {
                    onBackPress(true)
                    // Auto-release after brief tap
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        onBackPress(false)
                    }, 100)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "VIEW", color = XboxTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Xbox Guide Logo Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(XboxSurfaceElevated)
                .border(1.5.dp, XboxGreenLuminous, CircleShape)
                .clickable {
                    onGuidePress(true)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        onGuidePress(false)
                    }, 150)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = XboxGreenLuminous,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Start / Menu
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(XboxSurfaceElevated)
                .border(1.dp, XboxBorder, RoundedCornerShape(6.dp))
                .clickable {
                    onStartPress(true)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        onStartPress(false)
                    }, 100)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "MENU", color = XboxTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Telemetry Chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1216))
                .border(1.dp, XboxBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(XboxGreenLuminous)
            )
            Text(
                text = "${currentHz.toInt()} Hz",
                color = XboxGreenLuminous,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "·",
                color = XboxTextMuted,
                fontSize = 11.sp
            )
            Text(
                text = serverIp,
                color = XboxTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = XboxTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

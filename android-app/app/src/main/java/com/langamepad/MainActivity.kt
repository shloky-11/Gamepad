package com.langamepad

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.langamepad.data.GamepadPreferences
import com.langamepad.data.SettingsRepository
import com.langamepad.model.ConnectionState
import com.langamepad.model.GamepadState
import com.langamepad.network.DiscoveryManager
import com.langamepad.network.TcpPairingClient
import com.langamepad.network.UdpInputTransmitter
import com.langamepad.ui.screens.ConnectionScreen
import com.langamepad.ui.screens.GamepadScreen
import com.langamepad.ui.screens.SettingsScreen
import com.langamepad.ui.theme.LANGamepadTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var discoveryManager: DiscoveryManager
    private lateinit var settingsRepo: SettingsRepository
    private val gamepadState = GamepadState()
    private val tcpPairingClient = TcpPairingClient()
    private var transmitter: UdpInputTransmitter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        settingsRepo = SettingsRepository(this)
        discoveryManager = DiscoveryManager(this)
        discoveryManager.startDiscovery()

        setContent {
            LANGamepadTheme {
                val preferences by settingsRepo.preferencesFlow.collectAsState(initial = GamepadPreferences())
                val discoveredServers by discoveryManager.discoveredServers.collectAsState()

                var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.Disconnected) }
                var showSettings by remember { mutableStateOf(false) }
                var isConnecting by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                when {
                    showSettings -> {
                        SettingsScreen(
                            preferences = preferences,
                            onUpdateDeadzone = { scope.launch { settingsRepo.updateDeadzone(it) } },
                            onUpdateSensitivity = { scope.launch { settingsRepo.updateSensitivity(it) } },
                            onUpdateOpacity = { scope.launch { settingsRepo.updateOpacity(it) } },
                            onUpdateRightStick = { scope.launch { settingsRepo.updateRightJoystick(it) } },
                            onUpdateLeftHanded = { scope.launch { settingsRepo.updateLeftHanded(it) } },
                            onUpdateHz = { scope.launch { settingsRepo.updateHz(it) } },
                            onUpdateVibration = { scope.launch { settingsRepo.updateVibration(it) } },
                            onDisconnect = {
                                disconnectCurrentSession()
                                connectionState = ConnectionState.Disconnected
                                showSettings = false
                            },
                            onClose = { showSettings = false }
                        )
                    }

                    connectionState is ConnectionState.Connected -> {
                        val connected = connectionState as ConnectionState.Connected
                        GamepadScreen(
                            gamepadState = gamepadState,
                            transmitter = transmitter,
                            preferences = preferences,
                            serverIp = connected.ip,
                            onOpenSettings = { showSettings = true }
                        )
                    }

                    else -> {
                        ConnectionScreen(
                            discoveredServers = discoveredServers,
                            isConnecting = isConnecting,
                            errorMessage = errorMessage,
                            lastPairedIp = preferences.lastPairedIp,
                            onRefreshDiscovery = {
                                discoveryManager.startDiscovery()
                            },
                            onConnect = { ip, tcpPort, pin ->
                                isConnecting = true
                                errorMessage = null
                                scope.launch {
                                    val result = tcpPairingClient.pairWithServer(ip, tcpPort, pin)
                                    isConnecting = false
                                    if (result.success) {
                                        settingsRepo.updateLastConnection(ip, result.udpPort)
                                        // Start UDP Input transmitter
                                        transmitter = UdpInputTransmitter(
                                            targetIp = ip,
                                            targetPort = result.udpPort,
                                            sessionKey = result.sessionKey,
                                            targetHz = preferences.transmissionHz
                                        ).apply {
                                            start(lifecycleScope, gamepadState)
                                        }
                                        connectionState = ConnectionState.Connected(
                                            serverName = "Windows PC",
                                            ip = ip
                                        )
                                    } else {
                                        errorMessage = result.errorMessage
                                    }
                                }
                            },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }

    private fun disconnectCurrentSession() {
        transmitter?.stop()
        transmitter = null
        gamepadState.reset()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopDiscovery()
        disconnectCurrentSession()
    }
}

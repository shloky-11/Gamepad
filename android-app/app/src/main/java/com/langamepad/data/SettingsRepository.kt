package com.langamepad.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lan_gamepad_settings")

data class GamepadPreferences(
    val deadzone: Float = 0.10f,
    val sensitivity: Float = 1.0f,
    val buttonOpacity: Float = 0.85f,
    val enableRightJoystick: Boolean = true,
    val leftHandedLayout: Boolean = false,
    val transmissionHz: Int = 90,
    val vibrationFeedback: Boolean = true,
    val lastPairedIp: String = "",
    val lastPairedPort: Int = 42100
)

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_DEADZONE = floatPreferencesKey("deadzone")
        val KEY_SENSITIVITY = floatPreferencesKey("sensitivity")
        val KEY_OPACITY = floatPreferencesKey("opacity")
        val KEY_RIGHT_STICK = booleanPreferencesKey("right_stick")
        val KEY_LEFT_HANDED = booleanPreferencesKey("left_handed")
        val KEY_HZ = intPreferencesKey("hz")
        val KEY_VIBRATION = booleanPreferencesKey("vibration")
        val KEY_LAST_IP = stringPreferencesKey("last_ip")
        val KEY_LAST_PORT = intPreferencesKey("last_port")
    }

    val preferencesFlow: Flow<GamepadPreferences> = context.dataStore.data.map { prefs ->
        GamepadPreferences(
            deadzone = prefs[KEY_DEADZONE] ?: 0.10f,
            sensitivity = prefs[KEY_SENSITIVITY] ?: 1.0f,
            buttonOpacity = prefs[KEY_OPACITY] ?: 0.85f,
            enableRightJoystick = prefs[KEY_RIGHT_STICK] ?: true,
            leftHandedLayout = prefs[KEY_LEFT_HANDED] ?: false,
            transmissionHz = prefs[KEY_HZ] ?: 90,
            vibrationFeedback = prefs[KEY_VIBRATION] ?: true,
            lastPairedIp = prefs[KEY_LAST_IP] ?: "",
            lastPairedPort = prefs[KEY_LAST_PORT] ?: 42100
        )
    }

    suspend fun updateDeadzone(v: Float) = context.dataStore.edit { it[KEY_DEADZONE] = v }
    suspend fun updateSensitivity(v: Float) = context.dataStore.edit { it[KEY_SENSITIVITY] = v }
    suspend fun updateOpacity(v: Float) = context.dataStore.edit { it[KEY_OPACITY] = v }
    suspend fun updateRightJoystick(v: Boolean) = context.dataStore.edit { it[KEY_RIGHT_STICK] = v }
    suspend fun updateLeftHanded(v: Boolean) = context.dataStore.edit { it[KEY_LEFT_HANDED] = v }
    suspend fun updateHz(v: Int) = context.dataStore.edit { it[KEY_HZ] = v }
    suspend fun updateVibration(v: Boolean) = context.dataStore.edit { it[KEY_VIBRATION] = v }
    suspend fun updateLastConnection(ip: String, port: Int) = context.dataStore.edit {
        it[KEY_LAST_IP] = ip
        it[KEY_LAST_PORT] = port
    }
}

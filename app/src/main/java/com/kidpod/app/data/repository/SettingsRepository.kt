package com.kidpod.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kidpod.app.domain.models.OperatingMode
import com.kidpod.app.domain.models.ParentSettings
import com.kidpod.app.utils.Constants
import com.kidpod.app.utils.Logger
import java.security.MessageDigest

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isSetupComplete(): Boolean = prefs.getBoolean(Constants.PREF_KEY_SETUP_COMPLETE, false)

    fun markSetupComplete() {
        prefs.edit().putBoolean(Constants.PREF_KEY_SETUP_COMPLETE, true).apply()
    }

    fun isPinSet(): Boolean = prefs.getBoolean(Constants.PREF_KEY_PIN_SET, false)

    fun setPin(pin: String) {
        require(pin.length in Constants.MIN_PIN_LENGTH..Constants.MAX_PIN_LENGTH) {
            "PIN must be ${Constants.MIN_PIN_LENGTH}–${Constants.MAX_PIN_LENGTH} digits"
        }
        prefs.edit()
            .putString(Constants.PREF_KEY_PIN, hashPin(pin))
            .putBoolean(Constants.PREF_KEY_PIN_SET, true)
            .apply()
        Logger.i(TAG, "Parent PIN updated")
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(Constants.PREF_KEY_PIN, null) ?: return false
        return hashPin(pin) == stored
    }

    fun getOperatingMode(): OperatingMode {
        val name = prefs.getString(Constants.PREF_KEY_OPERATING_MODE, OperatingMode.OFFLINE.name)
        return try {
            OperatingMode.valueOf(name ?: OperatingMode.OFFLINE.name)
        } catch (e: IllegalArgumentException) {
            OperatingMode.OFFLINE
        }
    }

    fun setOperatingMode(mode: OperatingMode) {
        prefs.edit().putString(Constants.PREF_KEY_OPERATING_MODE, mode.name).apply()
    }

    fun getMaxVolumePercent(): Int =
        prefs.getInt(Constants.PREF_KEY_VOLUME_LIMIT, Constants.DEFAULT_MAX_VOLUME)

    fun setMaxVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        prefs.edit().putInt(Constants.PREF_KEY_VOLUME_LIMIT, clamped).apply()
    }

    fun getWhitelistedApps(): Set<String> =
        prefs.getStringSet(Constants.PREF_KEY_WHITELISTED_APPS, emptySet()) ?: emptySet()

    fun setWhitelistedApps(packages: Set<String>) {
        prefs.edit().putStringSet(Constants.PREF_KEY_WHITELISTED_APPS, packages).apply()
    }

    fun getSettings(): ParentSettings = ParentSettings(
        operatingMode = getOperatingMode(),
        maxVolumePercent = getMaxVolumePercent(),
        whitelistedApps = getWhitelistedApps(),
        isPinSet = isPinSet()
    )

    fun resetAll() {
        prefs.edit().clear().apply()
        Logger.w(TAG, "All KidPod settings cleared")
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "SettingsRepository"
    }
}

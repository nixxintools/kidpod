package com.kidpod.app.ui.viewmodels

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kidpod.app.data.repository.SettingsRepository
import com.kidpod.app.receiver.DeviceAdminReceiver
import com.kidpod.app.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SetupStep { WELCOME, DEVICE_ADMIN, PIN_CREATE, PIN_CONFIRM, COMPLETE }

data class SetupUiState(
    val step: SetupStep = SetupStep.WELCOME,
    val isAdminActive: Boolean = false,
    val pinFirst: String = "",
    val pinError: String? = null
)

class SetupViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun checkAdminStatus(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, DeviceAdminReceiver::class.java)
        val active = dpm.isAdminActive(component)
        _uiState.update { it.copy(isAdminActive = active) }
        Logger.i(TAG, "Device Admin active: $active")
    }

    fun proceedToNextStep(context: Context) {
        val current = _uiState.value.step
        val next = when (current) {
            SetupStep.WELCOME -> SetupStep.DEVICE_ADMIN
            SetupStep.DEVICE_ADMIN -> if (_uiState.value.isAdminActive) SetupStep.PIN_CREATE else {
                checkAdminStatus(context)
                SetupStep.DEVICE_ADMIN
            }
            SetupStep.PIN_CREATE -> SetupStep.PIN_CONFIRM
            SetupStep.PIN_CONFIRM -> SetupStep.COMPLETE
            SetupStep.COMPLETE -> SetupStep.COMPLETE
        }
        _uiState.update { it.copy(step = next, pinError = null) }
    }

    fun setPinFirst(pin: String) {
        _uiState.update { it.copy(pinFirst = pin, pinError = null) }
    }

    fun confirmPin(pin: String) {
        val first = _uiState.value.pinFirst
        if (pin != first) {
            _uiState.update { it.copy(pinError = "PINs don't match. Try again.") }
            return
        }
        settingsRepository.setPin(pin)
        _uiState.update { it.copy(step = SetupStep.COMPLETE, pinError = null) }
    }

    fun completeSetup() {
        settingsRepository.markSetupComplete()
        Logger.i(TAG, "Setup completed")
    }

    companion object {
        private const val TAG = "SetupViewModel"
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(settingsRepository) as T
        }
    }
}

package com.kidpod.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kidpod.app.data.repository.SettingsRepository
import com.kidpod.app.domain.models.OperatingMode
import com.kidpod.app.domain.models.ParentSettings
import com.kidpod.app.domain.usecases.AuthenticateParentUseCase
import com.kidpod.app.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ParentUiState(
    val isAuthenticated: Boolean = false,
    val settings: ParentSettings = ParentSettings(),
    val pinError: String? = null,
    val isLockedOut: Boolean = false,
    val lockoutRemainingMs: Long = 0L,
    val showChangePinDialog: Boolean = false,
    val showResetConfirm: Boolean = false,
    val statusMessage: String? = null
)

class ParentViewModel(
    private val settingsRepository: SettingsRepository,
    private val authenticateParent: AuthenticateParentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentUiState())
    val uiState: StateFlow<ParentUiState> = _uiState.asStateFlow()

    init {
        refreshSettings()
    }

    fun authenticate(pin: String) {
        when (val result = authenticateParent(pin)) {
            is AuthenticateParentUseCase.Result.Success -> {
                _uiState.update { it.copy(isAuthenticated = true, pinError = null, isLockedOut = false) }
                refreshSettings()
            }
            is AuthenticateParentUseCase.Result.WrongPin ->
                _uiState.update { it.copy(pinError = "Wrong PIN. Try again.") }
            is AuthenticateParentUseCase.Result.LockedOut ->
                _uiState.update {
                    it.copy(
                        isLockedOut = true,
                        lockoutRemainingMs = result.remainingMillis,
                        pinError = "Too many attempts. Wait 1 minute."
                    )
                }
            is AuthenticateParentUseCase.Result.NoPinSet ->
                _uiState.update { it.copy(isAuthenticated = true, pinError = null) }
        }
    }

    fun setPin(newPin: String, confirmPin: String) {
        if (newPin != confirmPin) {
            _uiState.update { it.copy(pinError = "PINs don't match") }
            return
        }
        if (newPin.length !in 4..6) {
            _uiState.update { it.copy(pinError = "PIN must be 4–6 digits") }
            return
        }
        settingsRepository.setPin(newPin)
        _uiState.update { it.copy(showChangePinDialog = false, pinError = null, statusMessage = "PIN updated") }
    }

    fun setOperatingMode(mode: OperatingMode) {
        settingsRepository.setOperatingMode(mode)
        refreshSettings()
    }

    fun setMaxVolume(percent: Int) {
        settingsRepository.setMaxVolumePercent(percent)
        refreshSettings()
    }

    fun setWhitelistedApps(packages: Set<String>) {
        settingsRepository.setWhitelistedApps(packages)
        refreshSettings()
    }

    fun requestChangePinDialog() {
        _uiState.update { it.copy(showChangePinDialog = true, pinError = null) }
    }

    fun dismissChangePinDialog() {
        _uiState.update { it.copy(showChangePinDialog = false, pinError = null) }
    }

    fun requestResetConfirm() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun dismissResetConfirm() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun resetAllSettings() {
        settingsRepository.resetAll()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                settings = ParentSettings(),
                showResetConfirm = false,
                statusMessage = "All settings cleared"
            )
        }
        Logger.w(TAG, "Parent reset all KidPod settings")
    }

    fun lock() {
        _uiState.update { it.copy(isAuthenticated = false, pinError = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun refreshSettings() {
        _uiState.update { it.copy(settings = settingsRepository.getSettings()) }
    }

    companion object {
        private const val TAG = "ParentViewModel"
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ParentViewModel(
                settingsRepository,
                AuthenticateParentUseCase(settingsRepository)
            ) as T
        }
    }
}

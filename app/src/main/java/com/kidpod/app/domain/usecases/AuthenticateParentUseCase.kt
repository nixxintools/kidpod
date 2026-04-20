package com.kidpod.app.domain.usecases

import com.kidpod.app.data.repository.SettingsRepository
import com.kidpod.app.utils.Constants
import com.kidpod.app.utils.Logger

class AuthenticateParentUseCase(private val settingsRepository: SettingsRepository) {

    private var failedAttempts = 0
    private var lockoutUntil = 0L

    sealed class Result {
        object Success : Result()
        object WrongPin : Result()
        data class LockedOut(val remainingMillis: Long) : Result()
        object NoPinSet : Result()
    }

    operator fun invoke(pin: String): Result {
        if (!settingsRepository.isPinSet()) return Result.NoPinSet

        val now = System.currentTimeMillis()
        if (now < lockoutUntil) {
            return Result.LockedOut(lockoutUntil - now)
        }

        return if (settingsRepository.verifyPin(pin)) {
            failedAttempts = 0
            Logger.i(TAG, "Parent authenticated successfully")
            Result.Success
        } else {
            failedAttempts++
            Logger.w(TAG, "Wrong PIN attempt #$failedAttempts")
            if (failedAttempts >= Constants.MAX_PIN_ATTEMPTS) {
                lockoutUntil = now + Constants.PIN_LOCKOUT_MILLIS
                failedAttempts = 0
                Result.LockedOut(Constants.PIN_LOCKOUT_MILLIS)
            } else {
                Result.WrongPin
            }
        }
    }

    companion object {
        private const val TAG = "AuthenticateParentUseCase"
    }
}

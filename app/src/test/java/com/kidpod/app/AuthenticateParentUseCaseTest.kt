package com.kidpod.app

import com.kidpod.app.data.repository.SettingsRepository
import com.kidpod.app.domain.usecases.AuthenticateParentUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AuthenticateParentUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: AuthenticateParentUseCase

    @Before
    fun setUp() {
        settingsRepository = mock()
        whenever(settingsRepository.isPinSet()).thenReturn(true)
        useCase = AuthenticateParentUseCase(settingsRepository)
    }

    @Test
    fun `correct PIN returns Success`() {
        whenever(settingsRepository.verifyPin("1234")).thenReturn(true)
        val result = useCase("1234")
        assertTrue(result is AuthenticateParentUseCase.Result.Success)
    }

    @Test
    fun `wrong PIN returns WrongPin`() {
        whenever(settingsRepository.verifyPin("0000")).thenReturn(false)
        val result = useCase("0000")
        assertTrue(result is AuthenticateParentUseCase.Result.WrongPin)
    }

    @Test
    fun `5 wrong PINs triggers lockout`() {
        whenever(settingsRepository.verifyPin("0000")).thenReturn(false)
        repeat(4) { useCase("0000") }
        val result = useCase("0000")
        assertTrue(result is AuthenticateParentUseCase.Result.LockedOut)
    }

    @Test
    fun `no PIN set returns NoPinSet`() {
        whenever(settingsRepository.isPinSet()).thenReturn(false)
        val result = useCase("anything")
        assertTrue(result is AuthenticateParentUseCase.Result.NoPinSet)
    }
}

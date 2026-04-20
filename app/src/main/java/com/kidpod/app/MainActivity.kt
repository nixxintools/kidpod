package com.kidpod.app

import android.Manifest
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kidpod.app.receiver.DeviceAdminReceiver
import com.kidpod.app.service.LockdownService
import com.kidpod.app.ui.screens.AudiobookScreen
import com.kidpod.app.ui.screens.KidModeScreen
import com.kidpod.app.ui.screens.MusicScreen
import com.kidpod.app.ui.screens.ParentModeScreen
import com.kidpod.app.ui.screens.PlaylistScreen
import com.kidpod.app.ui.screens.SetupScreen
import com.kidpod.app.ui.theme.KidPodTheme
import com.kidpod.app.ui.viewmodels.ParentViewModel
import com.kidpod.app.ui.viewmodels.SetupViewModel
import com.kidpod.app.utils.Logger
import com.kidpod.app.utils.PermissionHelper

class MainActivity : ComponentActivity() {

    private val app get() = application as KidPodApplication

    private val setupViewModel: SetupViewModel by viewModels {
        SetupViewModel.Factory(app.settingsRepository)
    }

    private val parentViewModel: ParentViewModel by viewModels {
        ParentViewModel.Factory(app.settingsRepository)
    }

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Logger.i(TAG, "Storage permission granted: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(DevicePolicyManager::class.java)
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)

        // Start lockdown service
        startForegroundService(Intent(this, LockdownService::class.java))

        // Attempt Lock Task Mode (succeeds only if whitelisted as device owner)
        tryStartLockTask()

        // Request storage permission for Phase 2 media scanning
        if (!PermissionHelper.hasStoragePermission(this)) {
            storagePermissionLauncher.launch(PermissionHelper.requiredStoragePermission())
        }

        setContent {
            KidPodTheme {
                val navController = rememberNavController()

                val startDestination = if (app.settingsRepository.isSetupComplete()) {
                    NavRoutes.KID_MODE
                } else {
                    NavRoutes.SETUP
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(NavRoutes.SETUP) {
                        SetupScreen(
                            viewModel = setupViewModel,
                            onSetupComplete = {
                                navController.navigate(NavRoutes.KID_MODE) {
                                    popUpTo(NavRoutes.SETUP) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(NavRoutes.KID_MODE) {
                        KidModeScreen(
                            onAuthenticatedAsParent = {
                                navController.navigate(NavRoutes.PARENT_MODE)
                            },
                            onVerifyPin = { pin ->
                                app.settingsRepository.verifyPin(pin)
                            },
                            onMusicTap = { navController.navigate(NavRoutes.MUSIC) },
                            onAudiobooksTap = { navController.navigate(NavRoutes.AUDIOBOOKS) },
                            onPlaylistsTap = { navController.navigate(NavRoutes.PLAYLISTS) }
                        )
                    }

                    composable(NavRoutes.PARENT_MODE) {
                        ParentModeScreen(
                            viewModel = parentViewModel,
                            onExit = { navController.popBackStack() },
                            onExitKiosk = { exitKioskMode() },
                            isDeviceAdminActive = dpm.isAdminActive(adminComponent),
                            isLockTaskActive = isInLockTaskMode()
                        )
                    }

                    composable(NavRoutes.MUSIC) {
                        MusicScreen(onBack = { navController.popBackStack() })
                    }

                    composable(NavRoutes.AUDIOBOOKS) {
                        AudiobookScreen(onBack = { navController.popBackStack() })
                    }

                    composable(NavRoutes.PLAYLISTS) {
                        PlaylistScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-enter lock task if focus is regained and we should be locked
        if (hasFocus && app.settingsRepository.isSetupComplete()) {
            tryStartLockTask()
        }
    }

    private fun tryStartLockTask() {
        try {
            if (dpm.isLockTaskPermitted(packageName)) {
                startLockTask()
                Logger.i(TAG, "Lock Task Mode started")
            } else {
                Logger.w(TAG, "Lock Task not permitted — device owner not set. Lockdown limited to launcher-only mode.")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start Lock Task Mode", e)
        }
    }

    fun exitKioskMode() {
        try {
            stopLockTask()
            Logger.i(TAG, "Lock Task Mode stopped by parent")
        } catch (e: Exception) {
            Logger.w(TAG, "Could not stop Lock Task Mode", e)
        }
    }

    private fun isInLockTaskMode(): Boolean {
        val am = getSystemService(ActivityManager::class.java)
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

object NavRoutes {
    const val SETUP = "setup"
    const val KID_MODE = "kid_mode"
    const val PARENT_MODE = "parent_mode"
    const val MUSIC = "music"
    const val AUDIOBOOKS = "audiobooks"
    const val PLAYLISTS = "playlists"
}

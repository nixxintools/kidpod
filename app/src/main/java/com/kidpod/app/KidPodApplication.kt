package com.kidpod.app

import android.app.Application
import com.kidpod.app.data.repository.SettingsRepository
import com.kidpod.app.utils.Logger

class KidPodApplication : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        Logger.i(TAG, "KidPod application started")
    }

    companion object {
        private const val TAG = "KidPodApplication"
    }
}

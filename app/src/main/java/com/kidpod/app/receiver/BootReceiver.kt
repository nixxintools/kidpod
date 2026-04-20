package com.kidpod.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kidpod.app.service.LockdownService
import com.kidpod.app.utils.Logger

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            Logger.i(TAG, "Device booted — starting lockdown service")
            val serviceIntent = Intent(context, LockdownService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

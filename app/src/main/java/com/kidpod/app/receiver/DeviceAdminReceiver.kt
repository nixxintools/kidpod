package com.kidpod.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.kidpod.app.utils.Logger

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Logger.i(TAG, "Device Admin enabled — lockdown active")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // We can't block this (requires device owner), but we log it.
        // If the user has set up Device Admin properly as device owner,
        // disabling requires going through the parent PIN flow in settings.
        Logger.w(TAG, "Device Admin disabled — lockdown weakened")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "KidPod Device Admin cannot be disabled without the parent PIN. " +
            "Please use the Parent Settings to disable lockdown."
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
}

package com.kidpod.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kidpod.app.MainActivity
import com.kidpod.app.R
import com.kidpod.app.utils.Constants
import com.kidpod.app.utils.Logger

class LockdownService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(Constants.NOTIF_ID_LOCKDOWN, buildNotification())
        Logger.i(TAG, "Lockdown service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Logger.w(TAG, "Lockdown service stopped — attempting restart")
        // Restart ourselves — START_STICKY handles this, but belt-and-suspenders
        val restartIntent = Intent(this, LockdownService::class.java)
        startForegroundService(restartIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIF_CHANNEL_LOCKDOWN,
            "KidPod Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps KidPod running in kid mode"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Constants.NOTIF_CHANNEL_LOCKDOWN)
            .setContentTitle("KidPod is active")
            .setContentText("Kid mode is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LockdownService"
    }
}

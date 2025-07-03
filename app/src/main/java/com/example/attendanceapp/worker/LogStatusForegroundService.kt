package com.example.attendanceapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.attendanceapp.R
import com.example.attendanceapp.data.LogStatusManager
import android.app.PendingIntent
import com.example.attendanceapp.MainActivity

class LogStatusForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis = 3 * 60 * 1000L // 3 minutes
    private var isStopped = false

    private val runnable = object : Runnable {
        override fun run() {
            if (!isStopped) {
                LogStatusManager.makeImmediateLogStatusCall(applicationContext)
                handler.postDelayed(this, intervalMillis)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        isStopped = false
        handler.post(runnable)
        return START_STICKY
    }

    override fun onDestroy() {
        isStopped = true
        handler.removeCallbacks(runnable)
        showStoppedNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "log_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Log Status Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Attendance Logging")
            .setContentText("Logging status every 3 minutes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun showStoppedNotification() {
        val channelId = "log_status_stopped_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Logging Status",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for when background logging is stopped."
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Background Logging Stopped")
            .setContentText("Attendance status logging has been turned off or stopped. Tap to open app.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(3, notification)
    }
} 
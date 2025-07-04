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
import java.text.SimpleDateFormat
import java.util.Date

class LogStatusForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis = 1 * 60 * 1000L // 1 minute
    private var isStopped = false
    private val notificationId = 1

    private val runnable = object : Runnable {
        override fun run() {
            if (!isStopped) {
                LogStatusManager.makeImmediateLogStatusCall(applicationContext)
                updateTrackingNotification()
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

    override fun onCreate() {
        super.onCreate()
        // Create notification channels for background service and location tracking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "background_service_channel",
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val locationChannel = NotificationChannel(
                "location_tracking_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_MIN
            )
            locationChannel.setSound(null, null)
            locationChannel.enableVibration(false)
            locationChannel.setShowBadge(false)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(locationChannel)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "background_service_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Background Service Running")
            .setContentText("Attendance logging is active.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateTrackingNotification() {
        val channelId = "location_tracking_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val time = SimpleDateFormat("HH:mm").format(Date())
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Eagle Location Tracking")
            .setContentText("Tracking active (synced at $time)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId, notification)
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
            2,
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
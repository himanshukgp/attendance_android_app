package com.example.attendanceapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.attendanceapp.R
import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import com.example.attendanceapp.worker.LogStatusForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

object LogStatusManager {
    fun toggleLogging(context: Context, enabled: Boolean) {
        DataStoreManager.saveWorkerToggleState(context, enabled)
        if (enabled) {
            val intent = Intent(context, LogStatusForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } else {
            val intent = Intent(context, LogStatusForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private fun showStoppedNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "log_status_stopped_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Logging Status", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications for when background logging is stopped."
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Background Logging Stopped")
            .setContentText("Attendance status logging has been turned off.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }

    fun makeImmediateLogStatusCall(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationUtils = LocationUtils(context)
                val location = locationUtils.getCurrentLocation().first()
                val ssid = DeviceUtils.getCurrentSsid(context)
                val imei = DeviceUtils.getDeviceIMEI(context)
                val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val phone = DataStoreManager.getEmployeePhone(context) ?: ""
                val request = LogStatusRequest(
                    imei = imei,
                    ssid = ssid,
                    latitude = location.latitude.toString(),
                    longitude = location.longitude.toString(),
                    timestamp = timestamp,
                    phone = phone
                )
                Log.d("LogStatusManager", "Making immediate log_status API call: $request")
                val response = NetworkModule.apiService.logStatus(request)
                if (response.isSuccessful) {
                    Log.d("LogStatusManager", "Immediate log_status call successful")
                } else {
                    Log.e("LogStatusManager", "Immediate log_status call failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LogStatusManager", "Immediate log_status call failed", e)
            }
        }
    }
} 
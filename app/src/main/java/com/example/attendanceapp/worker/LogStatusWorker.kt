package com.example.attendanceapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.attendanceapp.R
import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import android.content.pm.ServiceInfo

class LogStatusWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "log_status_work_chain"
    }

    override suspend fun doWork(): Result {
        val isEnabled = DataStoreManager.getWorkerToggleState(appContext)
        if (!isEnabled) {
            Log.d("LogStatusWorker", "Worker stopped as toggle is off.")
            showStoppedNotification(appContext)
            return Result.success()
        }

        setForeground(createForegroundInfo())

        try {
            Log.d("LogStatusWorker", "Worker starting")
            val locationUtils = LocationUtils(applicationContext)
            val location = locationUtils.getCurrentLocation().first()
            val ssid = DeviceUtils.getCurrentSsid(applicationContext)
            val imei = DeviceUtils.getDeviceIMEI(applicationContext)
            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val phone = DataStoreManager.getEmployeePhone(applicationContext) ?: ""
            val request = LogStatusRequest(imei, ssid, location.latitude.toString(), location.longitude.toString(), timestamp, phone)
            
            Log.d("LogStatusWorker", "Making API call with: $request")
            val response = NetworkModule.apiService.logStatus(request)

            if (response.isSuccessful) {
                Log.d("LogStatusWorker", "Worker finished successfully, re-enqueueing.")
            } else {
                Log.e("LogStatusWorker", "API call failed with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("LogStatusWorker", "Worker failed", e)
            // Still re-enqueue even if it fails, to keep the chain alive
        } finally {
            // Re-enqueue the worker to run again in 3 minutes
            val nextWorkRequest = OneTimeWorkRequestBuilder<LogStatusWorker>()
                .setInitialDelay(3, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(nextWorkRequest)
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "log_status_worker_channel"

        val channel = NotificationChannel(channelId, "Background Logging", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setContentTitle("Attendance App")
            .setContentText("Logging your attendance status in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }

        return ForegroundInfo(2, notification, foregroundServiceType)
    }

    private fun showStoppedNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "log_status_stopped_channel"

        val channel = NotificationChannel(channelId, "Logging Status", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Notifications for when background logging is stopped."
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Background Logging Stopped")
            .setContentText("Attendance status logging has been turned off.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }
} 
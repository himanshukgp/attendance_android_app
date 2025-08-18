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

/**
 * A [CoroutineWorker] responsible for logging the user's status in the background.
 * It collects location, network information, and device details, then sends them to the server.
 * The worker re-enqueues itself to run periodically.
 *
 * @param appContext The application context.
 * @param workerParams Parameters to setup the worker, including input data.
 */
class LogStatusWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        /**
         * Unique name for the work chain, used to identify and manage this worker.
         */
        const val WORK_NAME = "log_status_work_chain"
    }

    /**
     * The main entry point for the worker. This method is called on a background thread.
     * It checks if the worker is enabled, sets up foreground service information,
     * collects data, makes an API call, and re-enqueues the worker.
     *
     * @return [Result.success] if the work finished successfully, regardless of API call outcome.
     *         The worker always re-enqueues itself.
     */
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
                Log.e("LogStatusWorker", "API call failed with code: ${'$'}{response.code()}")
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

    /**
     * Creates [ForegroundInfo] for the worker to run as a foreground service.
     * This is necessary for background work that needs to be noticeable to the user,
     * like location tracking.
     *
     * @return An instance of [ForegroundInfo] containing the notification and foreground service type.
     */
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

    /**
     * Shows a notification to the user indicating that background logging has been stopped.
     *
     * @param context The context used to create and display the notification.
     */
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
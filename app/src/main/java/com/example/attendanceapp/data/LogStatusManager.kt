package com.example.attendanceapp.data

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import com.example.attendanceapp.worker.LogStatusWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

object LogStatusManager {
    fun toggleLogging(context: Context, enabled: Boolean) {
        DataStoreManager.saveWorkerToggleState(context, enabled)
        if (enabled) {
            Log.d("LogStatusManager", "Toggle ON: Scheduling worker and making immediate API call.")
            scheduleLogStatusWorker(context)
            makeImmediateLogStatusCall(context)
        } else {
            Log.d("LogStatusManager", "Toggle OFF: Cancelling worker.")
            WorkManager.getInstance(context).cancelUniqueWork("log_status_worker")
        }
    }

    private fun scheduleLogStatusWorker(context: Context) {
        val logStatusWorkRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(
            Duration.ofHours(1)
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "log_status_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            logStatusWorkRequest
        )
    }

    private fun makeImmediateLogStatusCall(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationUtils = LocationUtils(context)
                val location = locationUtils.getCurrentLocation().first()
                val ssid = DeviceUtils.getCurrentSsid(context)
                val imei = DeviceUtils.getDeviceIMEI(context)
                val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val request = LogStatusRequest(
                    imei = imei,
                    ssid = ssid,
                    latitude = location.latitude.toString(),
                    longitude = location.longitude.toString(),
                    timestamp = timestamp
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
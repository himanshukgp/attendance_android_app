package com.example.attendanceapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter

class LogStatusWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("LogStatusWorker", "Worker starting")
        try {
            val locationUtils = LocationUtils(applicationContext)
            Log.d("LogStatusWorker", "Fetching location...")
            val location = locationUtils.getCurrentLocation().first()
            Log.d("LogStatusWorker", "Location fetched: $location")

            Log.d("LogStatusWorker", "Fetching SSID...")
            val ssid = DeviceUtils.getCurrentSsid(applicationContext)
            Log.d("LogStatusWorker", "SSID fetched: $ssid")

            Log.d("LogStatusWorker", "Fetching device ID...")
            val imei = DeviceUtils.getDeviceIMEI(applicationContext)
            Log.d("LogStatusWorker", "Device ID fetched: $imei")

            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            Log.d("LogStatusWorker", "Timestamp: $timestamp")

            val request = LogStatusRequest(
                imei = imei,
                ssid = ssid,
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString(),
                timestamp = timestamp
            )
            
            Log.d("LogStatusWorker", "Making API call with: $request")
            val response = NetworkModule.apiService.logStatus(request)

            return if (response.isSuccessful) {
                Log.d("LogStatusWorker", "Worker finished successfully")
                Result.success()
            } else {
                Log.e("LogStatusWorker", "API call failed with code: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("LogStatusWorker", "Worker failed", e)
            return Result.failure()
        }
    }
} 
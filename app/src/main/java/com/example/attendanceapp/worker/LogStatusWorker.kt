package com.example.attendanceapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogStatusWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val imei = DeviceUtils.getDeviceIMEI(applicationContext)
            val ssid = DeviceUtils.getCurrentSsid(applicationContext)
            val lat = EmployeeDataManager.getLocation().split(",").getOrNull(0)?.trim() ?: ""
            val lon = EmployeeDataManager.getLocation().split(",").getOrNull(1)?.trim() ?: ""
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val req = LogStatusRequest(
                imei = imei,
                ssid = ssid,
                latitude = lat,
                longitude = lon,
                timestamp = timestamp
            )
            NetworkModule.apiService.logStatus(req)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 
package com.example.attendanceapp.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat

object DeviceUtils {
    
    @SuppressLint("HardwareIds")
    fun getDeviceIMEI(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Log.e("DeviceUtils", "Error getting ANDROID_ID", e)
            "4f233e3a6d232212" // Fallback ID
        }
    }
    
    @SuppressLint("HardwareIds")
    fun getDeviceIMEI2(context: Context): String {
        // Return the same ID for simplicity, or generate a different one if needed
        return getDeviceIMEI(context)
    }

    fun getCurrentSsid(context: Context): String {
        Log.d("DeviceUtils", "Attempting to get current SSID")
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo
            Log.d("DeviceUtils", "ConnectionInfo: $connectionInfo")

            @Suppress("DEPRECATION")
            val ssid = connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")
            Log.d("DeviceUtils", "Raw SSID from connectionInfo: $ssid")

            if (ssid.equals("<unknown ssid>", ignoreCase = true)) {
                Log.w("DeviceUtils", "SSID is '<unknown ssid>', returning empty string.")
                ""
            } else {
                Log.d("DeviceUtils", "Returning SSID: ${ssid ?: "null"}")
                ssid ?: ""
            }
        } catch (e: Exception) {
            Log.e("DeviceUtils", "Error getting SSID", e)
            ""
        }
    }
} 
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
        return try {
            val connectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val network = connectivityManager.activeNetwork ?: return ""
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ""

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    (capabilities.transportInfo as? WifiInfo)?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: ""
                } else {
                    ""
                }
            } else {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: ""
            }
        } catch (e: Exception) {
            Log.e("DeviceUtils", "Error getting SSID", e)
            ""
        }
    }
} 
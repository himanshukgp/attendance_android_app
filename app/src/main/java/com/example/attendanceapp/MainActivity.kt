package com.example.attendanceapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.attendanceapp.navigation.AppNavigation
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.api.OrgLoginResponse
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.attendanceapp.data.OrgDataManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.content.Intent
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.os.PowerManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification and location permissions for Android 10+
        val fineGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!fineGranted || !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !bgGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                1002
            )
        } else {
            // All permissions granted, proceed as normal
        }

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
        val onboardingJustCompleted = prefs.getBoolean("onboarding_just_completed", false)
        val employeeJson = DataStoreManager.getEmployee(this)
        val orgJson = DataStoreManager.getOrg(this)
        val startDestination = if (!onboardingComplete) {
            "onboarding"
        } else if (employeeJson != null) {
            "employeeAccount"
        } else if (orgJson != null) {
            try {
                val orgData = Gson().fromJson(orgJson, OrgLoginResponse::class.java)
                OrgDataManager.setOrgData(orgData)
                "orgDashboard"
            } catch (e: Exception) {
                "login"
            }
        } else {
            "login"
        }
        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
        // Only request permissions if onboarding was just completed
        if (onboardingJustCompleted) {
            prefs.edit().putBoolean("onboarding_just_completed", false).apply()
            requestAllPermissions()
        }

        // Resume logging if toggle is ON and location permission is granted
        if (com.example.attendanceapp.data.DataStoreManager.getWorkerToggleState(this)) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, com.example.attendanceapp.worker.LogStatusForegroundService::class.java)
                ContextCompat.startForegroundService(this, intent)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    1002
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val fineGranted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        1002
                    )
                } else if (com.example.attendanceapp.data.DataStoreManager.getWorkerToggleState(this)) {
                    val intent = Intent(this, com.example.attendanceapp.worker.LogStatusForegroundService::class.java)
                    ContextCompat.startForegroundService(this, intent)
                }
            }
        } else if (requestCode == 1002) {
            val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (com.example.attendanceapp.data.DataStoreManager.getWorkerToggleState(this)) {
                    val intent = Intent(this, com.example.attendanceapp.worker.LogStatusForegroundService::class.java)
                    ContextCompat.startForegroundService(this, intent)
                }
            } else if (!bgGranted) {
                // Show dialog to guide user to system settings for 'Allow all the time'
                AlertDialog.Builder(this)
                    .setTitle("Background Location Required")
                    .setMessage("To enable full background tracking, please allow 'All the time' location access in system settings.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            1000
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AttendanceAppTheme {
        Greeting("Android")
    }
}
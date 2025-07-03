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

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Prompt user to disable battery optimization for this app
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Disable Battery Optimization")
                .setMessage("To ensure background logging works reliably, please disable battery optimization for AttendanceApp.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val employeeJson = DataStoreManager.getEmployee(this)
        val orgJson = DataStoreManager.getOrg(this)

        val startDestination = if (employeeJson != null) {
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
        if (requestCode == 1002) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (com.example.attendanceapp.data.DataStoreManager.getWorkerToggleState(this)) {
                    val intent = Intent(this, com.example.attendanceapp.worker.LogStatusForegroundService::class.java)
                    ContextCompat.startForegroundService(this, intent)
                }
            }
        }
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
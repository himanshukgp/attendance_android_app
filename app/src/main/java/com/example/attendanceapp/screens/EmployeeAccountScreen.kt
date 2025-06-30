package com.example.attendanceapp.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.work.*
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import com.example.attendanceapp.worker.LogStatusWorker
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.attendanceapp.data.DataStoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeAccountScreen(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(false) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var currentSsid by remember { mutableStateOf("") }
    val currentDeviceId by remember { mutableStateOf(DeviceUtils.getDeviceIMEI(context)) }
    var locationError by remember { mutableStateOf("") }

    val locationUtils = remember { LocationUtils(context) }
    val coroutineScope = rememberCoroutineScope()

    val openLocationSettings = {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    val requestLocationUpdates = {
        if (!locationUtils.isLocationEnabled()) {
            locationError = "Location services are disabled."
        } else {
            coroutineScope.launch {
                locationUtils.getCurrentLocation()
                    .catch { e -> locationError = "Error: ${e.message}" }
                    .collect { location ->
                        currentLocation = location
                        currentSsid = DeviceUtils.getCurrentSsid(context)
                        locationError = ""
                    }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocationUpdates()
        } else {
            locationError = "Location permission denied."
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isLoggingEnabled, onCheckedChange = {
                        isLoggingEnabled = it
                        if (it) {
                            Log.d("EmployeeAccountScreen", "Toggle ON: Scheduling worker.")
                            scheduleLogStatusWorker(context)
                        } else {
                            Log.d("EmployeeAccountScreen", "Toggle OFF: Cancelling worker.")
                            WorkManager.getInstance(context).cancelUniqueWork("log_status_worker")
                        }
                    }, modifier = Modifier.height(20.dp))
                    IconButton(onClick = { /* TODO: Refresh action */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    onClick = { navController.navigate("employeeAccount") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    selected = currentRoute == "attendanceDetail",
                    icon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Attendance") },
                    label = { Text("Attendance") },
                    onClick = { navController.navigate("attendanceDetail") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendarSummary",
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Summary") },
                    label = { Text("Summary") },
                    onClick = { navController.navigate("calendarSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("🧑 ${EmployeeDataManager.getEmployeeName()}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))

            if (locationError.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = locationError,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (locationError.contains("disabled")) {
                            Button(onClick = openLocationSettings) {
                                Text("Enable")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // Location Verification
            val orgLocation = EmployeeDataManager.getEmployeeData()?.let {
                val lat = it.orgLat.toDoubleOrNull()
                val lon = it.orgLon.toDoubleOrNull()
                if (lat != null && lon != null) Location("").apply { latitude = lat; longitude = lon } else null
            }
            val distance = currentLocation?.let { orgLocation?.distanceTo(it) }
            val isLocationOk = distance != null && distance < 10

            AccountInfoRow(
                icon = if (isLocationOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (isLocationOk) Color.Green else Color.Red,
                line1 = "Location: ${currentLocation?.latitude?.format(4) ?: "N/A"}, ${currentLocation?.longitude?.format(4) ?: "N/A"}",
                line2 = "Org Location: ${orgLocation?.latitude?.format(4) ?: "N/A"}, ${orgLocation?.longitude?.format(4) ?: "N/A"}"
            )

            // Device ID Verification
            val isDeviceIdOk = currentDeviceId == EmployeeDataManager.getDeviceId()
            AccountInfoRow(
                icon = if (isDeviceIdOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (isDeviceIdOk) Color.Green else Color.Red,
                line1 = "Device ID:",
                line2 = currentDeviceId
            )

            // WiFi Verification
            val isWifiOk = currentSsid.isNotEmpty() && currentSsid == EmployeeDataManager.getOrgSSID()
            AccountInfoRow(
                icon = if (isWifiOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (isWifiOk) Color.Green else Color.Red,
                line1 = "WiFi: ${if (currentSsid.isEmpty()) "Not connected" else currentSsid}",
                line2 = "Org SSID: ${EmployeeDataManager.getOrgSSID()}"
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    EmployeeDataManager.clearEmployeeData()
                    DataStoreManager.clearEmployee(context)
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun AccountInfoRow(
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    line1: String,
    line2: String? = null
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Text(line1, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (line2 != null) {
                Text(line2, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun scheduleLogStatusWorker(context: Context) {
    Log.d("EmployeeAccountScreen", "Scheduling log status worker")
    val workRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(1, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
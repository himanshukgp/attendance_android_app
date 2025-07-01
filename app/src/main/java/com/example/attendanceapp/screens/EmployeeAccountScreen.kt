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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.attendanceapp.data.DataStoreManager
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.attendanceapp.api.EmployeeLoginRequest
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.screens.EmployeeTopBar
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeAccountScreen(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var currentSsid by remember { mutableStateOf("") }
    val currentDeviceId by remember { mutableStateOf(DeviceUtils.getDeviceIMEI(context)) }
    var locationError by remember { mutableStateOf("") }

    val locationUtils = remember { LocationUtils(context) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize toggle state from DataStore
    LaunchedEffect(Unit) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
        if (EmployeeDataManager.getEmployeeData() == null) {
            DataStoreManager.getEmployee(context)?.let { json ->
                try {
                    val employee = Gson().fromJson(json, com.example.attendanceapp.api.EmployeeLoginResponse::class.java)
                    EmployeeDataManager.setEmployeeData(employee)
                    Log.d("EmployeeAccountScreen", "Loaded employee data from DataStore")
                } catch (e: Exception) {
                    Log.e("EmployeeAccountScreen", "Failed to parse employee data from DataStore", e)
                }
            }
        }
    }

    val refreshData = {
        coroutineScope.launch {
            try {
                Log.d("EmployeeAccountScreen", "Starting employee data refresh...")
                var phone = DataStoreManager.getEmployeePhone(context)
                Log.d("EmployeeAccountScreen", "Primary phone from DataStore: $phone")
                if (phone == null || phone == "Unknown") {
                    phone = EmployeeDataManager.getPhoneNumber()
                    Log.d("EmployeeAccountScreen", "Fallback phone from EmployeeDataManager: $phone")
                    if (phone == "Unknown" || phone == null) {
                        DataStoreManager.getEmployee(context)?.let { json ->
                            try {
                                val employee = Gson().fromJson(json, com.example.attendanceapp.api.EmployeeLoginResponse::class.java)
                                phone = employee.phoneNumber
                                Log.d("EmployeeAccountScreen", "Fetched phone from employee JSON: $phone")
                            } catch (e: Exception) {
                                Log.e("EmployeeAccountScreen", "Failed to fetch phone from employee JSON", e)
                            }
                        }
                    }
                }
                var imei = EmployeeDataManager.getDeviceId()
                if (imei == "Unknown" || imei == null) {
                    DataStoreManager.getEmployee(context)?.let { json ->
                        try {
                            val employee = Gson().fromJson(json, com.example.attendanceapp.api.EmployeeLoginResponse::class.java)
                            imei = employee.imei1
                            Log.d("EmployeeAccountScreen", "Fetched imei from employee JSON: $imei")
                        } catch (e: Exception) {
                            Log.e("EmployeeAccountScreen", "Failed to fetch imei from employee JSON", e)
                        }
                    }
                }
                if (phone != null && phone != "Unknown" && imei != null && imei != "Unknown") {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val request = EmployeeLoginRequest(phone = phone, selected_date = currentDate, imei = imei)
                    Log.d("EmployeeAccountScreen", "Making API call with phone: $phone, date: $currentDate, imei: $imei")
                    val response = NetworkModule.apiService.employeeLogin(request)
                    EmployeeDataManager.setEmployeeData(response)
                    val employeeJson = Gson().toJson(response)
                    DataStoreManager.saveEmployee(context, employeeJson)
                    DataStoreManager.saveEmployeePhone(context, phone)
                    Log.d("EmployeeAccountScreen", "Employee data refresh completed successfully")
                } else {
                    Log.w("EmployeeAccountScreen", "Cannot refresh: phone=$phone, imei=$imei (null or Unknown)")
                    val emp = EmployeeDataManager.getEmployeeData()
                    Log.w("EmployeeAccountScreen", "EmployeeDataManager.getEmployeeData(): $emp")
                }
            } catch (e: Exception) {
                Log.e("EmployeeAccountScreen", "Failed to refresh employee data", e)
            }
        }
    }

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
            EmployeeTopBar(
                title = "Account",
                isLoggingEnabled = isLoggingEnabled,
                onToggleChanged = {
                    isLoggingEnabled = it
                    DataStoreManager.saveWorkerToggleState(context, it)
                    if (it) {
                        Log.d("EmployeeAccountScreen", "Toggle ON: Scheduling worker.")
                        scheduleLogStatusWorker(context)
                    } else {
                        Log.d("EmployeeAccountScreen", "Toggle OFF: Cancelling worker.")
                        WorkManager.getInstance(context).cancelUniqueWork("log_status_worker")
                    }
                },
                onRefresh = { refreshData() }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(48.dp)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Account") },
                    onClick = { navController.navigate("employeeAccount") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    selected = currentRoute == "attendanceDetail",
                    icon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Attendance") },
                    onClick = {
                        val today = SimpleDateFormat("dd/MM/yyyy").format(Date())
                        val encodedDate = URLEncoder.encode(today, "UTF-8")
                        navController.navigate("attendanceDetail/$encodedDate") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendarSummary",
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Summary") },
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
    val logStatusWorkRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(
        java.time.Duration.ofHours(1)
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        logStatusWorkRequest
    )
}
package com.example.attendanceapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.utils.DeviceUtils
import com.example.attendanceapp.utils.LocationUtils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeAccountScreen(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isOff by remember { mutableStateOf(true) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val currentSsid by remember { mutableStateOf(DeviceUtils.getCurrentSsid(context)) }
    val currentDeviceId by remember { mutableStateOf(DeviceUtils.getDeviceIMEI(context)) }

    val locationUtils = remember { LocationUtils(context) }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                locationUtils.getCurrentLocation()
                    .catch { e -> println("Error getting location: ${e.message}") }
                    .collect { location ->
                        currentLocation = location
                    }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            coroutineScope.launch {
                locationUtils.getCurrentLocation()
                    .catch { e -> println("Error getting location: ${e.message}") }
                    .collect { location ->
                        currentLocation = location
                    }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.Bold) },
                actions = {
                    Text("OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isOff, onCheckedChange = { isOff = it }, modifier = Modifier.height(20.dp))
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
            val isWifiOk = currentSsid == EmployeeDataManager.getOrgSSID()
            AccountInfoRow(
                icon = if (isWifiOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                iconTint = if (isWifiOk) Color.Green else Color.Red,
                line1 = "WiFi: $currentSsid",
                line2 = "Org SSID: ${EmployeeDataManager.getOrgSSID()}"
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    EmployeeDataManager.clearEmployeeData()
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
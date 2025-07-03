package com.example.attendanceapp.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.OrgDataManager
import com.example.attendanceapp.worker.LogStatusWorker
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.rememberCoroutineScope
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.OrgLoginRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgDashboardScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData = OrgDataManager.getOrgData()
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    val coroutineScope = rememberCoroutineScope()

    val refreshData = {
        coroutineScope.launch {
            try {
                Log.d("OrgDashboardScreen", "Starting organization data refresh...")
                val phone = DataStoreManager.getOrgPhone(context)
                if (phone != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val request = OrgLoginRequest(phone = phone, selected_date = currentDate)
                    Log.d("OrgDashboardScreen", "Making API call with phone: $phone, date: $currentDate")
                    val response = NetworkModule.apiService.orgLogin(request)
                    OrgDataManager.setOrgData(response)
                    val orgJson = Gson().toJson(response)
                    DataStoreManager.saveOrg(context, orgJson)
                    Log.d("OrgDashboardScreen", "Organization data refresh completed successfully")
                } else {
                    Log.w("OrgDashboardScreen", "Cannot refresh: organization phone not found")
                }
            } catch (e: Exception) {
                Log.e("OrgDashboardScreen", "Failed to refresh organization data", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(orgData?.orgName ?: "Organization Account", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = {
                            isLoggingEnabled = it
                            com.example.attendanceapp.data.LogStatusManager.toggleLogging(context, it)
                        },
                        modifier = Modifier.height(20.dp)
                    )
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            OrgBottomBar(navController, currentRoute)
        }
    ) { innerPadding ->
        if (orgData != null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                InfoRow("Admin Title", orgData.orgAdminTitle)
                InfoRow("SSID", orgData.orgSsid)
                InfoRow("Location", "${orgData.orgLat}, ${orgData.orgLon}")
                InfoRow("Total Employees", orgData.employeeCount.toString())
                InfoRow("Present Today", orgData.employeePresent.toString())
                InfoRow("Absent Today", orgData.employeeAbsent.toString())
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        OrgDataManager.clearOrgData()
                        DataStoreManager.clearOrg(context)
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Log Out", color = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("No organization data found. Please log in again.")
                Button(onClick = { navController.navigate("login") }) {
                    Text("Go to Login")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun scheduleOrgLogStatusWorker(context: Context) {
    val logStatusWorkRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(
        java.time.Duration.ofHours(1)
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker_org",
        ExistingPeriodicWorkPolicy.KEEP,
        logStatusWorkRequest
    )
}

@Preview(showBackground = true)
@Composable
fun OrgDashboardScreenPreview() {
    val navController = rememberNavController()
    OrgDashboardScreen(navController = navController)
}


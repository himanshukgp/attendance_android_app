package com.example.attendanceapp.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.attendanceapp.data.OrgDataManager
import com.example.attendanceapp.worker.LogStatusWorker
import java.util.concurrent.TimeUnit
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgSummaryScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData = OrgDataManager.getOrgData()
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(orgData?.orgName ?: "Organization Summary", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = {
                            isLoggingEnabled = it
                            if (it) {
                                Log.d("OrgSummaryScreen", "Toggle ON: Scheduling worker.")
                                scheduleOrgSummaryLogStatusWorker(context)
                            } else {
                                Log.d("OrgSummaryScreen", "Toggle OFF: Cancelling worker.")
                                WorkManager.getInstance(context).cancelUniqueWork("log_status_worker_org")
                            }
                        },
                        modifier = Modifier.height(20.dp)
                    )
                    IconButton(onClick = { /* TODO: Refresh action */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            OrgBottomBar(navController, currentRoute)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SummaryInfo(label = "Total Employees", value = 15)
            Spacer(modifier = Modifier.height(16.dp))
            SummaryInfo(label = "Present", value = 8, color = Color.Green)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryInfo(label = "Absent", value = 7, color = Color.Red)
        }
    }
}

@Composable
fun SummaryInfo(label: String, value: Int, color: Color = Color.Gray) {
    Text(
        text = "$label: $value",
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

private fun scheduleOrgSummaryLogStatusWorker(context: Context) {
    Log.d("OrgSummaryScreen", "Scheduling organization log status worker")
    val workRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(1, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker_org",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}

@Preview(showBackground = true)
@Composable
fun OrgSummaryScreenPreview() {
    val navController = rememberNavController()
    OrgSummaryScreen(navController = navController)
}


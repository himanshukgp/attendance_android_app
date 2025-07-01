package com.example.attendanceapp.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.attendanceapp.data.DataStoreManager
import androidx.compose.runtime.rememberCoroutineScope
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.OrgLoginRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmployeeAttendance(
    val name: String,
    val loginTime: String,
    val totalTime: String,
    val status: List<Char>,
    val timeline: List<Pair<Color, Float>>
)

val employeeData = listOf(
    EmployeeAttendance(
        name = "AKSHAYKRISHNAN KA",
        loginTime = "11:22:14",
        totalTime = "07:08",
        status = listOf('P', 'H', 'A'),
        timeline = listOf(
            Color.Green to 0.2f, Color.Red to 0.1f, Color.Green to 0.4f, Color.Red to 0.1f, Color.Green to 0.2f
        )
    ),
    EmployeeAttendance(
        name = "JayaCharan",
        loginTime = "11:20:12",
        totalTime = "04:04",
        status = listOf('P', 'H', 'A'),
        timeline = listOf(Color.Green to 0.2f, Color.Red to 0.1f, Color.Green to 0.7f)
    ),
    EmployeeAttendance(
        name = "SRUSHTI GANAMUKHI",
        loginTime = "11:20:12",
        totalTime = "07:10",
        status = listOf('H'),
        timeline = listOf(
            Color.Green to 0.3f, Color.Red to 0.1f, Color.Green to 0.1f, Color.Red to 0.2f, Color.Green to 0.3f
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgAttendanceScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData = OrgDataManager.getOrgData()
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize toggle state from DataStore
    LaunchedEffect(Unit) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
    }

    val refreshData = {
        coroutineScope.launch {
            try {
                Log.d("OrgAttendanceScreen", "Starting organization data refresh...")
                val phone = DataStoreManager.getOrgPhone(context)
                if (phone != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val request = OrgLoginRequest(phone = phone, selected_date = currentDate)
                    Log.d("OrgAttendanceScreen", "Making API call with phone: $phone, date: $currentDate")
                    val response = NetworkModule.apiService.orgLogin(request)
                    OrgDataManager.setOrgData(response)
                    val orgJson = Gson().toJson(response)
                    DataStoreManager.saveOrg(context, orgJson)
                    Log.d("OrgAttendanceScreen", "Organization data refresh completed successfully")
                } else {
                    Log.w("OrgAttendanceScreen", "Cannot refresh: organization phone not found")
                }
            } catch (e: Exception) {
                Log.e("OrgAttendanceScreen", "Failed to refresh organization data", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(orgData?.orgName ?: "Organization Attendance", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = {
                            isLoggingEnabled = it
                            DataStoreManager.saveWorkerToggleState(context, it)
                            if (it) {
                                Log.d("OrgAttendanceScreen", "Toggle ON: Scheduling worker.")
                                scheduleOrgLogStatusWorker(context)
                            } else {
                                Log.d("OrgAttendanceScreen", "Toggle OFF: Cancelling worker.")
                                WorkManager.getInstance(context).cancelUniqueWork("log_status_worker_org")
                            }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            FilterControls()
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(employeeData) { employee ->
                    EmployeeAttendanceCard(employee = employee)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FilterControls() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterItem(label = "Name", value = "All")
        FilterItem(label = "Month", value = "June 2025")
        FilterItem(label = "Date", value = "All")
    }
}

@Composable
fun FilterItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }
    }
}

@Composable
fun EmployeeAttendanceCard(employee: EmployeeAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusIcons(status = employee.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Login Time: ${employee.loginTime}", fontSize = 14.sp, color = Color.Gray)
            Text("Total Time: ${employee.totalTime}", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            TimelineBar(data = employee.timeline, height = 8.dp)
        }
    }
}

@Composable
fun StatusIcons(status: List<Char>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // This is a simplified representation based on the image
        val pColor = if (status.contains('A')) Color.Gray else MaterialTheme.colorScheme.surfaceVariant
        val hColor = if (status.contains('H')) Color(0xFFFFA500) else MaterialTheme.colorScheme.surfaceVariant
        val aColor = if (status.contains('A')) Color.Red else MaterialTheme.colorScheme.surfaceVariant

        StatusIcon(char = 'P', color = pColor)
        StatusIcon(char = 'H', color = hColor)
        StatusIcon(char = 'A', color = aColor)
    }
}

@Composable
fun StatusIcon(char: Char, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(char.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

//@Composable
//fun TimelineBar(data: List<Pair<Color, Float>>, height: Dp) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(height)
//            .clip(CircleShape)
//    ) {
//        data.forEach { (color, weight) ->
//            Box(
//                modifier = Modifier
//                    .background(color)
//                    .weight(weight)
//                    .fillMaxHeight()
//            )
//        }
//    }
//}

//@Composable
//fun OrgBottomBar(navController: NavController, currentRoute: String?) {
//    NavigationBar {
//        NavigationBarItem(
//            selected = currentRoute == "orgDashboard",
//            icon = { Icon(Icons.Default.Person, contentDescription = "Dashboard") },
//            label = { Text("Dashboard") },
//            onClick = { navController.navigate("orgDashboard") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//        NavigationBarItem(
//            selected = currentRoute == "orgAttendance",
//            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
//            label = { Text("Attendance") },
//            onClick = { navController.navigate("orgAttendance") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//        NavigationBarItem(
//            selected = currentRoute == "orgSummary",
//            icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
//            label = { Text("Summary") },
//            onClick = { navController.navigate("orgSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//    }
//}

@Preview(showBackground = true)
@Composable
fun OrgAttendanceScreenPreview() {
    val navController = rememberNavController()
    OrgAttendanceScreen(navController = navController)
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


package com.example.attendanceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.worker.LogStatusWorker
import java.util.concurrent.TimeUnit
import android.util.Log

data class UiShift(
    val name: String,
    val startTime: String,
    val inOfficeDuration: String,
    val endTime: String,
    val outOfOfficeDuration: String?
)

// Sample data based on the image - will be replaced with actual API data
val sampleShifts = listOf(
    UiShift("Shift 1", "11:40:44", "2.06 hrs", "13:44:12", null),
    UiShift("Shift 2", "14:56:12", "0.48 hrs", "15:25:00", null),
    UiShift("Shift 3", "17:20:12", "0.24 hrs", "17:34:36", null),
    UiShift("Shift 4", "17:47:34", "0.29 hrs", "18:04:51", null),
    UiShift("Shift 5", "18:14:55", "0.17 hrs", "18:25:00", null),
    UiShift("Shift 6", "18:27:22", "0.06 hrs", "18:30:55", null)
)

val timelineData = listOf(
    Color.Green to 0.4f,
    Color.Red to 0.2f,
    Color.Green to 0.1f,
    Color.Red to 0.25f,
    Color.Green to 0.05f
)

@Composable
fun getShiftsFromApiData(): List<UiShift> {
    val apiShifts = EmployeeDataManager.getShifts()
    return if (!apiShifts.isNullOrEmpty()) {
        apiShifts.map { (shiftName, shiftData) ->
            UiShift(
                name = shiftName,
                startTime = shiftData.inn,
                inOfficeDuration = "${shiftData.ti} hrs",
                endTime = shiftData.out,
                outOfOfficeDuration = "Out for ${shiftData.to} hrs"
            )
        }
    } else {
        // fallback sample data
        listOf()
    }
}

@Composable
fun getTimelineDataFromApi(): List<Pair<Color, Float>> {
    val apiShifts = EmployeeDataManager.getShifts()
    return if (!apiShifts.isNullOrEmpty()) {
        // Create timeline based on actual shift data
        val timeline = mutableListOf<Pair<Color, Float>>()
        apiShifts.values.forEach { shift ->
            // Add green for in-office time
            timeline.add(Color.Green to (shift.ti.toFloatOrNull() ?: 0f) / 8f)
            // Add red for out-of-office time
            timeline.add(Color.Red to (shift.to.toFloatOrNull() ?: 0f) / 8f)
        }
        timeline
    } else {
        timelineData // Fallback to sample data
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = {
                            isLoggingEnabled = it
                            if (it) {
                                Log.d("AttendanceDetailScreen", "Toggle ON: Scheduling worker.")
                                scheduleLogStatusWorker(context)
                            } else {
                                Log.d("AttendanceDetailScreen", "Toggle OFF: Cancelling worker.")
                                WorkManager.getInstance(context).cancelUniqueWork("log_status_worker")
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
            // Re-using the navigation logic from the other screen
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    onClick = {
                        navController.navigate("employeeAccount") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    selected = currentRoute == "attendanceDetail",
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Attendance") },
                    label = { Text("Attendance") },
                    onClick = {
                        navController.navigate("attendanceDetail") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendarSummary",
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
                    label = { Text("Summary") },
                    onClick = {
                        navController.navigate("calendarSummary") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AttendanceInfoBar()
            Spacer(modifier = Modifier.height(16.dp))
            TimelineBar(data = getTimelineDataFromApi(), height = 12.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Legend()
            Spacer(modifier = Modifier.height(16.dp))
            ShiftList(shifts = getShiftsFromApiData())
        }
    }
}

@Composable
fun AttendanceInfoBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("In: ${EmployeeDataManager.getLoginTime()}", fontSize = 14.sp)
        Text("Hrs: ${EmployeeDataManager.getCurrentHours()}", fontSize = 14.sp, color = Color.Gray)
        Text("Jun 20, 2025", fontSize = 14.sp, color = Color.Gray)
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

@Composable
fun Legend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = Color.Green, text = "In Office")
        LegendItem(color = Color.Red, text = "Out of Office")
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun ShiftList(shifts: List<UiShift>) {
    LazyColumn {
        items(shifts) { shift ->
            ShiftItem(shift = shift)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ShiftItem(shift: UiShift) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(shift.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(shift.startTime, color = Color.Gray, fontSize = 14.sp)
                Text("----${shift.inOfficeDuration}----", color = Color.Gray, fontSize = 12.sp)
                Text(shift.endTime, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        shift.outOfOfficeDuration?.let {
            Text(
                text = it,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AttendanceDetailScreenPreview() {
    // We need a NavController for the preview.
    val navController = rememberNavController()
    AttendanceDetailScreen(navController = navController)
}

private fun scheduleLogStatusWorker(context: android.content.Context) {
    Log.d("AttendanceDetailScreen", "Scheduling log status worker")
    val workRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(1, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}


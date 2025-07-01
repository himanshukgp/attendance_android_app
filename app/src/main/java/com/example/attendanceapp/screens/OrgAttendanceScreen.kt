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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

// Data classes for employee data
data class EmployeeAttendance(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val date: String,
    val loginTime: String,
    val totalTime: String,
    val marked: String,
    val shifts: Map<String, ShiftData>
)

data class ShiftData(
    val inTime: String,
    val outTime: String,
    val ti: Double,
    val to: Double
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

    // Filter states
    var selectedName by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf("June 2025") }
    var selectedDate by remember { mutableStateOf("All") }

    // Parse employee data from orgData
    val employeeData = remember(orgData, selectedName, selectedMonth, selectedDate) {
        orgData?.employeeList?.let { employees ->
            employees.map { employee ->
                EmployeeAttendance(
                    id = employee.id,
                    name = employee.name,
                    phoneNumber = employee.phoneNumber,
                    date = employee.date,
                    loginTime = employee.inTime,
                    totalTime = employee.hours,
                    marked = employee.marked,
                    shifts = employee.shifts.mapValues { (_, shift) ->
                        ShiftData(
                            inTime = shift.inn,
                            outTime = shift.out,
                            ti = if (shift.ti is String && shift.ti == "-") 0.0 else (shift.ti as? Double ?: 0.0),
                            to = if (shift.to is String && shift.to == "-") 0.0 else (shift.to as? Double ?: 0.0)
                        )
                    }
                )
            }.filter { employee ->
                // Apply filters
                val nameMatch = selectedName == "All" || employee.name == selectedName
                val monthMatch = selectedMonth == "All" || isDateInMonth(employee.date, selectedMonth)
                val dateMatch = selectedDate == "All" || employee.date == selectedDate

                nameMatch && monthMatch && dateMatch
            }
        } ?: emptyList()
    }

    // Get unique names, months, and dates for filters
    val uniqueNames = remember(orgData) {
        listOf("All") + (orgData?.employeeList?.map { it.name }?.distinct() ?: emptyList())
    }

    val uniqueMonths = remember(orgData) {
        listOf("All", "June 2025") + (orgData?.employeeList?.map { getMonthFromDate(it.date) }?.distinct()?.filterNot { it == "June 2025" } ?: emptyList())
    }

    val uniqueDates = remember(orgData) {
        listOf("All") + (orgData?.employeeList?.map { it.date }?.distinct() ?: emptyList())
    }

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
                title = { Text("Attendance", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
            // Name filter at the top, centered
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterLabelValue(
                    label = "Name",
                    value = selectedName,
                    options = uniqueNames,
                    onValueSelected = { selectedName = it }
                )
            }
            // Month and Date filters below name, centered
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterLabelValue(
                    label = "Month",
                    value = selectedMonth,
                    options = uniqueMonths,
                    onValueSelected = { selectedMonth = it }
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilterLabelValue(
                    label = "Date",
                    value = selectedDate,
                    options = uniqueDates,
                    onValueSelected = { selectedDate = it }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.padding(horizontal = 0.dp)) {
                items(employeeData) { employee ->
                    EmployeeAttendanceCardStyled(employee = employee)
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
fun FilterLabelValue(label: String, value: String, options: List<String>, onValueSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
        Spacer(modifier = Modifier.height(1.dp))
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = value,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .then(if (expanded) Modifier else Modifier)
                        .background(Color.Transparent),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color(0xFF1976D2))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeAttendanceCardStyled(employee: EmployeeAttendance) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = employee.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                StatusIndicator(status = employee.marked)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Login Time: ${employee.loginTime}",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "Total Time: ${employee.totalTime}",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(14.dp))
            TimelineBarStyled(shifts = employee.shifts)
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "P" -> Color(0xFF4CAF50) to Color.White // Green for Present
        "A" -> Color(0xFFF44336) to Color.White // Red for Absent
        "H" -> Color(0xFFFF9800) to Color.White // Orange for Half day
        else -> Color.Gray to Color.White
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimelineBarStyled(shifts: Map<String, ShiftData>) {
    val sortedShifts = shifts.toList().sortedBy { it.first }
    if (sortedShifts.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sortedShifts.forEach { (_, shift) ->
            val tiRatio = shift.ti.toFloat()
            val toRatio = shift.to.toFloat()
            if (tiRatio > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50))
                        .weight(tiRatio.coerceAtLeast(0.1f))
                        .fillMaxHeight()
                )
            }
            if (toRatio > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF44336))
                        .weight(toRatio.coerceAtLeast(0.1f))
                        .fillMaxHeight()
                )
            }
        }
    }
}

// Helper functions
private fun isDateInMonth(dateString: String, monthString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return false
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        monthYear == monthString
    } catch (e: Exception) {
        false
    }
}

private fun getMonthFromDate(dateString: String): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dateString) ?: Date(0)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        "Unknown"
    }
}

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
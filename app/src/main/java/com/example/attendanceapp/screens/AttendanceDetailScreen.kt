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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import android.util.Log
import com.example.attendanceapp.data.DataStoreManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import com.example.attendanceapp.api.EmployeeLoginResponse
import java.util.Locale

data class UiShift(
    val name: String,
    val startTime: String,
    val inOfficeDuration: String,
    val endTime: String,
    val outOfOfficeDuration: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(navController: NavController, selectedDateArg: String?) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }

    // Date picker state
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    val decodedDateArg = selectedDateArg?.let { URLDecoder.decode(it, "UTF-8") }
    var selectedDate by remember {
        mutableStateOf(
            decodedDateArg ?: DataStoreManager.getSelectedDate(context) ?: dateFormatter.format(java.util.Date())
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Use Compose state for employeeData
    var employeeData by remember { mutableStateOf<EmployeeLoginResponse?>(EmployeeDataManager.getEmployeeData()) }

    // Initialize toggle state from DataStore and employee data
    LaunchedEffect(selectedDate) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
        if (employeeData == null || decodedDateArg != null) {
            DataStoreManager.getEmployee(context)?.let { json ->
                try {
                    val employee = com.google.gson.Gson().fromJson(json, com.example.attendanceapp.api.EmployeeLoginResponse::class.java)
                    EmployeeDataManager.setEmployeeData(employee)
                    employeeData = employee
                    Log.d("AttendanceDetailScreen", "Loaded employee data from DataStore")
                } catch (e: Exception) {
                    Log.e("AttendanceDetailScreen", "Failed to parse employee data from DataStore", e)
                }
            }
        }
        // Only fetch for selectedDate if it is not null and not already loaded
        if (selectedDate.isNotBlank() && (decodedDateArg != null || employeeData == null)) {
            coroutineScope.launch {
                isLoading = true
                errorMessage = null
                val currentEmployeeData = employeeData
                try {
                    val phone = DataStoreManager.getEmployeePhone(context)
                    val imei = currentEmployeeData?.imei1 ?: ""
                    if (!phone.isNullOrBlank() && imei.isNotBlank()) {
                        val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                            phone = phone,
                            selected_date = selectedDate,
                            imei = imei
                        )
                        Log.d("AttendanceDetailScreen", "Making API call with phone: $phone, date: $selectedDate, imei: $imei")
                        val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                        EmployeeDataManager.setEmployeeData(response)
                        employeeData = response
                        val employeeJson = com.google.gson.Gson().toJson(response)
                        DataStoreManager.saveEmployee(context, employeeJson)
                        DataStoreManager.saveEmployeePhone(context, phone)
                        Log.d("AttendanceDetailScreen", "Employee data refresh completed successfully for date $selectedDate")
                    } else {
                        errorMessage = "Phone or IMEI missing."
                    }
                } catch (e: Exception) {
                    Log.e("AttendanceDetailScreen", "Failed to refresh employee data for date $selectedDate", e)
                    errorMessage = "Failed to refresh data. Please try again."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Date picker dialog
    val datePickerState = rememberDatePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        selectedDate = pickedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        DataStoreManager.saveSelectedDate(context, selectedDate)
                        showDatePicker = false
                        // Call API with new date (do NOT reset to today)
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            val currentEmployeeData = employeeData
                            try {
                                val phone = DataStoreManager.getEmployeePhone(context)
                                val imei = currentEmployeeData?.imei1
                                if (!phone.isNullOrBlank() && !imei.isNullOrBlank()) {
                                    val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                                        phone = phone,
                                        selected_date = selectedDate,
                                        imei = imei
                                    )
                                    Log.d("AttendanceDetailScreen", "Making API call with phone: $phone, date: $selectedDate, imei: $imei")
                                    val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                                    EmployeeDataManager.setEmployeeData(response)
                                    employeeData = response
                                    val employeeJson = com.google.gson.Gson().toJson(response)
                                    DataStoreManager.saveEmployee(context, employeeJson)
                                    DataStoreManager.saveEmployeePhone(context, phone)
                                    Log.d("AttendanceDetailScreen", "Employee data refresh completed successfully for date $selectedDate")
                                } else {
                                    errorMessage = "Phone or IMEI missing."
                                }
                            } catch (e: Exception) {
                                Log.e("AttendanceDetailScreen", "Failed to refresh employee data for date $selectedDate", e)
                                errorMessage = "Failed to refresh data. Please try again."
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        showDatePicker = false
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
            content = {
                DatePicker(state = datePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Attendance",
                isLoggingEnabled = isLoggingEnabled,
                onToggleChanged = {
                    isLoggingEnabled = it
                    com.example.attendanceapp.data.LogStatusManager.toggleLogging(context, it)
                },
                onRefresh = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val today = dateFormatter.format(java.util.Date())
                        selectedDate = today
                        DataStoreManager.saveSelectedDate(context, today)
                        val currentEmployeeData = employeeData
                        try {
                            val phone = DataStoreManager.getEmployeePhone(context)
                            val imei = currentEmployeeData?.imei1
                            if (!phone.isNullOrBlank() && !imei.isNullOrBlank()) {
                                val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                                    phone = phone,
                                    selected_date = today,
                                    imei = imei
                                )
                                Log.d("AttendanceDetailScreen", "Refreshing with phone: $phone, date: $today, imei: $imei")
                                val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                                EmployeeDataManager.setEmployeeData(response)
                                employeeData = response
                                val employeeJson = com.google.gson.Gson().toJson(response)
                                DataStoreManager.saveEmployee(context, employeeJson)
                                DataStoreManager.saveEmployeePhone(context, phone)
                                Log.d("AttendanceDetailScreen", "Employee data refresh completed successfully for date $today")
                            } else {
                                errorMessage = "Phone or IMEI missing."
                            }
                        } catch (e: Exception) {
                            Log.e("AttendanceDetailScreen", "Failed to refresh employee data for date $today", e)
                            errorMessage = "Failed to refresh data. Please try again."
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(
                navController = navController,
                currentRoute = when {
                    currentRoute?.startsWith("attendanceDetail") == true -> "attendanceDetail"
                    else -> currentRoute
                },
                items = listOf(
                    BottomBarItem(
                        label = "Account",
                        icon = Icons.Default.Person,
                        route = "employeeAccount"
                    ),
                    BottomBarItem(
                        label = "Attendance",
                        icon = Icons.Default.CalendarToday,
                        route = "attendanceDetail",
                        onClick = {
                            val today = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
                            val encodedDate = URLEncoder.encode(today, "UTF-8")
                            navController.navigate("attendanceDetail/$encodedDate") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    ),
                    BottomBarItem(
                        label = "Summary",
                        icon = Icons.Default.BarChart,
                        route = "calendarSummary"
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Attendance Info Bar with clickable date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "In: ${employeeData?.inTime ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Hrs: ${employeeData?.hours ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    selectedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent)
                        .clickable { showDatePicker = true }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Only replace the data area below the info bar when loading
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                employeeData == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading employee data...", color = Color.Gray)
                    }
                }
                else -> {
                    val currentEmployeeData = employeeData!!
                    val rawShifts = currentEmployeeData.shifts?.map { (shiftName, shiftData) ->
                        UiShift(
                            name = shiftName,
                            startTime = shiftData.inn,
                            inOfficeDuration = "${shiftData.ti} hrs",
                            endTime = shiftData.out,
                            outOfOfficeDuration = if (shiftData.to.toString() != "-") "Out for ${shiftData.to} hrs" else null
                        )
                    } ?: emptyList()
                    // Filter out empty/placeholder shifts
                    val shifts = rawShifts.filterNot { shift ->
                        (shift.startTime == "-" || shift.startTime.isBlank()) &&
                        (shift.inOfficeDuration == "- hrs" || shift.inOfficeDuration.isBlank()) &&
                        (shift.endTime == "-" || shift.endTime.isBlank())
                    }
                    if (shifts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No shift data available on this date", color = Color.Gray)
                        }
                    } else {
                        val timelineData = currentEmployeeData.shifts?.values?.flatMap { shift ->
                            val ti = shift.ti.toString().toFloatOrNull() ?: 0f
                            val to = shift.to.toString().toFloatOrNull() ?: 0f
                            listOf(Color.Green to (if (ti > 0f) ti / 8f else 0f), Color.Red to (if (to > 0f) to / 8f else 0f))
                        } ?: emptyList()
                        TimelineBar(data = timelineData, height = 24.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Legend()
                        Spacer(modifier = Modifier.height(16.dp))
                        ShiftList(shifts = shifts)
                    }
                }
            }
        }
    }
}

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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                shift.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                shift.startTime,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "----${shift.inOfficeDuration}----",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                shift.endTime,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        shift.outOfOfficeDuration?.let {
            Text(
                text = it,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceDetailScreenPreview() {
    val navController = rememberNavController()
    AttendanceDetailScreen(navController = navController, selectedDateArg = null)
}

private fun scheduleLogStatusWorker(context: Context) {
    val logStatusWorkRequest = PeriodicWorkRequestBuilder<LogStatusWorker>(
        java.time.Duration.ofMinutes(15)
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "log_status_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        logStatusWorkRequest
    )
}


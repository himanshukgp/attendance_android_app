package com.example.attendanceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.attendanceapp.screens.EmployeeTopBar
import com.example.attendanceapp.data.DataStoreManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date

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
fun AttendanceDetailScreen(navController: NavController, selectedDateArg: String?) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }

    // Date picker state
    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
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

    // Initialize toggle state from DataStore and employee data
    LaunchedEffect(selectedDate) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
        if (EmployeeDataManager.getEmployeeData() == null || decodedDateArg != null) {
            DataStoreManager.getEmployee(context)?.let { json ->
                try {
                    val employee = com.google.gson.Gson().fromJson(json, com.example.attendanceapp.api.EmployeeLoginResponse::class.java)
                    EmployeeDataManager.setEmployeeData(employee)
                    Log.d("AttendanceDetailScreen", "Loaded employee data from DataStore")
                } catch (e: Exception) {
                    Log.e("AttendanceDetailScreen", "Failed to parse employee data from DataStore", e)
                }
            }
        }
        // If a date is provided, make the API call for that date
        if (decodedDateArg != null) {
            coroutineScope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val phone = DataStoreManager.getEmployeePhone(context)
                    val employeeData = EmployeeDataManager.getEmployeeData()
                    val imei = employeeData?.imei1 ?: ""
                    if (!phone.isNullOrBlank() && imei.isNotBlank()) {
                        val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                            phone = phone,
                            selected_date = decodedDateArg,
                            imei = imei
                        )
                        Log.d("AttendanceDetailScreen", "Making API call with phone: $phone, date: $decodedDateArg, imei: $imei")
                        val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                        EmployeeDataManager.setEmployeeData(response)
                        val employeeJson = com.google.gson.Gson().toJson(response)
                        DataStoreManager.saveEmployee(context, employeeJson)
                        DataStoreManager.saveEmployeePhone(context, phone)
                        Log.d("AttendanceDetailScreen", "Employee data refresh completed successfully for date $decodedDateArg")
                        selectedDate = decodedDateArg
                    } else {
                        errorMessage = "Phone or IMEI missing."
                    }
                } catch (e: Exception) {
                    Log.e("AttendanceDetailScreen", "Failed to refresh employee data for date $decodedDateArg", e)
                    errorMessage = "Failed to refresh data. Please try again."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val employeeData = EmployeeDataManager.getEmployeeData()
    if (employeeData == null || isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Loading employee data...", color = Color.Gray)
            }
        }
        return
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
                        // Call API with new date
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val phone = DataStoreManager.getEmployeePhone(context)
                                val imei = employeeData.imei1
                                if (!phone.isNullOrBlank() && !imei.isNullOrBlank()) {
                                    val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                                        phone = phone,
                                        selected_date = selectedDate,
                                        imei = imei
                                    )
                                    Log.d("AttendanceDetailScreen", "Making API call with phone: $phone, date: $selectedDate, imei: $imei")
                                    val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                                    EmployeeDataManager.setEmployeeData(response)
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
            EmployeeTopBar(
                title = "Attendance",
                isLoggingEnabled = isLoggingEnabled,
                onToggleChanged = {
                    isLoggingEnabled = it
                    if (it) {
                        Log.d("AttendanceDetailScreen", "Toggle ON: Scheduling worker.")
                        scheduleLogStatusWorker(context)
                    } else {
                        Log.d("AttendanceDetailScreen", "Toggle OFF: Cancelling worker.")
                        WorkManager.getInstance(context).cancelUniqueWork("log_status_worker")
                    }
                },
                onRefresh = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val today = dateFormatter.format(java.util.Date())
                        selectedDate = today
                        DataStoreManager.saveSelectedDate(context, today)
                        try {
                            val phone = DataStoreManager.getEmployeePhone(context)
                            val imei = employeeData.imei1
                            if (!phone.isNullOrBlank() && !imei.isNullOrBlank()) {
                                val request = com.example.attendanceapp.api.EmployeeLoginRequest(
                                    phone = phone,
                                    selected_date = today,
                                    imei = imei
                                )
                                Log.d("AttendanceDetailScreen", "Refreshing with phone: $phone, date: $today, imei: $imei")
                                val response = com.example.attendanceapp.api.NetworkModule.apiService.employeeLogin(request)
                                EmployeeDataManager.setEmployeeData(response)
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
            NavigationBar(
                modifier = Modifier.height(48.dp)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
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
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
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
                    "In: ${employeeData.inTime ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Hrs: ${employeeData.hours ?: "-"}",
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
            val timelineData = employeeData.shifts?.values?.flatMap { shift ->
                val ti = shift.ti.toString().toFloatOrNull() ?: 0f
                val to = shift.to.toString().toFloatOrNull() ?: 0f
                listOf(Color.Green to (if (ti > 0f) ti / 8f else 0f), Color.Red to (if (to > 0f) to / 8f else 0f))
            } ?: emptyList()
            TimelineBar(data = timelineData, height = 24.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Legend()
            Spacer(modifier = Modifier.height(16.dp))
            val shifts = employeeData.shifts?.map { (shiftName, shiftData) ->
                UiShift(
                    name = shiftName,
                    startTime = shiftData.inn,
                    inOfficeDuration = "${shiftData.ti} hrs",
                    endTime = shiftData.out,
                    outOfOfficeDuration = if (shiftData.to.toString() != "-") "Out for ${shiftData.to} hrs" else null
                )
            } ?: emptyList()
            ShiftList(shifts = shifts)
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
    // We need a NavController for the preview.
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


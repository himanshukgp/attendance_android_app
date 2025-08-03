package com.example.attendanceapp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.OrgDataManager
import android.util.Log
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.OrgLoginRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Employee(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val date: String,
    val imei: String,
    val doj: String,
    val status: String,
    val hours: String,
    val inTime: String,
    val marked: String,
    val shifts: Map<String, Shift>
)

data class Shift(
    val IN: String?,
    val OUT: String?,
    val Ti: Double?,
    val To: Double?
)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgDashboardScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData = OrgDataManager.getOrgData()
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    val coroutineScope = rememberCoroutineScope()

    // Date and Time filter states
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var selectedTime by remember { mutableStateOf(getCurrentTime()) }
    var isLiveTime by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimeMenu by remember { mutableStateOf(false) }

    // Employee data state
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var filteredEmployees by remember { mutableStateOf<List<Pair<Employee, Boolean>>>(emptyList()) }

    val refreshData = {
        coroutineScope.launch {
            try {
                Log.d("OrgDashboardScreen", "Starting organization data refresh...")
                val phone = DataStoreManager.getOrgPhone(context)
                Log.d("OrgDashboardScreen", "Retrieved phone: $phone")
                if (phone != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val request = OrgLoginRequest(phone = phone, selected_date = currentDate)
                    Log.d("OrgDashboardScreen", "Making API call with phone: $phone, date: $currentDate")
                    val response = NetworkModule.apiService.orgLogin(request)
                    Log.d("OrgDashboardScreen", "API Response received: ${Gson().toJson(response)}")
                    OrgDataManager.setOrgData(response)
                    val orgJson = Gson().toJson(response)
                    DataStoreManager.saveOrg(context, orgJson)

                    // Parse employee data from response
                    employees = parseEmployeeData(response)
                    Log.d("OrgDashboardScreen", "Parsed ${employees.size} employees")

                    Log.d("OrgDashboardScreen", "Organization data refresh completed successfully")
                } else {
                    Log.e("OrgDashboardScreen", "Cannot refresh: organization phone not found in DataStore")
                }
            } catch (e: Exception) {
                Log.e("OrgDashboardScreen", "Failed to refresh organization data: ${e.message}", e)
            }
        }
    }

    // Load data when screen first loads - get existing org data
    LaunchedEffect(Unit) {
        Log.d("OrgDashboardScreen", "Screen loaded, loading existing org data...")
        val existingOrgData = OrgDataManager.getOrgData()
        if (existingOrgData != null) {
            Log.d("OrgDashboardScreen", "Found existing org data: ${Gson().toJson(existingOrgData)}")
            employees = parseEmployeeData(existingOrgData)
        } else {
            Log.d("OrgDashboardScreen", "No existing org data found")
        }
    }

    // Update live time every minute and refresh data
    LaunchedEffect(isLiveTime) {
        if (isLiveTime) {
            while (isLiveTime) {
                selectedTime = getCurrentTime()
                selectedDate = getCurrentDate() // Add this line
                // Call API every minute to refresh employee data
                refreshData()
                kotlinx.coroutines.delay(60000) // Wait 1 minute
            }
        }
    }

    // Filter employees when date, time, or employee data changes
    LaunchedEffect(selectedDate, selectedTime, employees) {
        Log.d("OrgDashboardScreen", "Filtering employees for date: $selectedDate, time: $selectedTime")
        filteredEmployees = filterEmployeesByDateTime(employees, selectedDate, selectedTime)
        Log.d("OrgDashboardScreen", "Filtered result: ${filteredEmployees.size} employees")
    }

    LaunchedEffect(selectedDate) {
        if (isLiveTime && selectedDate != getCurrentDate()) {
            isLiveTime = false
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Date Filter
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedDate)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time Filter
                OutlinedButton(
                    onClick = { showTimeMenu = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Select Time", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isLiveTime && selectedDate == getCurrentDate())
                            "LIVE: $selectedTime"
                        else
                            selectedTime
                    )
                }

                DropdownMenu(
                    expanded = showTimeMenu,
                    onDismissRequest = { showTimeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Time") },
                        onClick = {
                            showTimeMenu = false
                            showTimePicker = true
                            isLiveTime = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Live Time") },
                        onClick = {
                            showTimeMenu = false
                            isLiveTime = true
                            selectedTime = getCurrentTime()
                            selectedDate = getCurrentDate() // Add this line to update date to current
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Employee List
            Text(
                text = "Active Employees",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(filteredEmployees) { (employee, isActive) ->
                    EmployeeCard(employee = employee, isActive = isActive)
                }
            }

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
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Date(it)
                        val newSelectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                        selectedDate = newSelectedDate

                        // Check if the new date is different from current date
                        val currentDate = getCurrentDate()
                        if (newSelectedDate != currentDate && isLiveTime) {
                            // If date is changed from current and we're in live mode, disable live mode
                            isLiveTime = false
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmployeeCard(employee: Employee, isActive: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFE8F5E8) else Color(0xFFFFF0F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = employee.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isActive) "🟢" else "🔴",
                fontSize = 20.sp
            )
        }
    }
}

private fun getCurrentDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
}

private fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun parseEmployeeData(orgData: Any?): List<Employee> {
    try {
        Log.d("OrgDashboardScreen", "Parsing employee data...")

        when (orgData) {
            is com.example.attendanceapp.api.OrgLoginResponse -> {
                // Direct OrgLoginResponse object
                Log.d("OrgDashboardScreen", "Found OrgLoginResponse with ${orgData.employeeList.size} employees")
                return orgData.employeeList.map { orgEmployee ->
                    convertOrgEmployeeToEmployee(orgEmployee)
                }
            }
            else -> {
                // Try to parse as JSON string
                val gson = Gson()
                val jsonString = orgData as? String ?: gson.toJson(orgData)

                Log.d("OrgDashboardScreen", "Trying to parse JSON: $jsonString")

                val orgLoginResponse = gson.fromJson(jsonString, com.example.attendanceapp.api.OrgLoginResponse::class.java)
                Log.d("OrgDashboardScreen", "Parsed OrgLoginResponse with ${orgLoginResponse.employeeList.size} employees")

                return orgLoginResponse.employeeList.map { orgEmployee ->
                    convertOrgEmployeeToEmployee(orgEmployee)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("OrgDashboardScreen", "Error parsing employee data: ${e.message}", e)
        return emptyList()
    }
}

private fun convertOrgEmployeeToEmployee(orgEmployee: com.example.attendanceapp.api.OrgEmployee): Employee {
    // Convert OrgShift to Shift
    val shifts = orgEmployee.shifts.mapValues { (_, orgShift) ->
        Shift(
            IN = orgShift.inn,
            OUT = orgShift.out,
            Ti = when (orgShift.ti) {
                is Number -> orgShift.ti.toDouble()
                is String -> orgShift.ti.toDoubleOrNull()
                else -> null
            },
            To = when (orgShift.to) {
                is Number -> orgShift.to.toDouble()
                is String -> orgShift.to.toDoubleOrNull()
                else -> null
            }
        )
    }

    return Employee(
        id = orgEmployee.id,
        name = orgEmployee.name,
        phoneNumber = orgEmployee.phoneNumber,
        date = orgEmployee.date,
        imei = orgEmployee.imei,
        doj = orgEmployee.doj,
        status = orgEmployee.status,
        hours = orgEmployee.hours,
        inTime = orgEmployee.inTime,
        marked = orgEmployee.marked,
        shifts = shifts
    )
}

private fun filterEmployeesByDateTime(employees: List<Employee>, date: String, time: String): List<Pair<Employee, Boolean>> {
    // First, get unique employees by phone number
    val uniqueEmployees = employees.groupBy { it.phoneNumber }
        .mapValues { (_, employeeList) ->
            // For each phone number, get the employee with the matching date, or the most recent one
            employeeList.find { it.date == date } ?: employeeList.maxByOrNull { it.date }
        }
        .values
        .filterNotNull()

    Log.d("OrgDashboardScreen", "Unique employees: ${uniqueEmployees.size} (from ${employees.size} total)")

    // Filter and determine activity status
    val filtered = uniqueEmployees.map { employee ->
        val isActive = isEmployeeActiveAtTime(employee, date, time)
        employee to isActive
    }

    // Sort: active employees first, then by name
    val sorted = filtered.sortedWith(compareBy<Pair<Employee, Boolean>> { !it.second }.thenBy { it.first.name })

    Log.d("OrgDashboardScreen", "Active employees: ${sorted.count { it.second }}, Inactive: ${sorted.count { !it.second }}")

    return sorted
}

private fun isEmployeeActiveAtTime(employee: Employee, targetDate: String, targetTime: String): Boolean {
    // Check if the employee's date matches the target date
    if (employee.date != targetDate) return false

    // Parse target time
    val targetTimeMinutes = timeToMinutes(targetTime)

    // Check each shift
    for (shift in employee.shifts.values) {
        val inTime = shift.IN
        val outTime = shift.OUT

        if (inTime != null) {
            val inTimeMinutes = timeToMinutes(inTime)

            // If OUT time is null (NA), check if target time is after IN time
            if (outTime == null || outTime == "NA") {
                if (targetTimeMinutes >= inTimeMinutes) {
                    return true
                }
            } else {
                // If both IN and OUT times exist, check if target time is between them
                val outTimeMinutes = timeToMinutes(outTime)
                if (targetTimeMinutes >= inTimeMinutes && targetTimeMinutes <= outTimeMinutes) {
                    return true
                }
            }
        }
    }

    return false
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return if (parts.size >= 2) {
        parts[0].toInt() * 60 + parts[1].toInt()
    } else {
        0
    }
}

@Preview(showBackground = true)
@Composable
fun OrgDashboardScreenPreview() {
    val navController = rememberNavController()
    OrgDashboardScreen(navController = navController)
}
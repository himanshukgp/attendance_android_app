package com.example.attendanceapp.screens

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendanceapp.data.OrgDataManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import com.example.attendanceapp.data.DataStoreManager
import androidx.compose.runtime.rememberCoroutineScope
import com.example.attendanceapp.api.OrgLoginRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateMapOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter


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

data class EmployeeMaster(val name: String, val phoneNumber: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgAttendanceScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData by OrgDataManager.orgData
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val tempMarkedAttendance = remember { mutableStateMapOf<String, String>() }

    // Filter states
    var selectedName by remember { mutableStateOf("All") }

    // *** REPLACE old date picker states ***
    //var showDatePicker by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    val today = remember { Date() }
    val defaultDateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(today)
    var selectedDate by remember { mutableStateOf<String?>(defaultDateString) }
    // ----------------------------------------------------

    // Your existing data filtering logic remains unchanged...
// Updated data filtering logic to handle null date (All dates)
    val employeeData = remember(orgData, selectedName, selectedDate) {
        orgData?.employeeList?.let { employees ->
            employees
                .filter {
                    (selectedName == "All" || it.name == selectedName) &&
                            (selectedDate == null || it.date == selectedDate) // Handle null for "All dates"
                }
                .map { employee ->
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
                }
        } ?: emptyList()
    }

    val allEmployeesForSelectedName = remember(orgData, selectedName) {
        (orgData?.employeeList ?: emptyList()).filter {
            selectedName == "All" || it.name == selectedName
        }
    }
    val employeesWithAttendance = employeeData.map { it.id }.toSet()

    val uniqueNames = remember(orgData) {
        listOf("All") + (orgData?.employeeList?.map { it.name }?.distinct() ?: emptyList())
    }

    LaunchedEffect(Unit) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
    }

    val refreshData = {
        coroutineScope.launch {
            try {
                val phone = DataStoreManager.getOrgPhone(context)
                if (phone != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val request = OrgLoginRequest(phone = phone, selected_date = currentDate)
                    val response = com.example.attendanceapp.api.NetworkModule.apiService.orgLogin(request)
                    OrgDataManager.setOrgData(response)
                    val orgJson = Gson().toJson(response)
                    DataStoreManager.saveOrg(context, orgJson)
                }
            } catch (_: Exception) {
                // handle errors as needed or log
            }
        }
    }

    LaunchedEffect(employeeData) {
        // Cleanup temp attendance markings
        val ids = employeeData.map { it.phoneNumber }.toSet()
        tempMarkedAttendance.keys.filter { it in ids }.forEach { tempMarkedAttendance.remove(it) }
    }

    LaunchedEffect(Unit) {
        refreshData()
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Name and Date filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterLabelValue(
                    label = "Name",
                    value = selectedName,
                    options = uniqueNames,
                    onValueSelected = { selectedName = it }
                )

                // Date filter with label and button to show custom calendar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Date",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(48.dp)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Button(
                        onClick = { showCalendar = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedDate ?: "All", // Show "All" when selectedDate is null
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(employeeData) { employee ->
                    EmployeeAttendanceCardStyled(employee = employee)
                    Spacer(modifier = Modifier.height(18.dp))
                }

                if (selectedDate != null) {
// Get all employees uniquely by their phone number (master list)
                    val allEmployeesByPhone = (orgData?.employeeList ?: emptyList()).distinctBy { it.phoneNumber }

                    // Get IDs (phone_date) of employees that already have attendance for selectedDate
                    val markedIdsSet = (orgData?.employeeList ?: emptyList())
                        .filter { it.date == selectedDate }
                        .map { it.id }
                        .toSet()

                    // Filter to those who do NOT have attendance on selectedDate
                    val unmarkedEmployees = allEmployeesByPhone.filter { emp ->
                        val idForSelectedDate = "${emp.phoneNumber}_$selectedDate"
                        idForSelectedDate !in markedIdsSet
                    }
                    items(unmarkedEmployees) { emp ->
                        val markId = "${emp.phoneNumber}_$selectedDate"
                        val tempStatus = tempMarkedAttendance[markId]
                        MarkAttendanceCard(
                            employeeId = markId,
                            name = emp.name,
                            phoneNumber = emp.phoneNumber,
                            date = selectedDate!!,
                            alreadyMarked = tempStatus != null,
                            markedStatus = tempStatus,
                            onMark = { status ->
                                coroutineScope.launch {
                                    try {
                                        val phone = DataStoreManager.getOrgPhone(context)
                                        if (phone != null) {
                                            val response = markAttendanceApiCall(
                                                phone,
                                                markId,            // Pass phone_date as id
                                                selectedDate!!,
                                                status
                                            )
                                            if (response.isSuccessful) {
                                                tempMarkedAttendance[markId] = status
                                                snackbarHostState.showSnackbar("Marked attendance for ${emp.name}")
                                                refreshData()
                                            } else {
                                                snackbarHostState.showSnackbar("Failed marking attendance")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }


                }

            }
        if (showCalendar) {
            CalendarView(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                    showCalendar = false
                },
                onDismiss = { showCalendar = false }
            )
        }
        }
    }

// -------- Inline WEEK-VIEW CALENDAR COMPOSABLE --------
@Composable
fun CalendarView(
    selectedDate: String?,
    onDateSelected: (String?) -> Unit, // Changed to accept null for "All dates"
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val selected = remember(selectedDate) {
        selectedDate?.let {
            runCatching { LocalDate.parse(it, formatter) }.getOrNull() ?: today
        } ?: today
    }

    var currentMonth by remember { mutableStateOf(selected.withDayOfMonth(1)) }

    // Generate calendar grid for the current month
    val calendarDays = remember(currentMonth) {
        generateCalendarDays(currentMonth)
    }

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        shadowElevation = 10.dp,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button top-right
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            // "All dates" checkbox option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onDateSelected(null) }
                    .background(
                        if (selectedDate == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = selectedDate == null,
                    onCheckedChange = { if (it) onDateSelected(null) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All dates",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selectedDate == null) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Month/Year header with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            LazyColumn {
                items(calendarDays.chunked(7)) { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { dateInfo ->
                            CalendarDayCell(
                                dateInfo = dateInfo,
                                selectedDate = selected,
                                today = today,
                                currentMonth = currentMonth,
                                onDateSelected = { date ->
                                    onDateSelected(date.format(formatter))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fill remaining cells if the last week has fewer than 7 days
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick date selection buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = {
                        onDateSelected(today.format(formatter))
                    }
                ) {
                    Text("Today")
                }

                TextButton(
                    onClick = {
                        onDateSelected(today.minusDays(1).format(formatter))
                    }
                ) {
                    Text("Yesterday")
                }

                TextButton(
                    onClick = {
                        onDateSelected(today.plusDays(1).format(formatter))
                    }
                ) {
                    Text("Tomorrow")
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dateInfo: CalendarDateInfo,
    selectedDate: LocalDate,
    today: LocalDate,
    currentMonth: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = dateInfo.date == selectedDate
    val isToday = dateInfo.date == today
    val isCurrentMonth = dateInfo.date.month == currentMonth.month && dateInfo.date.year == currentMonth.year
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .clickable { onDateSelected(dateInfo.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateInfo.date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = when {
                isSelected -> FontWeight.Bold
                isToday -> FontWeight.SemiBold
                else -> FontWeight.Normal
            },
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

data class CalendarDateInfo(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

private fun generateCalendarDays(currentMonth: LocalDate): List<CalendarDateInfo> {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
    
    // Find the first Sunday of the calendar grid
    val firstCalendarDay = firstDayOfMonth.let { firstDay ->
        val dayOfWeek = firstDay.dayOfWeek.value % 7 // Convert to 0=Sunday, 1=Monday, etc.
        firstDay.minusDays(dayOfWeek.toLong())
    }
    
    // Find the last Saturday of the calendar grid
    val lastCalendarDay = lastDayOfMonth.let { lastDay ->
        val dayOfWeek = lastDay.dayOfWeek.value % 7
        val daysToAdd = (6 - dayOfWeek).toLong()
        lastDay.plusDays(daysToAdd)
    }
    
    // Generate all days in the calendar grid
    val calendarDays = mutableListOf<CalendarDateInfo>()
    var currentDate = firstCalendarDay
    
    while (!currentDate.isAfter(lastCalendarDay)) {
        calendarDays.add(
            CalendarDateInfo(
                date = currentDate,
                isCurrentMonth = currentDate.month == currentMonth.month && currentDate.year == currentMonth.year
            )
        )
        currentDate = currentDate.plusDays(1)
    }
    
    return calendarDays
}

@Composable
fun FilterLabelValue(label: String, value: String, options: List<String>, onValueSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            fontSize = 9.sp, // Reduced font size
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
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
                    fontSize = 11.sp, // Reduced font size
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
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
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Basic info (always visible)
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

            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                ShiftDetailsTable(shifts = employee.shifts)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun ShiftDetailsTable(shifts: Map<String, ShiftData>) {
    val sortedShifts = shifts.toList().sortedBy { it.first }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Table header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Shift",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "From",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "To",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Till",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Break",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Table rows
        sortedShifts.forEachIndexed { index, (shiftKey, shift) ->
            val shiftNumber = shiftKey.removePrefix("Shift_")
            val tillHours = String.format("%.2f", shift.ti)
            val breakHours = String.format("%.2f", shift.to)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shiftNumber,
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = shift.inTime,
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = shift.outTime,
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${tillHours}h",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${breakHours}h",
                    fontSize = 11.sp,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            if (index < sortedShifts.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
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

@Preview(showBackground = true)
@Composable
fun OrgAttendanceScreenPreview() {
    val navController = rememberNavController()
    OrgAttendanceScreen(navController = navController)
}

// Card for marking attendance for all employees
@Composable
fun MarkAttendanceCard(
    employeeId: String, // Add this parameter
    name: String,
    phoneNumber: String,
    date: String,
    alreadyMarked: Boolean,
    markedStatus: String?,
    onMark: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(18.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!alreadyMarked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusCircle(letter = "P", color = Color(0xFF4CAF50)) { showDialog = "P" }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusCircle(letter = "A", color = Color(0xFFF44336)) { showDialog = "A" }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusCircle(letter = "H", color = Color(0xFFFF9800)) { showDialog = "H" }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (alreadyMarked && markedStatus != null) {
                Text(
                    text = "Attendance already marked: ${statusText(markedStatus)}",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Normal
                )
            } else if (!alreadyMarked) {
                Text(
                    text = "No attendance marked for this day.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Confirm Attendance") },
            text = { Text("Mark $name as ${statusText(showDialog!!)} for $date?") },
            confirmButton = {
                TextButton(onClick = {
                    onMark(showDialog!!)
                    showDialog = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) { Text("Cancel") }
            }
        )
    }
}
@Composable
private fun StatusCircle(letter: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun statusText(status: String): String = when (status) {
    "P" -> "Present"
    "A" -> "Absent"
    "H" -> "Half Day"
    else -> status
}

// Add mark attendance API call
suspend fun markAttendanceApiCall(
    phone: String,
    employeeId: String, // Use actual employee ID instead of generating one
    date: String,
    status: String
): retrofit2.Response<Unit> {
    return withContext(Dispatchers.IO) {
        val body = mapOf(
            "phone" to phone,
            "id" to employeeId, // Use the actual employee ID
            "date" to date,     // Add date as separate field
            "status" to status
        )
        val api = com.example.attendanceapp.api.NetworkModule.apiService
        api.markAttendance(body)
    }
}
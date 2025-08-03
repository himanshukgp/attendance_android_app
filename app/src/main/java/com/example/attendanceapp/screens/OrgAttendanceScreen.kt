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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
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
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle


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
sealed class DateFilter {
    data class SingleDay(val day: String): DateFilter() // "dd/MM/yyyy"
    data class WholeMonth(val month: Int, val year: Int): DateFilter() // 1..12, yyyy
}
private fun defaultDateFilter(): DateFilter {
    val today = LocalDate.now()
    return DateFilter.SingleDay(today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
}

data class EmployeeMaster(val name: String, val phoneNumber: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgAttendanceScreen(
    navController: NavController,
    initialDate: String? = null,
    initialName: String? = null
) {
    val orgData by OrgDataManager.orgData
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val tempMarkedAttendance = remember { mutableStateMapOf<String, String>() }

    // --- FILTER state ---
    var selectedName by remember { mutableStateOf(initialName ?: "All") }
    var showCalendar by remember { mutableStateOf(false) }
    val defaultDate = remember { defaultDateFilter() }

    // --- DateFilter logic ---
    var dateFilter by remember {
        mutableStateOf<DateFilter>(
            initialDate?.let { DateFilter.SingleDay(it) } ?: defaultDate
        )
    }
    // Update filter when navigation parameters change
    LaunchedEffect(initialDate) {
        if (initialDate != null) dateFilter = DateFilter.SingleDay(initialDate)
    }
    LaunchedEffect(initialName) {
        if (initialName != null) selectedName = initialName
    }

    val allEmployeesMasterList = remember(orgData) {
        orgData?.employeeList?.distinctBy { it.phoneNumber }?.map {
            EmployeeMaster(name = it.name, phoneNumber = it.phoneNumber)
        } ?: emptyList()
    }
    val filteredEmployees = remember(allEmployeesMasterList, selectedName) {
        if (selectedName == "All") allEmployeesMasterList
        else allEmployeesMasterList.filter { it.name == selectedName }
    }

    fun parseDateToMonthYear(date: String): Pair<Int, Int>? = try {
        val parts = date.split("/")
        if (parts.size == 3)
            Pair(parts[1].toInt(), parts[2].toInt())
        else null
    } catch (_: Exception) { null }

    // Attendance filter for selected date/month
    val attendanceForFilter = remember(orgData, dateFilter) {
        when (dateFilter) {
            is DateFilter.SingleDay ->
                orgData?.employeeList?.filter { it.date == (dateFilter as DateFilter.SingleDay).day } ?: emptyList()
            is DateFilter.WholeMonth -> orgData?.employeeList?.filter {
                parseDateToMonthYear(it.date)?.let { (month, year) ->
                    month == (dateFilter as DateFilter.WholeMonth).month && year == (dateFilter as DateFilter.WholeMonth).year
                } ?: false
            } ?: emptyList()
        }
    }
    val attendanceIdSet = remember(attendanceForFilter) { attendanceForFilter.map { it.id }.toSet() }

    val employeeData = remember(attendanceForFilter, selectedName) {
        val filteredData = if (selectedName == "All") {
            attendanceForFilter
        } else attendanceForFilter.filter { it.name.equals(selectedName, ignoreCase = true) }
        filteredData.map { employee ->
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
    }

    // Unmarked employees (only for single day)
    val unmarkedEmployees = remember(filteredEmployees, attendanceIdSet, dateFilter) {
        if (dateFilter is DateFilter.SingleDay) {
            val dateStr = (dateFilter as DateFilter.SingleDay).day
            filteredEmployees.filter { emp ->
                val idForDate = "${emp.phoneNumber}_$dateStr"
                idForDate !in attendanceIdSet
            }
        } else emptyList()
    }

    // --- Refresh function ---
    val refreshData: () -> Unit = {
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
            } catch (_: Exception) { }
        }
    }
    LaunchedEffect(Unit) { refreshData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    Text(
                        text = if (DataStoreManager.getWorkerToggleState(context)) "ON" else "OFF",
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = DataStoreManager.getWorkerToggleState(context),
                        onCheckedChange = { com.example.attendanceapp.data.LogStatusManager.toggleLogging(context, it) },
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            OrgBottomBar(
                navController,
                navController.currentBackStackEntryAsState().value?.destination?.route
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {

            // ---- FILTERED from Summary/Explicit filter banner ----
            val isFiltered = remember(selectedName, dateFilter) {
                selectedName != "All" || dateFilter != defaultDate
            }
            if (isFiltered) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Filters applied",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = buildString {
                                    when (dateFilter) {
                                        is DateFilter.SingleDay -> append("Date: ${(dateFilter as DateFilter.SingleDay).day}")
                                        is DateFilter.WholeMonth -> {
                                            val m = Month.of((dateFilter as DateFilter.WholeMonth).month)
                                            append(
                                                "Month: ${m.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${(dateFilter as DateFilter.WholeMonth).year}"
                                            )
                                        }
                                    }
                                    if (selectedName != "All") {
                                        if (isNotEmpty()) append(" | ")
                                        append("Name: $selectedName")
                                    }
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedName = "All"
                                dateFilter = defaultDate
                            }
                        ) {
                            Text("Clear Filters", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ---- FILTER ROW ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterLabelValue(
                    label = "Name",
                    value = selectedName,
                    options = listOf("All") + allEmployeesMasterList.map { it.name }.distinct(),
                    onValueSelected = { selectedName = it }
                )
                Button(
                    onClick = { showCalendar = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    val filterLabel =
                        when (val df = dateFilter) {
                            is DateFilter.SingleDay -> df.day
                            is DateFilter.WholeMonth -> {
                                val monthStr = Month.of(df.month).getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }
                                "$monthStr ${df.year.toString().takeLast(2)}"
                            }
                        }
                    Text(text = filterLabel, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(employeeData) { employee ->
                    EmployeeAttendanceCardStyled(
                        employee = employee,
                        snackbarHostState = snackbarHostState,
                        refreshData = refreshData
                    )
                    Spacer(Modifier.height(18.dp))
                }
                if (dateFilter is DateFilter.SingleDay) {
                    val selectedDateStr = (dateFilter as DateFilter.SingleDay).day
                    items(unmarkedEmployees) { emp ->
                        val markId = "${emp.phoneNumber}_$selectedDateStr"
                        val tempStatus = tempMarkedAttendance[markId]
                        MarkAttendanceCard(
                            employeeId = markId,
                            name = emp.name,
                            phoneNumber = emp.phoneNumber,
                            date = selectedDateStr,
                            alreadyMarked = tempStatus != null,
                            markedStatus = tempStatus,
                            onMark = { status ->
                                coroutineScope.launch {
                                    try {
                                        val phone = DataStoreManager.getOrgPhone(context)
                                        if (phone != null) {
                                            val response = markAttendanceApiCall(
                                                phone = phone,
                                                employeeId = markId,
                                                date = selectedDateStr,
                                                status = status
                                            )
                                            if (response.isSuccessful) {
                                                tempMarkedAttendance[markId] = status
                                                snackbarHostState.showSnackbar("Marked attendance for ${emp.name}")
                                                refreshData()
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to mark attendance")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }

        // ---- Calendar dialog ----
        if (showCalendar) {
            CalendarViewWithMonth(
                selectedFilter = dateFilter,
                onDateFilterSelected = {
                    dateFilter = it
                    showCalendar = false
                },
                onDismiss = { showCalendar = false }
            )
        }
    }
}

@Composable
fun EmployeeAttendanceCardStyled(
    employee: EmployeeAttendance,
    refreshData: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<String?>(null) }
    var currentAttendanceStatus by remember { mutableStateOf(employee.marked) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val disabledColor = Color(0xFFBDBDBD) // Material design disabled gray

    // Update currentAttendanceStatus when employee.marked changes (useful for data refresh)
    LaunchedEffect(employee.marked) {
        currentAttendanceStatus = employee.marked
    }

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
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Show all three circles with current status highlighted
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Present circle
                    StatusCircle(
                        letter = "P",
                        color = if (currentAttendanceStatus == "P")
                            Color(0xFF4CAF50) else disabledColor,
                        onClick = {
                            if (currentAttendanceStatus != "P") {
                                showDialog = "P"
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Absent circle
                    StatusCircle(
                        letter = "A",
                        color = if (currentAttendanceStatus == "A")
                            Color(0xFFF44336) else disabledColor,
                        onClick = {
                            if (currentAttendanceStatus != "A") {
                                showDialog = "A"
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Holiday circle
                    StatusCircle(
                        letter = "H",
                        color = if (currentAttendanceStatus == "H")
                            Color(0xFFFF9800) else disabledColor,
                        onClick = {
                            if (currentAttendanceStatus != "H") {
                                showDialog = "H"
                            }
                        }
                    )
                }
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
            Text(
                text = "Current Status: ${statusText(currentAttendanceStatus)} (tap other circles to change)",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Normal,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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

    // Confirmation dialog for changing attendance
    showDialog?.let { newStatus ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Change Attendance") },
            text = {
                Text(
                    "Change ${employee.name}'s attendance from ${statusText(currentAttendanceStatus)} to ${statusText(newStatus)} for ${employee.date}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val phone = DataStoreManager.getOrgPhone(context)
                                if (phone != null) {
                                    val response = markAttendanceApiCall(
                                        phone = phone,
                                        employeeId = employee.id,
                                        date = employee.date,
                                        status = newStatus
                                    )
                                    if (response.isSuccessful) {
                                        // Update the local state immediately
                                        currentAttendanceStatus = newStatus
                                        snackbarHostState.showSnackbar("Updated attendance for ${employee.name}")
                                        // Refresh data to show updated status
                                        refreshData()
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to update attendance")
                                    }
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
                            }
                        }
                        showDialog = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CalendarViewWithMonth(
    selectedFilter: DateFilter,
    onDateFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val selectedDay = (selectedFilter as? DateFilter.SingleDay)?.day
    val selectedLocalDate = selectedDay?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() } ?: today
    var currentMonth by remember { mutableStateOf(selectedLocalDate.withDayOfMonth(1)) }
    val isAllThisMonth = selectedFilter is DateFilter.WholeMonth
            && selectedFilter.month == currentMonth.monthValue
            && selectedFilter.year == currentMonth.year
    val calendarDays = remember(currentMonth) { generateCalendarDays(currentMonth) }

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
            // Close button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            // All dates of this month
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        onDateFilterSelected(DateFilter.WholeMonth(currentMonth.monthValue, currentMonth.year))
                    }
                    .background(
                        if (isAllThisMonth) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = isAllThisMonth,
                    onCheckedChange = {
                        if (it) onDateFilterSelected(DateFilter.WholeMonth(currentMonth.monthValue, currentMonth.year))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isAllThisMonth) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Month nav
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
            // Days of week
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
                            val isSelected = (selectedFilter as? DateFilter.SingleDay)?.let {
                                runCatching { LocalDate.parse(it.day, formatter) }.getOrNull() == dateInfo.date
                            } == true && !isAllThisMonth
                            val isToday = today == dateInfo.date
                            val isCurrentMonth = dateInfo.date.month == currentMonth.month && dateInfo.date.year == currentMonth.year

                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                    .clickable {
                                        onDateFilterSelected(
                                            DateFilter.SingleDay(dateInfo.date.format(formatter))
                                        )
                                    },
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
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Quick Date buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = { onDateFilterSelected(DateFilter.SingleDay(today.format(formatter))) }
                ) { Text("Today") }
                TextButton(
                    onClick = { onDateFilterSelected(DateFilter.SingleDay(today.minusDays(1).format(formatter))) }
                ) { Text("Yesterday") }
                TextButton(
                    onClick = { onDateFilterSelected(DateFilter.SingleDay(today.plusDays(1).format(formatter))) }
                ) { Text("Tomorrow") }
            }
        }
    }
}

data class CalendarDateInfo(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

private fun generateCalendarDays(currentMonth: LocalDate): List<CalendarDateInfo> {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
    val firstCalendarDay = firstDayOfMonth.minusDays(((firstDayOfMonth.dayOfWeek.value % 7).toLong()))
    val lastCalendarDay = lastDayOfMonth.plusDays((6 - (lastDayOfMonth.dayOfWeek.value % 7)).toLong())
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

@Composable
fun MarkAttendanceCard(
    employeeId: String,
    name: String,
    phoneNumber: String,
    date: String,
    alreadyMarked: Boolean,
    markedStatus: String?,
    onMark: (String) -> Unit
) {
    val disabledColor = Color(0xFFBDBDBD) // Material design disabled gray
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

                // Always show all three circles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Present circle
                    StatusCircle(
                        letter = "P",
                        color = if (alreadyMarked && markedStatus == "P")
                            Color(0xFF4CAF50) else disabledColor,
                        onClick = { showDialog = "P" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Absent circle
                    StatusCircle(
                        letter = "A",
                        color = if (alreadyMarked && markedStatus == "A")
                            Color(0xFFF44336) else disabledColor,
                        onClick = { showDialog = "A" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Holiday circle
                    StatusCircle(
                        letter = "H",
                        color = if (alreadyMarked && markedStatus == "H")
                            Color(0xFFFF9800) else disabledColor,
                        onClick = { showDialog = "H" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status text
            if (alreadyMarked && markedStatus != null) {
                Text(
                    text = "Attendance marked: ${statusText(markedStatus)} (tap to change)",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Normal
                )
            } else {
                Text(
                    text = "No attendance marked for this day.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }

    // Confirmation dialog
    showDialog?.let { status ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Confirm Attendance") },
            text = {
                Text(
                    if (alreadyMarked)
                        "Change $name's attendance to ${statusText(status)} for $date?"
                    else
                        "Mark $name as ${statusText(status)} for $date?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMark(status)
                        showDialog = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusCircle(
    letter: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

fun statusText(status: String): String {
    return when (status) {
        "P" -> "Present"
        "A" -> "Absent"
        "H" -> "Halfday"
        else -> "Unknown"
    }
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

@Preview(showBackground = true)
@Composable
fun OrgAttendanceScreenPreview() {
    val navController = rememberNavController()
    OrgAttendanceScreen(navController = navController)
}
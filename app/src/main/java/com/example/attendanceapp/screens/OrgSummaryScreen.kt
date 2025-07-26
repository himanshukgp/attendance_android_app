package com.example.attendanceapp.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.OrgDataManager
import com.example.attendanceapp.api.OrgEmployee
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// --- Data holder for calendar day info ---
data class DayAttendanceInfo(
    val date: LocalDate,
    val color: Color,
    val progress: Float,
    val tooltip: String
)

// --- Helper: converts Any (ti/to) safely to Double ---
private fun convertToDouble(value: Any?): Double {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

// --- Build map of date -> DayAttendanceInfo for calendar ---
fun buildOrgCalendarMap(
    empList: List<OrgEmployee>,
    filterName: String?,
    empCount: Int
): Map<LocalDate, DayAttendanceInfo> {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateMap = mutableMapOf<LocalDate, DayAttendanceInfo>()

    // Group employees by date - adapt this based on your OrgEmployee structure
    val groupedByDate = empList.groupBy { emp ->
        // You may need to adjust this based on your actual OrgEmployee date field
        // Common field names: date, attendanceDate, workDate, etc.
        try {
            // Try to get date field using reflection or direct property access
            emp.javaClass.getDeclaredField("date").let { field ->
                field.isAccessible = true
                field.get(emp) as? String ?: ""
            }
        } catch (e: Exception) {
            // Fallback - you may need to replace this with your actual date field
            ""
        }
    }

    for ((dateStr, empOnThisDay) in groupedByDate) {
        if (dateStr.isBlank()) continue
        val date = try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: Exception) {
            null
        } ?: continue

        if (filterName == null || filterName == "All") {
            val presentCount = empOnThisDay.count { emp ->
                // Adapt this based on your OrgEmployee attendance field
                try {
                    val markedField = emp.javaClass.getDeclaredField("marked")
                    markedField.isAccessible = true
                    val marked = markedField.get(emp) as? String ?: ""
                    marked.equals("P", true)
                } catch (e: Exception) {
                    false
                }
            }
            val total = empCount.takeIf { it > 0 } ?: empOnThisDay.size
            val color = Color(0xFF4CAF50) // always green when all selected
            val progress = presentCount.toFloat() / total.coerceAtLeast(1)
            val tooltip = "Present: $presentCount, Total: $total"
            dateMap[date] = DayAttendanceInfo(date, color, progress.coerceIn(0f,1f), tooltip)
        } else {
            val emp = empOnThisDay.find { employee ->
                try {
                    val nameField = employee.javaClass.getDeclaredField("name")
                    nameField.isAccessible = true
                    val name = nameField.get(employee) as? String ?: ""
                    name.equals(filterName, ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
            }
            if (emp == null) continue

            val marked = try {
                val markedField = emp.javaClass.getDeclaredField("marked")
                markedField.isAccessible = true
                (markedField.get(emp) as? String ?: "").ifBlank { "" }
            } catch (e: Exception) {
                ""
            }

            val statusColor = when (marked) {
                "P" -> Color(0xFF4CAF50)    // Green Present
                "A" -> Color(0xFFF44336)    // Red Absent
                "H" -> Color(0xFFFFC107)    // Yellow Halfday
                else -> Color.Gray          // Default gray
            }

            // Calculate worked / (worked + break) progress
            var tiSum = 0.0
            var toSum = 0.0
            try {
                val shiftsField = emp.javaClass.getDeclaredField("shifts")
                shiftsField.isAccessible = true
                val shifts = shiftsField.get(emp) as? Map<String, Any>
                if (shifts != null) {
                    for (shift in shifts.values) {
                        // Try to extract ti and to values from shift object
                        try {
                            val tiField = shift.javaClass.getDeclaredField("ti")
                            tiField.isAccessible = true
                            tiSum += convertToDouble(tiField.get(shift))

                            val toField = shift.javaClass.getDeclaredField("to")
                            toField.isAccessible = true
                            toSum += convertToDouble(toField.get(shift))
                        } catch (e: Exception) {
                            // Ignore individual shift errors
                        }
                    }
                }
            } catch (e: Exception) {
                // No shifts field or error accessing it
            }

            val denom = tiSum + toSum
            val progress = if (denom > 0.0) (tiSum / denom).toFloat() else 0f
            val tooltip = "Worked: %.2f hrs, Break: %.2f hrs".format(tiSum, toSum)

            dateMap[date] = DayAttendanceInfo(date, statusColor, progress.coerceIn(0f,1f), tooltip)
        }
    }
    return dateMap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgSummaryScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val orgData = OrgDataManager.getOrgData()
    val context = LocalContext.current
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    val coroutineScope = rememberCoroutineScope()

    val refreshData = {
        coroutineScope.launch {
            try {
                val phone = DataStoreManager.getOrgPhone(context)
                if (phone != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    // TODO: Implement your API refresh logic here
                }
            } catch (e: Exception) {
                Log.e("OrgSummaryScreen", "Failed to refresh org", e)
            }
        }
    }

    var filterName by remember { mutableStateOf("All") }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val empList: List<OrgEmployee> = orgData?.employeeList ?: emptyList()
    val empCount = orgData?.employeeCount ?: empList.size

    // Prepare distinct names list with "All" option at top
    val distinctNames = remember(empList) {
        listOf("All") + empList.mapNotNull { emp ->
            try {
                val nameField = emp.javaClass.getDeclaredField("name")
                nameField.isAccessible = true
                val name = nameField.get(emp) as? String
                name?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }.distinct().sortedBy { it.lowercase(Locale.getDefault()) }
    }

    val calendarData = remember(empList, filterName, empCount) {
        buildOrgCalendarMap(empList, filterName, empCount)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(orgData?.orgName ?: "Organization Summary", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isLoggingEnabled) "ON" else "OFF",
                            color = Color.Gray
                        )
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filter Dropdown
            var dropdownExpanded by remember { mutableStateOf(false) }
            Box(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Button(onClick = { dropdownExpanded = true }) {
                    Text(filterName, fontWeight = FontWeight.Medium)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    distinctNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                filterName = name
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Calendar View
            OrgCalendarView(
                yearMonth = calendarMonth,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
                onMonthChange = { calendarMonth = it },
                attendanceByDate = calendarData
            )

            // Tooltip info for selected date
            calendarData[selectedDate]?.let { info ->
                Spacer(Modifier.height(18.dp))
                Text(
                    text = info.tooltip,
                    color = Color.DarkGray,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun SummaryInfo(label: String, value: Int, color: Color = Color.Gray) {
    Text(
        text = "$label: $value",
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
fun OrgCalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    attendanceByDate: Map<LocalDate, DayAttendanceInfo>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }) {
            Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
        IconButton(onClick = { onMonthChange(yearMonth.plusMonths(1)) }) {
            Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
            Text(
                dayLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
    Spacer(Modifier.height(6.dp))

    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = 42 // 6 weeks * 7 days

    val dayNumbers = (1..daysInMonth).toList()
    val cells = List(firstDayOffset) { null } + dayNumbers + List(totalCells - daysInMonth - firstDayOffset) { null }

    cells.chunked(7).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            week.forEach { dayNum ->
                val thisDate = dayNum?.let { yearMonth.atDay(it) }
                val info = attendanceByDate[thisDate]
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = thisDate != null) {
                            thisDate?.let(onSelect)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (dayNum != null && info != null) {
                        CircularDayCell(
                            day = dayNum,
                            color = info.color,
                            progress = info.progress,
                            selected = selectedDate == thisDate
                        )
                    } else if (dayNum != null) {
                        Text(
                            "$dayNum",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularDayCell(
    day: Int,
    color: Color,
    progress: Float,
    selected: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(40.dp)
            .then(if (selected) Modifier.border(2.dp, primaryColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background circle
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(day.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrgSummaryScreenPreview() {
    val navController = rememberNavController()
    OrgSummaryScreen(navController = navController)
}
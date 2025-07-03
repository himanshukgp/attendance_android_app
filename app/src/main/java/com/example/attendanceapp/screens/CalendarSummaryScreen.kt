package com.example.attendanceapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendanceapp.data.EmployeeDataManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.example.attendanceapp.screens.AppTopBar
import com.example.attendanceapp.data.DataStoreManager
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.content.Context
import com.example.attendanceapp.worker.LogStatusWorker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date

// Data class to represent the status of a day in the calendar
data class DayStatus(val date: LocalDate, val color: Color, val loggedHours: String, val status: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSummaryScreen(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isLoggingEnabled by remember { mutableStateOf(DataStoreManager.getWorkerToggleState(context)) }
    var currentMonth by remember { mutableStateOf(YearMonth.of(2025, 5)) }
    var selectedDate by remember { mutableStateOf(LocalDate.of(2025, 5, 28)) } // Changed to match your image

    // Initialize toggle state from DataStore
    LaunchedEffect(Unit) {
        isLoggingEnabled = DataStoreManager.getWorkerToggleState(context)
    }

    // Always use real data
    val attendanceData = EmployeeDataManager.getAttendanceData() ?: emptyMap()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Summary",
                isLoggingEnabled = isLoggingEnabled,
                onToggleChanged = {
                    isLoggingEnabled = it
                    com.example.attendanceapp.data.LogStatusManager.toggleLogging(context, it)
                },
                onRefresh = { /* TODO: Implement refresh logic if needed */ }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(48.dp)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    onClick = { navController.navigate("employeeAccount") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
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
                    onClick = { navController.navigate("calendarSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CalendarView(
                currentMonth = currentMonth,
                onMonthChange = { currentMonth = it },
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                attendanceData = attendanceData // pass real data
            )
            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 24.dp to 8.dp

            // Parse attendance data for selected day
            val selectedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val selectedDayData = attendanceData[selectedDateStr]

            if (selectedDayData != null) {
                val parts = selectedDayData.split(" | ")
                if (parts.size == 2) {
                    val hours = parts[0]
                    val status = parts[1]

                    Text(
                        text = "Logged: $hours hours on $selectedDateStr",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar for the selected day
                    val progress = calculateProgress(hours)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(status))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val encodedDate = URLEncoder.encode(selectedDateStr, "UTF-8")
                            navController.navigate("attendanceDetail/$encodedDate")
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    ) {
                        Text("See Details")
                    }
                }
            } else {
                Text(
                    text = "No data for ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val encodedDate = URLEncoder.encode(selectedDateStr, "UTF-8")
                        navController.navigate("attendanceDetail/$encodedDate")
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Text("See Details")
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    attendanceData: Map<String, String>
) {
    Column {
        CalendarHeader(currentMonth, onMonthChange)
        Spacer(modifier = Modifier.height(16.dp))
        CalendarGrid(currentMonth, selectedDate, onDateSelected, attendanceData)
    }
}

@Composable
fun CalendarHeader(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    attendanceData: Map<String, String>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value - DayOfWeek.MONDAY.value + 7) % 7

    // Parse attendance data for the current month
    val monthAttendanceData = mutableMapOf<Int, Pair<String, String>>()
    attendanceData.forEach { (dateStr, statusStr) ->
        try {
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            if (date.month == currentMonth.month && date.year == currentMonth.year) {
                val parts = statusStr.split(" | ")
                if (parts.size == 2) {
                    monthAttendanceData[date.dayOfMonth] = Pair(parts[0], parts[1])
                }
            }
        } catch (e: Exception) {
            // Skip invalid dates
        }
    }

    Column {
        // Day of week labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        val totalCells = 42 // 6 rows * 7 columns
        val days = (1..daysInMonth).toList()
        val emptyCells = List(firstDayOffset) { null }
        val allCells = emptyCells + days
        val remainingCells = List(totalCells - allCells.size) { null }
        val gridCells = (allCells + remainingCells).chunked(7)

        gridCells.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val date = currentMonth.atDay(day)
                            val attendanceInfo = monthAttendanceData[day]

                            if (attendanceInfo != null) {
                                val (hours, status) = attendanceInfo
                                val progress = calculateProgress(hours)
                                val statusColor = getStatusColor(status)

                                DayCellWithProgress(
                                    day = day,
                                    isSelected = date == selectedDate,
                                    statusColor = statusColor,
                                    progress = progress,
                                    onClick = { onDateSelected(date) }
                                )
                            } else {
                                DayCell(
                                    day = day,
                                    isSelected = date == selectedDate,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCellWithProgress(
    day: Int,
    isSelected: Boolean,
    statusColor: Color,
    progress: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Canvas for drawing the circular progress ring
        Canvas(modifier = Modifier.size(40.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

            // Background circle (light gray ring)
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc (blue progress ring)
            if (progress > 0f) {
                drawArc(
                    color = Color(0xFF2196F3), // Blue color for progress
                    startAngle = -90f, // Start from top
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        // Inner colored circle with day number
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = statusColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Selection indicator (optional - for highlighting selected date)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun DayCell(day: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

// Helper function to get status color
fun getStatusColor(status: String): Color {
    return when (status) {
        "P" -> Color(0xFF4CAF50) // Green for Present
        "A" -> Color(0xFFF44336) // Red for Absent
        "H" -> Color(0xFFFF9800) // Orange for Half day
        else -> Color.Gray
    }
}

// Helper function to calculate progress based on hours
fun calculateProgress(hours: String): Float {
    return try {
        val parts = hours.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toFloatOrNull() ?: 0f
            val m = parts[1].toFloatOrNull() ?: 0f
            val totalHours = h + (m / 60f)
            // Assuming 8 hours is full day (100% progress)
            (totalHours / 8f).coerceIn(0f, 1f)
        } else {
            0f
        }
    } catch (e: Exception) {
        0f
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarSummaryScreenPreview() {
    val navController = rememberNavController()
    CalendarSummaryScreen(navController = navController)
}
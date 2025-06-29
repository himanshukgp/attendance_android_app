package com.example.attendanceapp.screens

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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Data class to represent the status of a day in the calendar
data class DayStatus(val date: LocalDate, val color: Color, val loggedHours: String)

// Sample data based on the screenshot
val sampleDayStatus = mapOf(
    28 to DayStatus(LocalDate.of(2025, 5, 28), Color.Green, "08:00 hours"),
    29 to DayStatus(LocalDate.of(2025, 5, 29), Color.Red, "02:55 hours"),
    30 to DayStatus(LocalDate.of(2025, 5, 30), Color.hsl(39f, 1f, 0.5f), "05:30 hours"), // Orange
    31 to DayStatus(LocalDate.of(2025, 5, 31), Color.Green, "08:15 hours"),
    4 to DayStatus(LocalDate.of(2025, 6, 4), Color.hsl(39f, 1f, 0.5f), "04:00 hours") // Orange
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSummaryScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isOff by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.of(2025, 5)) }
    var selectedDate by remember { mutableStateOf(LocalDate.of(2025, 5, 29)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary", fontWeight = FontWeight.Bold) },
                actions = {
                    Text("OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isOff, onCheckedChange = { isOff = it }, modifier = Modifier.height(20.dp))
                    IconButton(onClick = { /* Refresh action */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    onClick = { navController.navigate("employeeAccount") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    selected = currentRoute == "attendanceDetail",
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Attendance") },
                    label = { Text("Attendance") },
                    onClick = { navController.navigate("attendanceDetail") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendarSummary",
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
                    label = { Text("Summary") },
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
                dayStatusMap = sampleDayStatus
            )
            Spacer(modifier = Modifier.height(24.dp))

            val selectedDayStatus = sampleDayStatus[selectedDate.dayOfMonth]
            if (selectedDayStatus != null && selectedDayStatus.date.month == currentMonth.month) {
                Text(
                    text = "Logged: ${selectedDayStatus.loggedHours} on ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("attendanceDetail") }) {
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
    dayStatusMap: Map<Int, DayStatus>
) {
    Column {
        CalendarHeader(currentMonth, onMonthChange)
        Spacer(modifier = Modifier.height(16.dp))
        CalendarGrid(currentMonth, selectedDate, onDateSelected, dayStatusMap)
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
    dayStatusMap: Map<Int, DayStatus>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value - DayOfWeek.MONDAY.value + 7) % 7

    Column {
        // Day of week labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            (DayOfWeek.MONDAY.value..DayOfWeek.SATURDAY.value).map { DayOfWeek.of(it) }.plus(DayOfWeek.SUNDAY).forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
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
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val date = currentMonth.atDay(day)
                            val status = dayStatusMap[day]
                            if (status != null) {
                                DayCell(
                                    day = day,
                                    isSelected = date == selectedDate,
                                    statusColor = if (status.date.month == currentMonth.month) status.color else null,
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
fun DayCell(day: Int, isSelected: Boolean, statusColor: Color?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .then(
                if (statusColor != null) {
                    Modifier.border(2.dp, statusColor, CircleShape)
                } else Modifier
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.toString(), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarSummaryScreenPreview() {
    val navController = rememberNavController()
    CalendarSummaryScreen(navController = navController)
}


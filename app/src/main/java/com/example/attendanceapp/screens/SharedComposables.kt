package com.example.attendanceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController

@Composable
fun TimelineBar(data: List<Pair<Color, Float>>, height: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
    ) {
        data.forEach { (color, weight) ->
            Box(
                modifier = Modifier
                    .background(color)
                    .weight(weight)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun OrgBottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "orgDashboard",
            icon = { Icon(Icons.Default.Person, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            onClick = { navController.navigate("orgDashboard") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
        NavigationBarItem(
            selected = currentRoute == "orgAttendance",
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
            label = { Text("Attendance") },
            onClick = { navController.navigate("orgAttendance") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
        NavigationBarItem(
            selected = currentRoute == "orgSummary",
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
            label = { Text("Summary") },
            onClick = { navController.navigate("orgSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
    }
} 
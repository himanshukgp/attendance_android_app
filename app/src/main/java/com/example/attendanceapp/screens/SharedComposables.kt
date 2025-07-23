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
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun TimelineBar(data: List<Pair<Color, Float>>, height: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
    ) {
        data.forEach { (color, weight) ->
            if (weight > 0f) {
                Box(
                    modifier = Modifier
                        .background(color)
                        .weight(weight)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun OrgBottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar(
        modifier = Modifier.height(80.dp)
    ) {
        NavigationBarItem(
            selected = currentRoute == "orgDashboard",
            icon = { Icon(Icons.Default.Person, contentDescription = "Dashboard") },
            onClick = { navController.navigate("orgDashboard") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
        NavigationBarItem(
            selected = currentRoute == "orgAttendance",
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
            onClick = { navController.navigate("orgAttendance") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
        NavigationBarItem(
            selected = currentRoute == "orgSummary",
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
            onClick = { navController.navigate("orgSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    isLoggingEnabled: Boolean = false,
    onToggleChanged: ((Boolean) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    navigationIcon: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { if (navigationIcon != null) ({ navigationIcon() }) else null },
        actions = {
            if (onToggleChanged != null) {
                Text(if (isLoggingEnabled) "ON" else "OFF", color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isLoggingEnabled,
                    onCheckedChange = onToggleChanged,
                    modifier = Modifier.height(20.dp)
                )
            }
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    )
}

@Composable
fun AppBottomBar(
    navController: NavController,
    currentRoute: String?,
    items: List<BottomBarItem>
) {
    NavigationBar(
        modifier = Modifier.height(48.dp)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                icon = { Icon(item.icon, contentDescription = item.label) },
                onClick = {
                    item.onClick?.invoke() ?: navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

data class BottomBarItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
    val onClick: (() -> Unit)? = null
) 
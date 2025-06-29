package com.example.attendanceapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
// Material 3 imports
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar // Changed from BottomNavigation
import androidx.compose.material3.NavigationBarItem // Changed from BottomNavigationItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeAccountScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isOff by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.Bold) },
                actions = {
                    Text("OFF", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isOff, onCheckedChange = { isOff = it }, modifier = Modifier.height(20.dp))
                    IconButton(onClick = { /* TODO: Refresh action */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "employeeAccount",
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    onClick = {
                        navController.navigate("employeeAccount") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    selected = currentRoute == "attendanceDetail",
                    icon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Attendance") },
                    label = { Text("Attendance") },
                    onClick = {
                        navController.navigate("attendanceDetail") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendarSummary",
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Summary") },
                    label = { Text("Summary") },
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
                .padding(16.dp)
        ) {
            Text("🧑 Nalla VasishttaKumar", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
            AccountInfoRow(
                icon = Icons.Default.CheckCircle,
                iconTint = Color.Green,
                line1 = "Location: 12.985364, 77.726184",
                line2 = "Org Location: 12.985366, 77.7261836"
            )
            AccountInfoRow(
                icon = Icons.Default.CheckCircle,
                iconTint = Color.Green,
                line1 = "Device ID:",
                line2 = "E9758369-39E6-4CBC-8AA4-F3B5DBFF0923"
            )
            AccountInfoRow(
                icon = Icons.Default.Cancel,
                iconTint = Color.Red,
                line1 = "WiFi: Unavailable",
                line2 = "Org SSID: ACT102726546387"
            )
            Spacer(Modifier.height(16.dp))
            AccountInfoRow(line1 = "Phone Number: +917762998869")
            AccountInfoRow(line1 = "Joining Date: 16/04/2025")
            AccountInfoRow(line1 = "Organization: Navi")
            AccountInfoRow(line1 = "Organization ID: A01")

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun AccountInfoRow(
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    line1: String,
    line2: String? = null
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Text(line1, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (line2 != null) {
                Text(line2, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
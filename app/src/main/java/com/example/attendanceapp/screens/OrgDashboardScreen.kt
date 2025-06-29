package com.example.attendanceapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgDashboardScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Organization Account", fontWeight = FontWeight.Bold) })
        },
        bottomBar = {
            // Using the same bottom bar as OrgAttendanceScreen for consistency
            OrgBottomBar(navController, currentRoute)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            InfoRow("Login Message", "Logged In Successfully")
            InfoRow("Organization Name", "Navi")
            InfoRow("Admin Title", "Tenda Web Master")
            InfoRow("SSID", "ACT102726546373")
            InfoRow("Latitude", "12.985366")
            InfoRow("Longitude", "77.7261836")
            InfoRow("Employee count", "16")
            Spacer(Modifier.weight(1f)) // Pushes the button to the bottom
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Log Out", color = Color.White)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Copied from OrgAttendanceScreen to avoid complex dependencies
// In a larger app, this would be moved to a shared file.
//@Composable
//fun OrgBottomBar(navController: NavController, currentRoute: String?) {
//    NavigationBar {
//        NavigationBarItem(
//            selected = currentRoute == "orgDashboard",
//            icon = { Icon(Icons.Default.Person, contentDescription = "Dashboard") },
//            label = { Text("Dashboard") },
//            onClick = { navController.navigate("orgDashboard") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//        NavigationBarItem(
//            selected = currentRoute == "orgAttendance",
//            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
//            label = { Text("Attendance") },
//            onClick = { navController.navigate("orgAttendance") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//        NavigationBarItem(
//            selected = currentRoute == "orgSummary",
//            icon = { Icon(Icons.Default.BarChart, contentDescription = "Summary") },
//            label = { Text("Summary") },
//            onClick = { navController.navigate("orgSummary") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
//        )
//    }
//}

@Preview(showBackground = true)
@Composable
fun OrgDashboardScreenPreview() {
    val navController = rememberNavController()
    OrgDashboardScreen(navController = navController)
}


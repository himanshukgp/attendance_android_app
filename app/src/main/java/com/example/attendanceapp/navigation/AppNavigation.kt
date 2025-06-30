package com.example.attendanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attendanceapp.screens.LoginScreen
import com.example.attendanceapp.screens.OrgLoginScreen
import com.example.attendanceapp.screens.EmployeeAccountScreen
import com.example.attendanceapp.screens.AttendanceDetailScreen
import com.example.attendanceapp.screens.CalendarSummaryScreen
import com.example.attendanceapp.screens.OrgDashboardScreen
import com.example.attendanceapp.screens.OrgAttendanceScreen
import com.example.attendanceapp.screens.OrgSummaryScreen

@Composable
fun AppNavigation(isLoggedIn: Boolean = false) {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) "employeeAccount" else "login"

    NavHost(navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onEmployeeLogin = { employeeData -> 
                    // Store employee data in a ViewModel or pass it through navigation
                    navController.navigate("employeeAccount") 
                },
                onOrgLogin = { navController.navigate("orgLogin") }
            )
        }
        composable("orgLogin") { OrgLoginScreen(navController) }
        composable("employeeAccount") { EmployeeAccountScreen(navController) }
        composable("attendanceDetail") { AttendanceDetailScreen(navController) }
        composable("calendarSummary") { CalendarSummaryScreen(navController) }
        composable("orgDashboard") { OrgDashboardScreen(navController) }
        composable("orgAttendance") { OrgAttendanceScreen(navController) }
        composable("orgSummary") { OrgSummaryScreen(navController) }
        // Add other screens here...
    }
}

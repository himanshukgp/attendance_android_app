package com.example.attendanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.attendanceapp.screens.AttendanceDetailScreen
import com.example.attendanceapp.screens.CalendarSummaryScreen
import com.example.attendanceapp.screens.EmployeeAccountScreen
import com.example.attendanceapp.screens.LoginScreen
import com.example.attendanceapp.screens.OrgAttendanceScreen
import com.example.attendanceapp.screens.OrgDashboardScreen
import com.example.attendanceapp.screens.OrgLoginScreen
import com.example.attendanceapp.screens.OrgSummaryScreen
import com.example.attendanceapp.screens.OnboardingScreen
import java.net.URLDecoder

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(onNext = { navController.navigate("login") { popUpTo("onboarding") { inclusive = true } } })
        }
        composable("login") {
            LoginScreen(
                onEmployeeLogin = {
                    navController.navigate("employeeAccount") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onOrgLogin = {
                    navController.navigate("orgLogin")
                }
            )
        }
        composable("orgLogin") { OrgLoginScreen(navController) }
        composable("employeeAccount") { EmployeeAccountScreen(navController) }
        composable(
            "attendanceDetail/{selectedDate}",
            arguments = listOf(navArgument("selectedDate") { type = NavType.StringType })
        ) { backStackEntry ->
            AttendanceDetailScreen(navController, backStackEntry.arguments?.getString("selectedDate"))
        }
        composable("calendarSummary") { CalendarSummaryScreen(navController) }
        composable("orgDashboard") { OrgDashboardScreen(navController) }
        composable("orgAttendance") { OrgAttendanceScreen(navController) }
        composable("orgSummary") { OrgSummaryScreen(navController) }
        // Add other screens here...
        // New route with parameters
        composable(
            route = "orgAttendance?date={date}&name={name}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")?.let {
                try {
                    URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    null
                }
            }
            val name = backStackEntry.arguments?.getString("name")?.let {
                try {
                    URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    null
                }
            }

            OrgAttendanceScreen(
                navController = navController,
                initialDate = date,
                initialName = name
            )
        }
    }
}

package com.example.attendanceapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.attendanceapp.navigation.AppNavigation
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.screens.EmployeeLoginResponse
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.attendanceapp.data.OrgDataManager
import com.example.attendanceapp.screens.OrgLoginResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val employeeJson = DataStoreManager.getEmployee(this)
        val orgJson = DataStoreManager.getOrg(this)

        val startDestination = if (employeeJson != null) {
            "employeeAccount"
        } else if (orgJson != null) {
            try {
                val orgData = Gson().fromJson(orgJson, com.example.attendanceapp.api.OrgLoginResponse::class.java)
                OrgDataManager.setOrgData(orgData)
                "orgDashboard"
            } catch (e: Exception) {
                "login"
            }
        } else {
            "login"
        }

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AttendanceAppTheme {
        Greeting("Android")
    }
}
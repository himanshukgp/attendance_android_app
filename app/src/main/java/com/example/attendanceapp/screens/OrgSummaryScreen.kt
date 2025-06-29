package com.example.attendanceapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
fun OrgSummaryScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Summary", fontWeight = FontWeight.Bold) })
        },
        bottomBar = {
            OrgBottomBar(navController, currentRoute)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SummaryInfo(label = "Total Employees", value = 15)
            Spacer(modifier = Modifier.height(16.dp))
            SummaryInfo(label = "Present", value = 8, color = Color.Green)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryInfo(label = "Absent", value = 7, color = Color.Red)
        }
    }
}

@Composable
fun SummaryInfo(label: String, value: Int, color: Color = Color.Gray) {
    Text(
        text = "$label: $value",
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Preview(showBackground = true)
@Composable
fun OrgSummaryScreenPreview() {
    val navController = rememberNavController()
    OrgSummaryScreen(navController = navController)
}


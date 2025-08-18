package com.example.attendanceapp.screens

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    var showPermissions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to AttendanceApp!\n\nTo use this app, you need to allow the following permissions:", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Text("- Location (All the time, precise)\n- Notifications\n- Background Service\n- Battery Optimization Exemption", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = {
            // Mark onboarding as complete and flag just completed
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("onboarding_complete", true).putBoolean("onboarding_just_completed", true).apply()
            showPermissions = true
        }) {
            Text("Next")
        }
    }

    if (showPermissions) {
        // Trigger permission requests by restarting the activity (MainActivity will handle it)
        LaunchedEffect(Unit) {
            (context as? Activity)?.recreate()
            onNext()
        }
    }
} 
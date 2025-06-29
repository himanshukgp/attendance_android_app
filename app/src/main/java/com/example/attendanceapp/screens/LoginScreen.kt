package com.example.attendanceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.EmployeeLoginRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Data classes for API response
data class EmployeeLoginResponse(
    val message: String,
    val Org_Name: String,
    val Org_ID: String,
    val Org_SSID: String,
    val Org_Lat: String,
    val Org_Lon: String,
    val Org_Admin_Title: String,
    val Name: String,
    val Phone_Number: String,
    val IMEI1: String,
    val IMEI2: String,
    val SSID: String,
    val DOJ: String,
    val DOL: String,
    val Date: String,
    val Status: String,
    val Hours: String,
    val IN_time: String,
    val Shifts: Map<String, Shift>,
    val attendanceData: Map<String, String> // For the date-based attendance data
)

data class Shift(
    val IN: String,
    val OUT: String,
    val Ti: String,
    val To: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onEmployeeLogin: (EmployeeLoginResponse) -> Unit,
    onOrgLogin: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(LoginScreenType.MAIN) }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (currentScreen != LoginScreenType.MAIN) {
                TopAppBar(
                    title = { Text("Employee Login") },
                    navigationIcon = {
                        IconButton(onClick = { 
                            currentScreen = LoginScreenType.MAIN
                            phoneNumber = ""
                            otp = ""
                            errorMessage = ""
                            showOtpField = false
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentScreen) {
                LoginScreenType.MAIN -> {
                    Text(
                        text = "Login",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )

                    Button(
                        onClick = { currentScreen = LoginScreenType.EMPLOYEE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                    ) {
                        Text("Login as Employee", color = Color.White)
                    }

                    Button(
                        onClick = onOrgLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
                    ) {
                        Text("Login as Organization", color = Color.White)
                    }
                }
                
                LoginScreenType.EMPLOYEE -> {
                    Text(
                        text = "Employee Login",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { 
                                    if (it.length <= 10) phoneNumber = it 
                                },
                                label = { Text("Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (showOtpField) {
                                OutlinedTextField(
                                    value = otp,
                                    onValueChange = { 
                                        if (it.length <= 6) otp = it 
                                    },
                                    label = { Text("OTP") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (!showOtpField) {
                                        // Request OTP
                                        if (phoneNumber.length == 10) {
                                            showOtpField = true
                                            errorMessage = ""
                                            // TODO: Call API to send OTP
                                        } else {
                                            errorMessage = "Please enter a valid 10-digit mobile number"
                                        }
                                    } else {
                                        // Verify OTP and login
                                        if (otp.length == 6) {
                                            performEmployeeLogin(
                                                phoneNumber = phoneNumber,
                                                otp = otp,
                                                onLoading = { isLoading = true },
                                                onSuccess = { response ->
                                                    isLoading = false
                                                    onEmployeeLogin(response)
                                                },
                                                onError = { error ->
                                                    isLoading = false
                                                    errorMessage = error
                                                }
                                            )
                                        } else {
                                            errorMessage = "Please enter a valid 6-digit OTP"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = if (!showOtpField) "Send OTP" else "Login",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class LoginScreenType {
    MAIN, EMPLOYEE
}

// Mock API call - replace with actual implementation
private fun performEmployeeLogin(
    phoneNumber: String,
    otp: String,
    onLoading: () -> Unit,
    onSuccess: (EmployeeLoginResponse) -> Unit,
    onError: (String) -> Unit
) {
    onLoading()
    
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val request = EmployeeLoginRequest(
                phone = phoneNumber,
                selected_date = currentDate,
                imei = "4f233e3a6d232212" // This should be obtained from device
            )
            
            val response = NetworkModule.apiService.employeeLogin(request)
            
            withContext(Dispatchers.Main) {
                onSuccess(response)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Login failed: ${e.message}")
            }
        }
    }
}

package com.example.attendanceapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavController
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.attendanceapp.api.NetworkModule
import com.example.attendanceapp.api.OrgLoginRequest
import com.example.attendanceapp.api.OrgLoginResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.data.OrgDataManager
import com.google.gson.Gson
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgLoginScreen(navController: NavController) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organization Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Organization Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            if (it.length <= 10) phone = it 
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
                            onValueChange = { if (it.length <= 6) otp = it },
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
                                if (phone.length == 10) {
                                    Log.d("OrgLoginScreen", "Valid phone number. Showing OTP field.")
                                    showOtpField = true
                                    errorMessage = ""
                                    // TODO: Call API to send OTP
                                } else {
                                    Log.w("OrgLoginScreen", "Invalid phone number length: ${phone.length}")
                                    errorMessage = "Please enter a valid 10-digit mobile number"
                                }
                            } else {
                                if (otp.length == 6) {
                                    Log.d("OrgLoginScreen", "Valid OTP. Starting login process.")
                                    performOrgLogin(
                                        phoneNumber = phone,
                                        otp = otp,
                                        onLoading = { 
                                            Log.d("OrgLoginScreen", "Setting loading state")
                                            isLoading = true 
                                        },
                                        onSuccess = { response ->
                                            Log.d("OrgLoginScreen", "Login successful, navigating to dashboard")
                                            isLoading = false
                                            val json = Gson().toJson(response)
                                            Log.d("OrgLoginScreen", "Saving organization data to DataStore: $json")
                                            DataStoreManager.saveOrg(navController.context, json)
                                            DataStoreManager.saveOrgPhone(navController.context, "+91$phone")
                                            OrgDataManager.setOrgData(response)
                                            navController.navigate("orgDashboard")
                                        },
                                        onError = { error ->
                                            Log.e("OrgLoginScreen", "Login failed: $error")
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                } else {
                                    Log.w("OrgLoginScreen", "Invalid OTP length: ${otp.length}")
                                    errorMessage = "Please enter a valid 6-digit OTP"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(if (!showOtpField) "Send OTP" else "Login", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun performOrgLogin(
    phoneNumber: String,
    otp: String,
    onLoading: () -> Unit,
    onSuccess: (OrgLoginResponse) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("OrgLoginScreen", "Starting organization login. Phone: $phoneNumber, OTP: $otp")
    onLoading()
    
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val request = OrgLoginRequest(
                phone = "+91$phoneNumber",
                selected_date = currentDate,
                otp = otp
            )
            
            Log.d("OrgLoginScreen", "API Request: phone=${request.phone}, date=${request.selected_date}, otp=${request.otp}")
            
            val response = NetworkModule.apiService.orgLogin(request)
            Log.d("OrgLoginScreen", "API Response: ${response.message}")
            
            withContext(Dispatchers.Main) {
                onSuccess(response)
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("OrgLoginScreen", "HTTP Error: ${e.code()}, Body: $errorBody", e)
            val errorMessage = "Network error (${e.code()}). Please try again."
            withContext(Dispatchers.Main) {
                onError(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("OrgLoginScreen", "API call failed", e)
            withContext(Dispatchers.Main) {
                onError("Login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrgLoginScreenPreview() {
    val navController = rememberNavController()
    OrgLoginScreen(navController = navController)
}
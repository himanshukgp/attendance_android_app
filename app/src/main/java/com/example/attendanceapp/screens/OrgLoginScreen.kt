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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgLoginScreen(navController: NavController) {
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                            Log.d("OrgLoginScreen", "Login button clicked")
                            if (phone.length == 10) {
                                Log.d("OrgLoginScreen", "Valid phone number entered, starting login")
                                performOrgLogin(
                                    phoneNumber = phone,
                                    onLoading = { 
                                        Log.d("OrgLoginScreen", "Setting loading state")
                                        isLoading = true 
                                    },
                                    onSuccess = { response ->
                                        Log.d("OrgLoginScreen", "Login successful, navigating to dashboard")
                                        isLoading = false
                                        navController.navigate("orgDashboard")
                                    },
                                    onError = { error ->
                                        Log.e("OrgLoginScreen", "Login failed: $error")
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            } else {
                                Log.w("OrgLoginScreen", "Invalid phone number length: ${phone.length}")
                                errorMessage = "Please enter a valid 10-digit mobile number"
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
                            Text("Login", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun performOrgLogin(
    phoneNumber: String,
    onLoading: () -> Unit,
    onSuccess: (OrgLoginResponse) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("OrgLoginScreen", "Starting organization login process")
    Log.d("OrgLoginScreen", "Phone: $phoneNumber")
    
    onLoading()
    
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val request = OrgLoginRequest(
                phone = "+91$phoneNumber", // Add country code
                selected_date = currentDate
            )
            
            Log.d("OrgLoginScreen", "API Request Details:")
            Log.d("OrgLoginScreen", "URL: ${NetworkModule.getBaseUrl()}ios_org_login")
            Log.d("OrgLoginScreen", "Request Body: phone=${request.phone}, selected_date=${request.selected_date}")
            
            Log.d("OrgLoginScreen", "Making API call...")
            val response = NetworkModule.apiService.orgLogin(request)
            
            Log.d("OrgLoginScreen", "API Response received successfully")
            Log.d("OrgLoginScreen", "Response Message: ${response.message}")
            Log.d("OrgLoginScreen", "Organization: ${response.orgName}")
            Log.d("OrgLoginScreen", "Status: ${response.status}")
            
            withContext(Dispatchers.Main) {
                Log.d("OrgLoginScreen", "Navigating to organization dashboard")
                onSuccess(response)
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("OrgLoginScreen", "HTTP Error: ${e.code()}", e)
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("OrgLoginScreen", "Error body: $errorBody")
            
            val errorMessage = when (e.code()) {
                400 -> "Invalid request. Please check your phone number and try again."
                401 -> "Unauthorized. Please check your credentials."
                404 -> "Service not found. Please try again later."
                500 -> "Server error. Please try again later."
                else -> "Network error (${e.code()}). Please try again."
            }
            
            withContext(Dispatchers.Main) {
                Log.e("OrgLoginScreen", "Showing HTTP error to user: $errorMessage")
                onError(errorMessage)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("OrgLoginScreen", "Network timeout", e)
            withContext(Dispatchers.Main) {
                onError("Request timed out. Please check your internet connection and try again.")
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("OrgLoginScreen", "No internet connection", e)
            withContext(Dispatchers.Main) {
                onError("No internet connection. Please check your network and try again.")
            }
        } catch (e: Exception) {
            Log.e("OrgLoginScreen", "API call failed", e)
            Log.e("OrgLoginScreen", "Error message: ${e.message}")
            Log.e("OrgLoginScreen", "Error type: ${e.javaClass.simpleName}")
            
            withContext(Dispatchers.Main) {
                val errorMessage = "Login failed: ${e.message ?: "Unknown error occurred"}"
                Log.e("OrgLoginScreen", "Showing error to user: $errorMessage")
                onError(errorMessage)
            }
        }
    }
}
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
import com.example.attendanceapp.data.EmployeeDataManager
import com.example.attendanceapp.utils.DeviceUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.util.Log
import android.content.Context
import com.google.gson.Gson
import com.example.attendanceapp.data.DataStoreManager
import com.example.attendanceapp.worker.LogStatusWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.google.gson.annotations.SerializedName
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// Data classes for API response
data class EmployeeLoginResponse(
    val message: String,
    @SerializedName("Org_Name") val orgName: String,
    @SerializedName("Org_ID") val orgId: String,
    @SerializedName("Org_SSID") val orgSsid: String,
    @SerializedName("Org_Lat") val orgLat: String,
    @SerializedName("Org_Lon") val orgLon: String,
    @SerializedName("Org_Admin_Title") val orgAdminTitle: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Phone_Number") val phoneNumber: String,
    @SerializedName("IMEI1") val imei1: String,
    @SerializedName("IMEI2") val imei2: String,
    @SerializedName("SSID") val ssid: String,
    @SerializedName("DOJ") val doj: String,
    @SerializedName("DOL") val dol: String,
    @SerializedName("Date") val date: String,
    @SerializedName("Status") val status: String,
    @SerializedName("Hours") val hours: String,
    @SerializedName("IN_time") val inTime: String,
    @SerializedName("Shifts") val shifts: Map<String, Shift>? = null,
    val attendanceData: Map<String, String>? = null // For the date-based attendance data
)

data class OrgLoginResponse(
    val message: String,
    @SerializedName("Org_Name") val orgName: String,
    @SerializedName("Org_ID") val orgId: String,
    @SerializedName("Org_SSID") val orgSsid: String,
    @SerializedName("Org_Lat") val orgLat: String,
    @SerializedName("Org_Lon") val orgLon: String,
    @SerializedName("Org_Admin_Title") val orgAdminTitle: String,
    @SerializedName("Phone_Number") val phoneNumber: String,
    @SerializedName("Date") val date: String,
    @SerializedName("Status") val status: String
)

data class Shift(
    @SerializedName("IN") val inn: String,
    @SerializedName("OUT") val out: String,
    @SerializedName("Ti") val ti: String,
    @SerializedName("To") val to: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onEmployeeLogin: (EmployeeLoginResponse) -> Unit,
    onOrgLogin: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                                context = context,
                                                phoneNumber = phoneNumber,
                                                otp = otp,
                                                onLoading = { isLoading = true },
                                                onSuccess = { response ->
                                                    isLoading = false
                                                    val json = Gson().toJson(response)
                                                    DataStoreManager.saveEmployee(context, json)
                                                    EmployeeDataManager.setEmployeeData(response)
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
    context: Context,
    phoneNumber: String,
    otp: String,
    onLoading: () -> Unit,
    onSuccess: (EmployeeLoginResponse) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("LoginScreen", "Starting employee login process")
    Log.d("LoginScreen", "Phone: $phoneNumber, OTP: $otp")
    
    onLoading()
    
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val deviceIMEI = DeviceUtils.getDeviceIMEI(context)
            Log.d("LoginScreen", "Using device IMEI: $deviceIMEI")
            
            val request = EmployeeLoginRequest(
                phone = "+91$phoneNumber", // Add country code
                selected_date = currentDate,
                imei = deviceIMEI
            )
            
            Log.d("LoginScreen", "API Request Details:")
            Log.d("LoginScreen", "URL: ${NetworkModule.getBaseUrl()}ios_emp_login")
            Log.d("LoginScreen", "Request Body: phone=${request.phone}, selected_date=${request.selected_date}, imei=${request.imei}")
            
            Log.d("LoginScreen", "Making API call...")
            val response = NetworkModule.apiService.employeeLogin(request)
            
            Log.d("LoginScreen", "API Response received successfully")
            Log.d("LoginScreen", "Response Message: ${response.message}")
            Log.d("LoginScreen", "Employee Name: ${response.name}")
            Log.d("LoginScreen", "Organization: ${response.orgName}")
            Log.d("LoginScreen", "Status: ${response.status}")
            Log.d("LoginScreen", "Hours: ${response.hours}")
            Log.d("LoginScreen", "Shifts: ${response.shifts}")
            Log.d("LoginScreen", "Shifts count: ${response.shifts?.size ?: 0}")
            Log.d("LoginScreen", "Attendance data: ${response.attendanceData}")
            Log.d("LoginScreen", "Attendance data count: ${response.attendanceData?.size ?: 0}")
            
            withContext(Dispatchers.Main) {
                Log.d("LoginScreen", "Navigating to employee account screen")
                EmployeeDataManager.setEmployeeData(response)
                DataStoreManager.saveEmployee(context, Gson().toJson(response))
                onSuccess(response)
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("LoginScreen", "HTTP Error: ${e.code()}", e)
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("LoginScreen", "Error body: $errorBody")
            
            val errorMessage = when (e.code()) {
                400 -> "Invalid request. Please check your phone number and try again."
                401 -> "Unauthorized. Please check your credentials."
                404 -> "Service not found. Please try again later."
                500 -> "Server error. Please try again later."
                else -> "Network error (${e.code()}). Please try again."
            }
            
            withContext(Dispatchers.Main) {
                Log.e("LoginScreen", "Showing HTTP error to user: $errorMessage")
                onError(errorMessage)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("LoginScreen", "Network timeout", e)
            withContext(Dispatchers.Main) {
                onError("Request timed out. Please check your internet connection and try again.")
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("LoginScreen", "No internet connection", e)
            withContext(Dispatchers.Main) {
                onError("No internet connection. Please check your network and try again.")
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "API call failed", e)
            Log.e("LoginScreen", "Error message: ${e.message}")
            Log.e("LoginScreen", "Error type: ${e.javaClass.simpleName}")
            
            withContext(Dispatchers.Main) {
                val errorMessage = "Login failed: ${e.message ?: "Unknown error occurred"}"
                Log.e("LoginScreen", "Showing error to user: $errorMessage")
                onError(errorMessage)
            }
        }
    }
}

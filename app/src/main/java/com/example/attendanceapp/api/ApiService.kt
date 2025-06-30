package com.example.attendanceapp.api

import com.example.attendanceapp.screens.EmployeeLoginResponse
import com.example.attendanceapp.screens.OrgLoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

data class EmployeeLoginRequest(
    val phone: String,
    val selected_date: String,
    val imei: String
)

data class OrgLoginRequest(
    val phone: String,
    val selected_date: String
)

data class ApiError(
    val error: String? = null,
    val message: String? = null
)

data class LogStatusRequest(
    val imei: String,
    val ssid: String,
    val latitude: String,
    val longitude: String,
    val timestamp: String
)

interface ApiService {
    @POST("ios_emp_login")
    suspend fun employeeLogin(@Body request: EmployeeLoginRequest): EmployeeLoginResponse
    
    @POST("ios_org_login")
    suspend fun orgLogin(@Body request: OrgLoginRequest): OrgLoginResponse

    @POST("log_status")
    suspend fun logStatus(@Body request: LogStatusRequest): retrofit2.Response<Unit>
} 
package com.example.attendanceapp.api

import com.example.attendanceapp.screens.EmployeeLoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

data class EmployeeLoginRequest(
    val phone: String,
    val selected_date: String,
    val imei: String
)

interface ApiService {
    @POST("ios_emp_login")
    suspend fun employeeLogin(@Body request: EmployeeLoginRequest): EmployeeLoginResponse
} 
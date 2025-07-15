package com.example.attendanceapp.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("ios_emp_login")
    suspend fun employeeLogin(@Body request: EmployeeLoginRequest): EmployeeLoginResponse
    
    @POST("ios_org_login")
    suspend fun orgLogin(@Body request: OrgLoginRequest): OrgLoginResponse

    @POST("log_status")
    suspend fun logStatus(@Body request: LogStatusRequest): retrofit2.Response<Unit>
    @POST("mark_attendance")
    suspend fun markAttendance(@Body body: Map<String, String>): retrofit2.Response<Unit>
} 
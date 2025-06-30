package com.example.attendanceapp.data

import com.example.attendanceapp.screens.EmployeeLoginResponse
import com.example.attendanceapp.screens.Shift

object EmployeeDataManager {
    private var employeeData: EmployeeLoginResponse? = null
    
    fun setEmployeeData(data: EmployeeLoginResponse) {
        employeeData = data
    }
    
    fun getEmployeeData(): EmployeeLoginResponse? {
        return employeeData
    }
    
    fun clearEmployeeData() {
        employeeData = null
    }
    
    fun getEmployeeName(): String {
        return employeeData?.name ?: "Unknown"
    }
    
    fun getOrganizationName(): String {
        return employeeData?.orgName ?: "Unknown"
    }
    
    fun getOrganizationId(): String {
        return employeeData?.orgId ?: "Unknown"
    }
    
    fun getPhoneNumber(): String {
        return employeeData?.phoneNumber ?: "Unknown"
    }
    
    fun getJoiningDate(): String {
        return employeeData?.doj ?: "Unknown"
    }
    
    fun getCurrentHours(): String {
        return employeeData?.hours ?: "00:00"
    }
    
    fun getLoginTime(): String {
        return employeeData?.inTime ?: "00:00:00"
    }
    
    fun getShifts(): Map<String, Shift>? {
        return employeeData?.shifts
    }
    
    fun getAttendanceData(): Map<String, String>? {
        return employeeData?.attendanceData
    }
    
    fun getLocation(): String {
        return "${employeeData?.orgLat ?: "0.0"}, ${employeeData?.orgLon ?: "0.0"}"
    }
    
    fun getDeviceId(): String {
        return employeeData?.imei1 ?: "Unknown"
    }
    
    fun getWiFiStatus(): String {
        return if (employeeData?.ssid.isNullOrEmpty()) "Unavailable" else "Available"
    }
    
    fun getOrgSSID(): String {
        return employeeData?.orgSsid ?: "Unknown"
    }
} 
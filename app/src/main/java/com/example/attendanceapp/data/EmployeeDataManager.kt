package com.example.attendanceapp.data
import com.example.attendanceapp.api.EmployeeLoginResponse
import com.example.attendanceapp.api.Shift
import com.google.gson.annotations.SerializedName

data class EmployeeLoginResponse(
    val message: String,
    @SerializedName("Org_Name") val orgName: String,
    @SerializedName("Org_ID") val orgId: String,
    @SerializedName("Org_SSID") val orgSsid: String,
    @SerializedName("Org_Lat") val orgLat: String,
    @SerializedName("Org_Lon") val orgLon: String,
    @SerializedName("Org_Admin_Title") val orgAdminTitle: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Phone Number") val phoneNumber: String,
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

object EmployeeDataManager {
    private var employeeData: EmployeeLoginResponse? = null
    private var attendanceData: Map<String, String>? = null
    
    fun setEmployeeData(data: EmployeeLoginResponse) {
        employeeData = data
        // Extract attendance data from the response
        attendanceData = data.attendanceData ?: data.run {
            // Fallback: collect all keys that look like dates (dd/MM/yyyy)
            this::class.java.declaredFields
                .filter { it.type == String::class.java && it.name.matches(Regex("\\d{2}_\\d{2}_\\d{4}")) }
                .mapNotNull { field ->
                    field.isAccessible = true
                    val value = field.get(this) as? String
                    val key = field.name.replace('_', '/')
                    if (value != null) key to value else null
                }.toMap().ifEmpty {
                    // Try to collect all map entries that look like dates
                    this::class.java.declaredFields
                        .filter { it.type == Map::class.java }
                        .flatMap { field ->
                            field.isAccessible = true
                            val map = field.get(this) as? Map<*, *>
                            map?.entries?.filter { (k, v) ->
                                k is String && k.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) && v is String
                            }?.map { (k, v) -> k as String to v as String } ?: emptyList()
                        }.toMap()
                }
        }
    }
    
    fun getEmployeeData(): EmployeeLoginResponse? {
        return employeeData
    }
    
    fun clearEmployeeData() {
        employeeData = null
        attendanceData = null
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
        return attendanceData
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
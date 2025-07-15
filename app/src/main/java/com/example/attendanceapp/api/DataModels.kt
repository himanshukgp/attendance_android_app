package com.example.attendanceapp.api

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.Gson
import java.lang.reflect.Type
import kotlin.text.Regex

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

class EmployeeLoginResponseDeserializer : JsonDeserializer<EmployeeLoginResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmployeeLoginResponse {
        val jsonObj = json.asJsonObject
        val attendanceMap = mutableMapOf<String, String>()
        val dateRegex = Regex("\\d{2}/\\d{2}/\\d{4}")
        for ((key, value) in jsonObj.entrySet()) {
            if (dateRegex.matches(key) && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                attendanceMap[key] = value.asString
            }
        }
        // Remove attendanceData if present to avoid double parsing
        val cleanedJson = jsonObj.deepCopy()
        cleanedJson.remove("attendanceData")
        // Parse the rest of the fields as normal
        val base = Gson().fromJson(cleanedJson, EmployeeLoginResponse::class.java)
        return base.copy(attendanceData = attendanceMap)
    }
}

data class OrgLoginResponse(
    val message: String,
    @SerializedName("Org_name") val orgName: String,
    @SerializedName("Org_ID") val orgId: String,
    @SerializedName("Org_SSID") val orgSsid: String,
    @SerializedName("Org_Lat") val orgLat: String,
    @SerializedName("Org_Lon") val orgLon: String,
    @SerializedName("Org_Admin_Title") val orgAdminTitle: String,
    @SerializedName("Attendance_Total") val attendanceTotal: Int,
    @SerializedName("Attendance_Present") val attendancePresent: Int,
    @SerializedName("Attendance_Absent") val attendanceAbsent: Int,
    @SerializedName("Employee_Count") val employeeCount: Int,
    @SerializedName("Employee_Present") val employeePresent: Int,
    @SerializedName("Employee_Absent") val employeeAbsent: Int,
    @SerializedName("Employee_List") val employeeList: List<OrgEmployee>
)

data class OrgEmployee(
    val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Phone Number") val phoneNumber: String,
    @SerializedName("Date") val date: String,
    @SerializedName("IMEI") val imei: String,
    @SerializedName("DOJ") val doj: String,
    @SerializedName("Status") val status: String,
    @SerializedName("Hours") val hours: String,
    @SerializedName("IN_time") val inTime: String,
    @SerializedName("Marked") val marked: String,
    @SerializedName("Shifts") val shifts: Map<String, OrgShift>
)

data class OrgShift(
    @SerializedName("IN") val inn: String,
    @SerializedName("OUT") val out: String,
    @SerializedName("Ti") val ti: Any,
    @SerializedName("To") val to: Any
)

data class Shift(
    @SerializedName("IN") val inn: String,
    @SerializedName("OUT") val out: String,
    @SerializedName("Ti") val ti: String,
    @SerializedName("To") val to: String
)

data class EmployeeLoginRequest(
    val phone: String,
    val selected_date: String,
    val imei: String
)

data class OrgLoginRequest(
    val phone: String,
    val selected_date: String,
    val otp: String? = null
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
    val timestamp: String,
    val phone: String
) 
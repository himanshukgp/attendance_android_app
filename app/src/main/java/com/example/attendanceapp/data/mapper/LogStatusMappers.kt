package com.example.attendanceapp.data.mapper

import com.example.attendanceapp.api.LogStatusRequest
import com.example.attendanceapp.data.local.LogStatusEntity

fun LogStatusRequest.toEntity(): LogStatusEntity =
    LogStatusEntity(
        imei = imei,
        ssid = ssid,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        phone = phone
    )

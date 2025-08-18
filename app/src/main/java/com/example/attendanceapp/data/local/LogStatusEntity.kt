package com.example.attendanceapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_status")
data class LogStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val imei: String?,
    val ssid: String?,
    val latitude: String?,
    val longitude: String?,
    val timestamp: String,
    val phone: String?,
    val syncState: SyncState = SyncState.PENDING, // PENDING, SENT, FAILED
    val createdAt: Long = System.currentTimeMillis()
)

enum class SyncState { PENDING, SENT, FAILED }

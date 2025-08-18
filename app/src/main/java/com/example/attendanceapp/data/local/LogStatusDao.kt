package com.example.attendanceapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LogStatusDao {
    @Upsert
    suspend fun upsert(e: LogStatusEntity): Long

    @Query("UPDATE log_status SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: Long, state: SyncState)

    @Query("SELECT * FROM log_status ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LogStatusEntity>>
}

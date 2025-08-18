package com.example.attendanceapp.data.repo

import android.content.Context
import com.example.attendanceapp.data.local.AppDb
import com.example.attendanceapp.data.local.LogStatusEntity
import com.example.attendanceapp.data.local.SyncState

class LogStatusRepository private constructor(context: Context) {
    private val dao = AppDb.get(context).logStatusDao()

    suspend fun savePending(e: LogStatusEntity): Long = dao.upsert(e)
    suspend fun markSent(id: Long) = dao.setSyncState(id, SyncState.SENT)
    suspend fun markFailed(id: Long) = dao.setSyncState(id, SyncState.FAILED)

    companion object {
        @Volatile private var INSTANCE: LogStatusRepository? = null
        fun get(context: Context): LogStatusRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogStatusRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

package com.example.attendanceapp.data

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.attendanceapp.api.OrgLoginResponse

object OrgDataManager {
    private val orgDataState = mutableStateOf<OrgLoginResponse?>(null)
    val orgData: State<OrgLoginResponse?> = orgDataState

    fun setOrgData(data: OrgLoginResponse) {
        Log.d("OrgDataManager", "Setting organization data")
        orgDataState.value = data
    }

    fun getOrgData(): OrgLoginResponse? {
        Log.d("OrgDataManager", "Getting organization data")
        return orgDataState.value
    }

    fun clearOrgData() {
        Log.d("OrgDataManager", "Clearing organization data")
        orgDataState.value = null
    }
} 
package com.example.attendanceapp.data

import com.example.attendanceapp.screens.OrgLoginResponse
import android.util.Log

object OrgDataManager {
    private var orgData: OrgLoginResponse? = null

    fun setOrgData(data: OrgLoginResponse) {
        Log.d("OrgDataManager", "Setting organization data: $data")
        orgData = data
    }

    fun getOrgData(): OrgLoginResponse? {
        Log.d("OrgDataManager", "Getting organization data: $orgData")
        return orgData
    }

    fun clearOrgData() {
        Log.d("OrgDataManager", "Clearing organization data")
        orgData = null
    }
} 
package com.example.attendanceapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "user_prefs")

private val KEY_EMPLOYEE_JSON = stringPreferencesKey("employee_json")
private val KEY_ORG_JSON = stringPreferencesKey("org_json")

object DataStoreManager {
    fun saveEmployee(context: Context, json: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_EMPLOYEE_JSON.name, json).apply()
    }

    fun getEmployee(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_EMPLOYEE_JSON.name, null)
    }

    fun clearEmployee(context: Context) {
        Log.d("DataStoreManager", "Clearing employee data")
        val prefs = getPreferences(context)
        prefs.edit().remove(KEY_EMPLOYEE_JSON.name).apply()
    }

    fun saveOrg(context: Context, json: String) {
        Log.d("DataStoreManager", "Saving org data: $json")
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_ORG_JSON.name, json).apply()
    }

    fun getOrg(context: Context): String? {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_ORG_JSON.name, null)
        Log.d("DataStoreManager", "Getting org data: $json")
        return json
    }

    fun clearOrg(context: Context) {
        Log.d("DataStoreManager", "Clearing org data")
        val prefs = getPreferences(context)
        prefs.edit().remove(KEY_ORG_JSON.name).apply()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
}
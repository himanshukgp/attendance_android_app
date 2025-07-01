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
private const val KEY_WORKER_TOGGLE = "worker_toggle"
private const val KEY_ORG_PHONE = "org_phone"
private const val KEY_EMPLOYEE_PHONE = "employee_phone"
private const val KEY_SELECTED_DATE = "selected_date"

object DataStoreManager {
    fun saveEmployee(context: Context, json: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_EMPLOYEE_JSON.name, json).commit()
    }

    fun getEmployee(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_EMPLOYEE_JSON.name, null)
    }

    fun clearEmployee(context: Context) {
        Log.d("DataStoreManager", "Clearing employee data")
        val prefs = getPreferences(context)
        prefs.edit().remove(KEY_EMPLOYEE_JSON.name).commit()
    }

    fun saveOrg(context: Context, json: String) {
        Log.d("DataStoreManager", "Saving org data: $json")
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_ORG_JSON.name, json).commit()
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
        prefs.edit().remove(KEY_ORG_JSON.name).commit()
    }

    fun saveOrgPhone(context: Context, phone: String) {
        val prefs = getPreferences(context)
        Log.d("DataStoreManager", "Saving org phone: $phone")
        prefs.edit().putString(KEY_ORG_PHONE, phone).commit()
    }

    fun getOrgPhone(context: Context): String? {
        val prefs = getPreferences(context)
        val phone = prefs.getString(KEY_ORG_PHONE, null)
        Log.d("DataStoreManager", "Getting org phone: $phone")
        return phone
    }

    fun saveWorkerToggleState(context: Context, isEnabled: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit().putBoolean(KEY_WORKER_TOGGLE, isEnabled).commit()
    }

    fun getWorkerToggleState(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(KEY_WORKER_TOGGLE, false)
    }

    fun saveEmployeePhone(context: Context, phone: String) {
        val prefs = getPreferences(context)
        Log.d("DataStoreManager", "Saving employee phone: $phone")
        prefs.edit().putString(KEY_EMPLOYEE_PHONE, phone).commit()
    }

    fun getEmployeePhone(context: Context): String? {
        val prefs = getPreferences(context)
        val phone = prefs.getString(KEY_EMPLOYEE_PHONE, null)
        Log.d("DataStoreManager", "Getting employee phone: $phone")
        return phone
    }

    fun saveSelectedDate(context: Context, date: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_SELECTED_DATE, date).commit()
    }

    fun getSelectedDate(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_SELECTED_DATE, null)
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
}
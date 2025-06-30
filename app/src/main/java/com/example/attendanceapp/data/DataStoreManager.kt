package com.example.attendanceapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object DataStoreManager {
    private val KEY_EMPLOYEE_JSON = stringPreferencesKey("employee_json")

    fun saveEmployee(context: Context, json: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[KEY_EMPLOYEE_JSON] = json
            }
        }
    }

    fun getEmployee(context: Context): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[KEY_EMPLOYEE_JSON]
        }
    }

    fun clearEmployee(context: Context) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_EMPLOYEE_JSON)
            }
        }
    }
}
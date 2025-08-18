package com.example.attendanceapp.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://eagle-app-dd32851971ff.herokuapp.com/"
    
    fun getBaseUrl(): String = BASE_URL
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("NetworkModule", "HTTP: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("NetworkModule", "=== REQUEST DETAILS ===")
            Log.d("NetworkModule", "Full URL: ${request.url}")
            Log.d("NetworkModule", "Base URL: $BASE_URL")
            Log.d("NetworkModule", "Request method: ${request.method}")
            Log.d("NetworkModule", "Request headers: ${request.headers}")
            
            val response = chain.proceed(request)
            Log.d("NetworkModule", "=== RESPONSE DETAILS ===")
            Log.d("NetworkModule", "Response code: ${response.code}")
            Log.d("NetworkModule", "Response message: ${response.message}")
            Log.d("NetworkModule", "Response URL: ${response.request.url}")
            
            response
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(EmployeeLoginResponse::class.java, EmployeeLoginResponseDeserializer())
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
    
    init {
        Log.d("NetworkModule", "=== NETWORK MODULE INITIALIZATION ===")
        Log.d("NetworkModule", "Base URL: $BASE_URL")
        Log.d("NetworkModule", "Retrofit base URL: ${retrofit.baseUrl()}")
        Log.d("NetworkModule", "NetworkModule initialized successfully")
    }
} 
package com.example.rescuerobotcontroller.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Client Singleton
 * Server: http://192.168.137.1:8000
 */
object RetrofitClient {
    
    // Server IP ve Port
    private const val BASE_URL = "http://192.168.137.1:8000/"
    
    /**
     * Lazy-initialized Retrofit API instance
     */
    val api: RescueApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RescueApiService::class.java)
    }
}

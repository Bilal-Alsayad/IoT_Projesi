package com.example.rescuerobotcontroller.data.api

import com.example.rescuerobotcontroller.data.models.*
import retrofit2.http.*

/**
 * Rescue Robot REST API Service
 * Base URL: http://192.168.137.1:8000/
 */
interface RescueApiService {
    
    /**
     * Tüm cihazların harita pozisyonlarını getirir
     * @return Map<DeviceName, DevicePosition>
     */
    @GET("/get-map")
    suspend fun getMap(): Map<String, DevicePosition>
    
    /**
     * Robot durum bilgisini getirir
     * @return RobotStatus
     */
    @GET("/robot-status")
    suspend fun getRobotStatus(): RobotStatus
    
    /**
     * Robot'a komut gönderir
     * @param cmd CommandRequest(command = "F"/"B"/"L"/"R"/"S"/"A"/"M")
     * @return CommandResponse
     */
    @POST("/robot-command")
    suspend fun sendCommand(@Body cmd: CommandRequest): CommandResponse
    
    /**
     * Robot modunu değiştirir
     * @param mode "auto" veya "manual"
     */
    @POST("/set-mode/{mode}")
    suspend fun setMode(@Path("mode") mode: String)
}

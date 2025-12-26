package com.example.rescuerobotcontroller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rescuerobotcontroller.data.api.RetrofitClient
import com.example.rescuerobotcontroller.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Robot Control ViewModel
 * Server ile iletişim, state management
 */
class RobotControlViewModel : ViewModel() {
    
    // ========== State Flows ==========
    
    private val _robotStatus = MutableStateFlow<RobotStatus?>(null)
    val robotStatus: StateFlow<RobotStatus?> = _robotStatus
    
    private val _mapData = MutableStateFlow<Map<String, DevicePosition>>(emptyMap())
    val mapData: StateFlow<Map<String, DevicePosition>> = _mapData
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    // ========== Initialization ==========
    
    init {
        startPolling()
    }
    
    // ========== Polling ==========
    
    /**
     * Server'dan sürekli veri çekme (1 saniyede bir)
     */
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Harita verisini çek
                    _mapData.value = RetrofitClient.api.getMap()
                    
                    // Robot durumunu çek
                    _robotStatus.value = RetrofitClient.api.getRobotStatus()
                    
                    // Bağlantı başarılı
                    _isConnected.value = true
                    _errorMessage.value = null
                    
                } catch (e: Exception) {
                    // Bağlantı hatası
                    _isConnected.value = false
                    _errorMessage.value = "Server bağlantısı hatası: ${e.message}"
                }
                
                // 1 saniye bekle
                delay(1000)
            }
        }
    }
    
    // ========== Commands ==========
    
    /**
     * Robot'a komut gönder
     */
    fun sendCommand(command: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.sendCommand(CommandRequest(command))
                // Başarılı
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Komut gönderme hatası: ${e.message}"
            }
        }
    }
    
    /**
     * Robot modunu değiştir
     */
    fun setMode(mode: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.setMode(mode)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Mod değiştirme hatası: ${e.message}"
            }
        }
    }
    
    // ========== Quick Commands ==========
    
    fun moveForward() = sendCommand("F")
    fun moveBackward() = sendCommand("B")
    fun turnLeft() = sendCommand("L")
    fun turnRight() = sendCommand("R")
    fun stop() = sendCommand("S")
    fun setAutoMode() = setMode("auto")
    fun setManualMode() = setMode("manual")
}

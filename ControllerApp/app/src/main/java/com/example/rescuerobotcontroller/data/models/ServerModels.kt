package com.example.rescuerobotcontroller.data.models

/**
 * Server'dan gelen cihaz pozisyon verisi
 */
data class DevicePosition(
    val type: String,        // "ROBOT", "VICTIM", "BLE"
    val x: Float,
    val y: Float,
    val priority: String?,   // "HIGH", "MEDIUM", "LOW" (sadece victim için)
    val last_updated: String // ISO timestamp
)

/**
 * RSSI (Sinyal Gücü) verisi
 */
data class RssiData(
    val robot: Map<String, Int>?,   // {"Anchor_1": -75, "Anchor_2": -68}
    val victim: Map<String, Int>?,  // {"Anchor_1": -82, "Anchor_2": -65}
    val average: Float?             // Ortalama RSSI
)

/**
 * Robot durum bilgisi
 */
data class RobotStatus(
    val mode: String,                    // "AUTO", "MANUAL"
    val robot_position: Position?,
    val victim_position: Position?,
    val distance_to_victim: Float?,      // Metre cinsinden
    val pending_command: String?,        // Son bekleyen komut
    val rssi: RssiData?                  // RSSI sinyal gücü verileri
)

/**
 * Pozisyon bilgisi
 */
data class Position(
    val x: Float,
    val y: Float,
    val priority: String? = null
)

/**
 * Komut gönderme isteği
 */
data class CommandRequest(
    val command: String  // "F", "B", "L", "R", "S", "A", "M"
)

/**
 * Komut cevabı
 */
data class CommandResponse(
    val status: String,   // "OK", "ERROR"
    val command: String,  
    val mode: String      // Mevcut mod
)

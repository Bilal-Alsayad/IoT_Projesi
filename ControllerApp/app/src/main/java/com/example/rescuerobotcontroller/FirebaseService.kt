package com.example.rescuerobotcontroller

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseService {
    
    private val database: FirebaseDatabase = Firebase.database
    val rootRef = database.getReference("rescuebot")
    
    companion object {
        private const val ROBOT_ID = "robot_001"
    }
    
    fun initializeRobot() {
        val infoRef = rootRef.child("robots").child(ROBOT_ID).child("info")
        val info = mapOf(
            "mc" to "ESP32-WROOM-32",
            "motor_driver" to "L298N",
            "power" to "2x18650"
        )
        infoRef.setValue(info)
        updateRobotStatus("IDLE")
    }
    
    fun updateRobotStatus(mode: String, batteryV: Float = 7.4f) {
        val statusRef = rootRef.child("robots").child(ROBOT_ID).child("status")
        val updates = mapOf(
            "mode" to mode,
            "battery_v" to batteryV,
            "last_seen" to System.currentTimeMillis()
        )
        statusRef.updateChildren(updates)
    }
    
    fun logTelemetry(direction: String, motorCommand: String, distanceCm: Float = 0f) {
        val telemetryRef = rootRef.child("telemetry").child(ROBOT_ID).push()
        val data = mapOf(
            "direction" to direction,
            "motor_command" to motorCommand,
            "distance_cm" to distanceCm,
            "timestamp" to System.currentTimeMillis()
        )
        telemetryRef.setValue(data)
    }
    
    fun logVictim(rssi: Int, helpFlag: Boolean): String {
        val victimRef = rootRef.child("victims").push()
        val victimId = victimRef.key ?: "victim_${System.currentTimeMillis()}"
        
        val data = mapOf(
            "uuid" to "unknown",
            "rssi" to rssi,
            "help_flag" to helpFlag,
            "detected_by" to ROBOT_ID,
            "timestamp" to System.currentTimeMillis()
        )
        victimRef.setValue(data)
        
        return victimId
    }
}

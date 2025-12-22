package com.selim.victimapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BleForegroundService : Service() {

    private var scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
    private var mediaPlayer: MediaPlayer? = null
    private var isScanning = false

    private lateinit var advertiserManager: BleAdvertiserManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        advertiserManager = BleAdvertiserManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        startScanning()

        if (!AppState.isPriorityClicked) {
            advertiserManager.startAdvertising(isPriority = false)
        }

        if (intent?.action == "ACTIVATE_PRIORITY") {
            advertiserManager.startAdvertising(isPriority = true)
            AppState.isPriorityClicked = true
        }

        return START_STICKY
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val deviceName = it.scanRecord?.deviceName ?: it.device.name ?: "Unknown"
                val rssi = it.rssi

                if (deviceName == "RescueBot") {
                    Log.d("BLE_SERVICE", "Robot Sinyali: $rssi")

                    // UI güncellensin
                    AppState.rssi = rssi

                    // --- YENİ OTOMATİK MANTIK ---

                    if (rssi >= -80) {
                        // DURUM 3: Robot Geldi (-80 ve üzeri)
                        // Alarmı OTOMATİK DURDUR
                        stopAlarm()
                    }
                    else if (rssi >= -98) {
                        // DURUM 2: Robot Yaklaşıyor (-98 ile -81 arası)
                        // Alarm ÇALSIN
                        playAlarm()
                    }
                    else {
                        // DURUM 1: Robot Uzak (-99 ve altı)
                        // Alarmı DURDUR (Ne olur ne olmaz)
                        stopAlarm()
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE_SERVICE", "Tarama Hatası: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (isScanning || scanner == null) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        scanner?.startScan(null, settings, scanCallback)
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        scanner?.stopScan(scanCallback)
        isScanning = false
        stopAlarm()
    }

    private fun playAlarm() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) return // Zaten çalıyorsa elleme
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.siren)
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("BLE_SERVICE", "Ses hatası: ${e.message}")
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        advertiserManager.stopAdvertising()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "VictimServiceChannel",
                "Robot Arama Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "VictimServiceChannel")
            .setContentTitle("Robot Aranıyor")
            .setContentText("Arka plan taraması aktif.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }
}
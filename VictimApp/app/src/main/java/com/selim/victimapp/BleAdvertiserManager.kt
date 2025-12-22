package com.selim.victimapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

class BleAdvertiserManager(private val context: Context) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser
    private var currentCallback: AdvertiseCallback? = null
    private var originalName: String? = null

    companion object {
        val UUID_NORMAL = ParcelUuid(UUID.fromString("00001802-0000-1000-8000-00805f9b34fb"))
        val UUID_PRIORITY = ParcelUuid(UUID.fromString("00001809-0000-1000-8000-00805f9b34fb"))
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(isPriority: Boolean) {
        stopAdvertising()
        if (originalName == null) {
            originalName = adapter?.name
        }
        adapter?.name = "SOS_TARGET"

        val name = adapter?.name
        Log.e("BLE_ADV", "ADAPTER NAME : $name")

        Handler(Looper.getMainLooper()).postDelayed({
            startAdvertisingInternal(isPriority)
        }, 200)
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingInternal(isPriority: Boolean) {
        val serviceUuid = if (isPriority) UUID_PRIORITY else UUID_NORMAL

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .build()

        currentCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i("BLE_ADV", "YAYIN BAŞLADI (UUID ODAKLI)")
                Log.i("BLE_ADV", "UUID: $serviceUuid")
                Log.i("BLE_ADV", "Cihaz Adı: ${adapter?.name}")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("BLE_ADV", "Yayın Hatası Kodu: $errorCode")
            }
        }

        advertiser?.startAdvertising(settings, data, scanResponse, currentCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        currentCallback?.let {
            advertiser?.stopAdvertising(it)
        }
        currentCallback = null

        originalName?.let {
            adapter?.name = it
        }
    }
}
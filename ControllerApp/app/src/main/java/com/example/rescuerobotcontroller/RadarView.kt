package com.example.rescuerobotcontroller

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RadarView(context: Context) : View(context) {

    data class BLEDevice(
        val address: String,
        val rssi: Int,
        val isVictim: Boolean,
        val angle: Float,
        val distance: Float
    )

    private val devices = mutableMapOf<String, BLEDevice>()
    private var sweepAngle = 0f

    private val paintGrid = Paint().apply {
        color = Color.parseColor("#2A2A2A")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val paintSweep = Paint().apply {
        color = Color.parseColor("#00BCD4")
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintDevice = Paint().apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintVictim = Paint().apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(centerX, centerY) - 50

        // Draw circles
        for (i in 1..4) {
            val r = radius * i / 4
            canvas.drawCircle(centerX, centerY, r, paintGrid)
        }

        // Draw cross lines
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paintGrid)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paintGrid)

        // Draw sweep line
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepX = centerX + (radius * cos(sweepRad)).toFloat()
        val sweepY = centerY + (radius * sin(sweepRad)).toFloat()
        canvas.drawLine(centerX, centerY, sweepX, sweepY, paintSweep)

        // Draw devices
        for (device in devices.values) {
            val angle = Math.toRadians(device.angle.toDouble())
            val dist = device.distance * radius / 100f
            val x = centerX + (dist * cos(angle)).toFloat()
            val y = centerY + (dist * sin(angle)).toFloat()

            val paint = if (device.isVictim) paintVictim else paintDevice
            canvas.drawCircle(x, y, 10f, paint)
            
            // RSSI değerini göster
            val rssiText = "${device.rssi} dBm"
            paintText.textSize = 20f
            canvas.drawText(rssiText, x + 15f, y + 5f, paintText)
        }

        // Draw device count
        paintText.textSize = 24f
        canvas.drawText("Devices: ${devices.size}", 20f, 40f, paintText)

        // Update sweep
        sweepAngle = (sweepAngle + 2f) % 360f
        postInvalidateDelayed(50)
    }

    fun addDevice(address: String, rssi: Int, isVictim: Boolean) {
        val distance = ((rssi + 100) * 100 / 70f).coerceIn(10f, 100f)
        val angle = (address.hashCode() % 360).toFloat()
        
        devices[address] = BLEDevice(address, rssi, isVictim, angle, distance)
        invalidate()
    }

    fun clear() {
        devices.clear()
        invalidate()
    }
}

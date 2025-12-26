package com.example.rescuerobotcontroller

import android.content.Context
import android.graphics.*
import android.view.View

class RobotMapView(context: Context) : View(context) {

    data class Position(val x: Float, val y: Float)

    private val path = mutableListOf<Position>()
    private val obstacles = mutableListOf<Position>()
    private val victims = mutableListOf<Position>()
    
    private var currentX = 0f
    private var currentY = 0f
    private var currentAngle = 0f
    private var radarStrength = 0

    // Paints
    private val paintPath = Paint().apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val paintRobot = Paint().apply {
        color = Color.parseColor("#00BCD4")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintObstacle = Paint().apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintVictim = Paint().apply {
        color = Color.parseColor("#FFC107")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintGrid = Paint().apply {
        color = Color.parseColor("#2A2A2A")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        currentX = (w / 2).toFloat()
        currentY = (h / 2).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // Draw grid
        val gridSize = 50f
        for (i in 0 until width step gridSize.toInt()) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paintGrid)
        }
        for (i in 0 until height step gridSize.toInt()) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paintGrid)
        }

        // Draw path
        if (path.size > 1) {
            for (i in 0 until path.size - 1) {
                val p1 = path[i]
                val p2 = path[i + 1]
                canvas.drawLine(
                    centerX + p1.x, centerY + p1.y,
                    centerX + p2.x, centerY + p2.y,
                    paintPath
                )
            }
        }

        // Draw obstacles
        for (obstacle in obstacles) {
            canvas.drawCircle(
                centerX + obstacle.x,
                centerY + obstacle.y,
                15f,
                paintObstacle
            )
        }

        // Draw victims
        for (victim in victims) {
            canvas.drawCircle(
                centerX + victim.x,
                centerY + victim.y,
                20f,
                paintVictim
            )
        }

        // Draw robot
        canvas.save()
        canvas.translate(centerX + currentX, centerY + currentY)
        canvas.rotate(currentAngle)
        
        val robotPath = Path().apply {
            moveTo(0f, -20f)
            lineTo(-15f, 15f)
            lineTo(15f, 15f)
            close()
        }
        canvas.drawPath(robotPath, paintRobot)
        
        canvas.restore()
    }

    fun moveForward() {
        val rad = Math.toRadians(currentAngle.toDouble())
        currentX += (20 * Math.sin(rad)).toFloat()
        currentY -= (20 * Math.cos(rad)).toFloat()
        path.add(Position(currentX, currentY))
        invalidate()
    }

    fun moveBackward() {
        val rad = Math.toRadians(currentAngle.toDouble())
        currentX -= (20 * Math.sin(rad)).toFloat()
        currentY += (20 * Math.cos(rad)).toFloat()
        path.add(Position(currentX, currentY))
        invalidate()
    }

    fun turnLeft() {
        currentAngle -= 15f
        invalidate()
    }

    fun turnRight() {
        currentAngle += 15f
        invalidate()
    }

    fun addObstacle() {
        val rad = Math.toRadians(currentAngle.toDouble())
        val obsX = currentX + (40 * Math.sin(rad)).toFloat()
        val obsY = currentY - (40 * Math.cos(rad)).toFloat()
        obstacles.add(Position(obsX, obsY))
        invalidate()
    }

    fun addVictim() {
        val rad = Math.toRadians(currentAngle.toDouble())
        val vicX = currentX + (40 * Math.sin(rad)).toFloat()
        val vicY = currentY - (40 * Math.cos(rad)).toFloat()
        victims.add(Position(vicX, vicY))
        invalidate()
    }

    fun updateRadar(rssi: Int) {
        radarStrength = rssi
        invalidate()
    }
}

/**
 * Kotlin Code Examples for Rescue Robot App
 * 
 * اتصل بنفس شبكة السيرفر (PFBJQQU)
 * Server: http://192.168.137.1:8000
 */

// ==========================================
//  1. Data Classes
// ==========================================

data class DevicePosition(
    val type: String,
    val x: Float,
    val y: Float,
    val priority: String?,
    val last_updated: String
)

data class RobotStatus(
    val mode: String,
    val robot_position: Position?,
    val victim_position: Position?,
    val distance_to_victim: Float?,
    val pending_command: String?
)

data class Position(
    val x: Float,
    val y: Float,
    val priority: String? = null
)

data class CommandRequest(
    val command: String
)

data class CommandResponse(
    val status: String,
    val command: String,
    val mode: String
)

// ==========================================
//  2. Retrofit API Interface
// ==========================================

import retrofit2.http.*
import retrofit2.Response

interface RescueApiService {
    
    // جلب الخريطة (مواقع الضحايا والروبوت)
    @GET("/get-map")
    suspend fun getMap(): Map<String, DevicePosition>
    
    // جلب حالة الروبوت
    @GET("/robot-status")
    suspend fun getRobotStatus(): RobotStatus
    
    // إرسال أمر للروبوت
    @POST("/robot-command")
    suspend fun sendCommand(@Body cmd: CommandRequest): CommandResponse
    
    // تغيير الوضع
    @POST("/set-mode/{mode}")
    suspend fun setMode(@Path("mode") mode: String): Response<Any>
}

// ==========================================
//  3. Retrofit Instance
// ==========================================

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.137.1:8000/"
    
    val api: RescueApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RescueApiService::class.java)
    }
}

// ==========================================
//  4. ViewModel for Map & Control
// ==========================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class RobotControlViewModel : ViewModel() {
    
    private val _robotStatus = MutableStateFlow<RobotStatus?>(null)
    val robotStatus: StateFlow<RobotStatus?> = _robotStatus
    
    private val _mapData = MutableStateFlow<Map<String, DevicePosition>>(emptyMap())
    val mapData: StateFlow<Map<String, DevicePosition>> = _mapData
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    init {
        startPolling()
    }
    
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    // جلب الخريطة
                    _mapData.value = RetrofitClient.api.getMap()
                    
                    // جلب حالة الروبوت
                    _robotStatus.value = RetrofitClient.api.getRobotStatus()
                    
                    _isConnected.value = true
                } catch (e: Exception) {
                    _isConnected.value = false
                }
                delay(1000) // كل ثانية
            }
        }
    }
    
    // إرسال أمر حركة
    fun sendCommand(command: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.sendCommand(CommandRequest(command))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // تغيير الوضع
    fun setMode(mode: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.setMode(mode)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // أوامر سريعة
    fun moveForward() = sendCommand("F")
    fun moveBackward() = sendCommand("B")
    fun turnLeft() = sendCommand("L")
    fun turnRight() = sendCommand("R")
    fun stop() = sendCommand("S")
    fun setAutoMode() = sendCommand("A")
    fun setManualMode() = sendCommand("M")
}

// ==========================================
//  5. Custom MapView (Canvas)
// ==========================================

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class MapCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private val victimPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    
    private val robotPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    
    private val gridPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
    }
    
    private var devices: Map<String, DevicePosition> = emptyMap()
    private val scale = 100f  // 1 متر = 100 بكسل
    private val offsetX = 50f
    private val offsetY = 50f
    
    fun updateDevices(data: Map<String, DevicePosition>) {
        devices = data
        invalidate() // إعادة الرسم
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // خلفية
        canvas.drawColor(Color.parseColor("#1a1a2e"))
        
        // رسم الشبكة
        drawGrid(canvas)
        
        // رسم الأجهزة
        devices.forEach { (name, pos) ->
            val screenX = pos.x * scale + offsetX
            val screenY = height - (pos.y * scale + offsetY)
            
            val paint = when (pos.type) {
                "VICTIM" -> victimPaint
                "ROBOT" -> robotPaint
                else -> Paint().apply { color = Color.BLUE }
            }
            
            // رسم الدائرة
            canvas.drawCircle(screenX, screenY, 25f, paint)
            
            // رسم الاسم
            canvas.drawText(name, screenX, screenY - 35, textPaint)
            
            // رسم الإحداثيات
            val posText = "(${pos.x}, ${pos.y})"
            canvas.drawText(posText, screenX, screenY + 50, textPaint.apply { textSize = 24f })
        }
    }
    
    private fun drawGrid(canvas: Canvas) {
        val gridSize = scale  // كل 1 متر
        
        // خطوط عمودية
        var x = offsetX
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }
        
        // خطوط أفقية
        var y = offsetY
        while (y < height) {
            canvas.drawLine(0f, height - y, width.toFloat(), height - y, gridPaint)
            y += gridSize
        }
    }
}

// ==========================================
//  6. XML Layout Example
// ==========================================

/*
<!-- activity_control.xml -->

<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#1a1a2e">

    <!-- الخريطة -->
    <com.yourpackage.MapCanvasView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- معلومات الحالة -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">
        
        <TextView
            android:id="@+id/tvMode"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:text="Mode: MANUAL"
            android:textColor="#00ff00"
            android:textSize="16sp" />
            
        <TextView
            android:id="@+id/tvDistance"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:text="Distance: --"
            android:textColor="#ffffff"
            android:textSize="16sp" />
    </LinearLayout>

    <!-- أزرار التحكم -->
    <GridLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:columnCount="3"
        android:rowCount="3"
        android:padding="16dp">
        
        <Button android:id="@+id/btnAuto" android:text="AUTO" />
        <Button android:id="@+id/btnForward" android:text="⬆" />
        <Button android:id="@+id/btnManual" android:text="MANUAL" />
        
        <Button android:id="@+id/btnLeft" android:text="⬅" />
        <Button android:id="@+id/btnStop" android:text="⏹" />
        <Button android:id="@+id/btnRight" android:text="➡" />
        
        <View android:layout_width="0dp" />
        <Button android:id="@+id/btnBackward" android:text="⬇" />
        <View android:layout_width="0dp" />
        
    </GridLayout>

</LinearLayout>
*/

// ==========================================
//  7. Activity Example
// ==========================================

/*
class ControlActivity : AppCompatActivity() {
    
    private lateinit var mapView: MapCanvasView
    private val viewModel: RobotControlViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        
        mapView = findViewById(R.id.mapView)
        
        // مراقبة الخريطة
        lifecycleScope.launch {
            viewModel.mapData.collect { data ->
                mapView.updateDevices(data)
            }
        }
        
        // مراقبة حالة الروبوت
        lifecycleScope.launch {
            viewModel.robotStatus.collect { status ->
                status?.let {
                    findViewById<TextView>(R.id.tvMode).text = "Mode: ${it.mode}"
                    findViewById<TextView>(R.id.tvDistance).text = 
                        "Distance: ${it.distance_to_victim ?: "--"}m"
                }
            }
        }
        
        // أزرار التحكم
        findViewById<Button>(R.id.btnForward).setOnClickListener { viewModel.moveForward() }
        findViewById<Button>(R.id.btnBackward).setOnClickListener { viewModel.moveBackward() }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { viewModel.turnLeft() }
        findViewById<Button>(R.id.btnRight).setOnClickListener { viewModel.turnRight() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { viewModel.stop() }
        findViewById<Button>(R.id.btnAuto).setOnClickListener { viewModel.setAutoMode() }
        findViewById<Button>(R.id.btnManual).setOnClickListener { viewModel.setManualMode() }
    }
}
*/

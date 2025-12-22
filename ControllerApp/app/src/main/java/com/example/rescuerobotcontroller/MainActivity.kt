package com.example.rescuerobotcontroller
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    // --- AYARLAR ---
    private val ROBOT_IP = "192.168.4.1" // 
    private val ROBOT_PORT = 8080

    // Değişkenler
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    private lateinit var statusText: TextView
    private lateinit var mapView: RobotMapView // Özel harita sınıfımız

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.txtStatus)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)
        val switchAuto = findViewById<Switch>(R.id.switchAuto)

        // Haritayı oluştur ve ekle
        mapView = RobotMapView(this)
        mapContainer.addView(mapView)

        // Butonları Tanımla
        setupTouchButton(findViewById(R.id.btnForward), "F")
        setupTouchButton(findViewById(R.id.btnLeft), "L")
        setupTouchButton(findViewById(R.id.btnRight), "R")
        setupTouchButton(findViewById(R.id.btnBackward), "B")

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            sendCommand("S") // Acil durdurma
        }

        // Otomatik Mod Switch
        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sendCommand("A") // Auto On
            else sendCommand("M") // Manual (Auto Off)
        }

        // Bağlantıyı Başlat
        connectToRobot()
    }

    // Butonlara basılı tutma mantığı (Basınca git, bırakınca dur)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchButton(btn: Button, command: String) {
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> sendCommand(command) // Basınca gönder
                MotionEvent.ACTION_UP -> sendCommand("S")   // Bırakınca dur
            }
            true
        }
    }

    // Socket Bağlantısı (Arka planda çalışır)
    private fun connectToRobot() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runOnUiThread { statusText.text = "Bağlanıyor..." }
                socket = Socket(ROBOT_IP, ROBOT_PORT)
                outputStream = socket?.getOutputStream()
                isConnected = true

                runOnUiThread {
                    statusText.text = "BAĞLANDI 🟢"
                    statusText.setTextColor(Color.GREEN)
                }

                // Veri Dinleme Döngüsü
                val reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                while (isConnected) {
                    val message = reader.readLine() ?: break
                    handleIncomingMessage(message)
                }

            } catch (e: Exception) {
                isConnected = false
                runOnUiThread {
                    statusText.text = "Bağlantı Hatası 🔴: ${e.message}"
                    statusText.setTextColor(Color.RED)
                }
            }
        }
    }

    // Gelen veriyi işle ve haritayı güncelle
    // Gelen veriyi işle ve haritayı güncelle
    private fun handleIncomingMessage(msg: String) {
        runOnUiThread {
            when {
                msg == "MOVE:F" -> mapView.moveForward()
                msg == "MOVE:B" -> mapView.moveBackward() // Geri eklendi
                msg == "MOVE:R" -> mapView.turnRight()
                msg == "MOVE:L" -> mapView.turnLeft()
                msg == "OBSTACLE" -> mapView.addObstacle()
                msg == "VICTIM" -> {
                    mapView.addVictim()
                    // Titreşim veya ses efekti eklenebilir
                    Toast.makeText(this, "🚨 KURTARMA: Vaka Bulundu!", Toast.LENGTH_LONG).show()
                }
                msg.startsWith("RADAR:") -> {
                    // Mesaj örneği: "RADAR:-65"
                    // ':' işaretinden sonrasını alıp sayıya çeviriyoruz
                    try {
                        val rssiStr = msg.split(":")[1]
                        val rssiVal = rssiStr.trim().toInt()
                        mapView.updateRadar(rssiVal)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Komut Gönderme Fonksiyonu
    private fun sendCommand(cmd: String) {
        if (!isConnected) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputStream?.write(cmd.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// --- HARİTA ÇİZİM SINIFI (Custom View) ---
// --- PROFESYONEL HARİTA SINIFI (BlueScoutMap) ---
class RobotMapView(context: Context) : View(context) {

    // 1. BOYA KALEMLERİ (Paints)
    private val paintTrail = Paint().apply {
        color = Color.parseColor("#00FF00") // Hacker Yeşili
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true // Pürüzsüz çizgi
    }

    private val paintWall = Paint().apply {
        color = Color.parseColor("#FF0000") // Alarm Kırmızısı
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }

    private val paintRobot = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        // Robotun üzerine gölge efekti
        setShadowLayer(10f, 0f, 0f, Color.BLUE)
    }

    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isFakeBoldText = true
    }

    private val paintRadarBg = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintRadarLevel = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    // 2. DEĞİŞKENLER
    private var posX = 500f // Başlangıç X
    private var posY = 800f // Başlangıç Y
    private var angle = 0.0 // Yön (Derece)
    private val step = 15f  // Adım mesafesi

    // Radar verisi (-100 ile -30 arası)
    private var currentRssi = -100

    // Harita Hafızası (Bitmap)
    private lateinit var mapBitmap: Bitmap
    private lateinit var mapCanvas: Canvas

    // Kurban İkonu (Resim dosyasından okuma)
    // Eğer resim yoksa kod hata vermesin diye null yapıyoruz, aşağıda daire çizeceğiz
    private var victimIcon: Bitmap? = null

    init {
        // İstersen buraya ikon yükleme kodu eklenebilir
        // victimIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_victim)
    }

    // Ekran oluştuğunda çalışır
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Ekran boyutunda boş bir kağıt (Bitmap) yarat
        mapBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mapCanvas = Canvas(mapBitmap)

        // Robotu ekranın tam ortasına koy
        posX = (w / 2).toFloat()
        posY = (h / 2).toFloat()

        // Arka planı hafif ızgaralı yapabiliriz (Opsiyonel)
        mapCanvas.drawColor(Color.BLACK)
    }

    // Ekrana Çizim Yapma (Saniyede 60 kare)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Hafızadaki haritayı (yollar, duvarlar) ekrana bas
        canvas.drawBitmap(mapBitmap, 0f, 0f, null)

        // 2. Robotun Kendisini Çiz (Üçgen şeklinde)
        drawRobotTriangle(canvas)

        // 3. HUD: Sağ Üst Köşeye Radar Çiz
        drawRadarHUD(canvas)
    }

    // --- ÖZEL ÇİZİM FONKSİYONLARI ---

    private fun drawRobotTriangle(canvas: Canvas) {
        val path = Path()
        // Matematiksel dönüşüm ile üçgeni döndür
        val r = 25f // Robot boyutu
        val rad = Math.toRadians(angle)

        // Robotun burnu
        val noseX = posX + (r * Math.sin(rad)).toFloat()
        val noseY = posY - (r * Math.cos(rad)).toFloat()

        // Sol arka
        val leftX = posX + (r * Math.sin(rad + 2.5)).toFloat()
        val leftY = posY - (r * Math.cos(rad + 2.5)).toFloat()

        // Sağ arka
        val rightX = posX + (r * Math.sin(rad - 2.5)).toFloat()
        val rightY = posY - (r * Math.cos(rad - 2.5)).toFloat()

        path.moveTo(noseX, noseY)
        path.lineTo(leftX, leftY)
        path.lineTo(rightX, rightY)
        path.close()

        canvas.drawPath(path, paintRobot)
    }

    private fun drawRadarHUD(canvas: Canvas) {
        val cx = width - 100f // Sağdan 100px içeride
        val cy = 100f         // Yukarıdan 100px aşağıda
        val radius = 60f

        // Radar Çerçevesi
        canvas.drawCircle(cx, cy, radius, paintRadarBg)
        canvas.drawText("RADAR", cx - 60, cy + radius + 40, paintText)

        // Sinyal Seviyesi (Doluluk)
        // RSSI -100 (boş) ile -30 (dolu) arasını 0-100'e çevir
        val signalPercent = ((currentRssi + 100) / 70.0).coerceIn(0.0, 1.0)

        // Renge karar ver (Kırmızı -> Sarı -> Yeşil)
        paintRadarLevel.color = when {
            signalPercent < 0.3 -> Color.RED
            signalPercent < 0.7 -> Color.YELLOW
            else -> Color.GREEN
        }

        // Dairenin içini sinyale göre doldur
        canvas.drawCircle(cx, cy, (radius * signalPercent).toFloat(), paintRadarLevel)

        // Metin olarak dBm yaz
        paintText.textSize = 30f
        canvas.drawText("$currentRssi dBm", cx - 60, cy, paintText.apply { color = Color.WHITE })
    }

    // --- DIŞARIDAN ÇAĞRILACAK HAREKETLER ---

    fun moveForward() {
        val oldX = posX
        val oldY = posY

        // Yeni konumu hesapla
        posX += (step * Math.sin(Math.toRadians(angle))).toFloat()
        posY -= (step * Math.cos(Math.toRadians(angle))).toFloat()

        // Bitmap üzerine kalıcı yeşil çizgi çek
        mapCanvas.drawLine(oldX, oldY, posX, posY, paintTrail)
        invalidate() // Ekranı yenile
    }

    // Geri gitme fonksiyonu (İstersen kırmızı çizebilirsin)
    fun moveBackward() {
        val oldX = posX
        val oldY = posY
        posX -= (step * Math.sin(Math.toRadians(angle))).toFloat()
        posY += (step * Math.cos(Math.toRadians(angle))).toFloat()
        // Geri giderken daha ince bir çizgi çizelim
        mapCanvas.drawLine(oldX, oldY, posX, posY, paintTrail.apply { strokeWidth = 3f })
        invalidate()
        paintTrail.strokeWidth = 6f // Eski haline getir
    }

    fun turnRight() { angle += 10.0; invalidate() }
    fun turnLeft()  { angle -= 10.0; invalidate() }

    fun addObstacle() {
        // Robotun 20px önüne kırmızı bir duvar çiz
        val wallDist = 30f
        val rad = Math.toRadians(angle)

        val wallCX = posX + (wallDist * Math.sin(rad)).toFloat()
        val wallCY = posY - (wallDist * Math.cos(rad)).toFloat()

        // Duvarın robotun açısına dik olması için +90 ve -90 derece
        val w1x = wallCX + (20 * Math.sin(rad + 1.57)).toFloat()
        val w1y = wallCY - (20 * Math.cos(rad + 1.57)).toFloat()

        val w2x = wallCX + (20 * Math.sin(rad - 1.57)).toFloat()
        val w2y = wallCY - (20 * Math.cos(rad - 1.57)).toFloat()

        mapCanvas.drawLine(w1x, w1y, w2x, w2y, paintWall)
        invalidate()
    }

    fun addVictim() {
        // Bulunduğu yere sarı bir işaret koy (Kurban)
        val paintVictim = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL }
        mapCanvas.drawCircle(posX, posY, 25f, paintVictim)

        // Üzerine 'V' yaz
        val paintV = Paint().apply { color = Color.BLACK; textSize = 35f; isFakeBoldText = true }
        mapCanvas.drawText("V", posX - 10, posY + 10, paintV)
        invalidate()
    }

    fun updateRadar(rssi: Int) {
        currentRssi = rssi
        invalidate() // Sadece radarı güncellemek için ekranı yenile
    }
}
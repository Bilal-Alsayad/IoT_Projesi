package com.example.rescuerobotcontroller.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.rescuerobotcontroller.R
import com.example.rescuerobotcontroller.RobotMapView
import com.example.rescuerobotcontroller.viewmodel.RobotControlViewModel
import kotlinx.coroutines.launch

/**
 * Control Fragment - Server-Based REST API Version
 * Artık TCP socket yok, HTTP REST API kullanıyor
 */
class ControlFragment : Fragment() {

    // ViewModel (Server iletişimi için)
    private val viewModel: RobotControlViewModel by activityViewModels()
    
    private lateinit var statusText: TextView
    private lateinit var mapView: RobotMapView
    private lateinit var switchAuto: Switch
    
    // RSSI gösterimi için
    private lateinit var tvRssiAvg: TextView
    private lateinit var tvRobotRssi: TextView
    private lateinit var tvVictimRssi: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create main layout
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Status Card
        val statusCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        
        statusText = TextView(requireContext()).apply {
            text = "🔄 Server'a Bağlanıyor..."
            textSize = 16f
            setTextColor(Color.parseColor("#00BCD4"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        statusCard.addView(statusText)
        mainLayout.addView(statusCard)

        // Map container
        val mapContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            elevation = 2f
        }
        mapView = RobotMapView(requireContext())
        mapContainer.addView(mapView)
        mainLayout.addView(mapContainer)

        // Auto switch card
        val switchCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 20, 32, 20)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        
        val switchLabel = TextView(requireContext()).apply {
            text = "🤖 Otomatik Mod"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        switchCard.addView(switchLabel)
        
        switchAuto = Switch(requireContext()).apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setAutoMode()  // ✅ Server'a HTTP POST
                } else {
                    viewModel.setManualMode()  // ✅ Server'a HTTP POST
                }
            }
        }
        switchCard.addView(switchAuto)
        mainLayout.addView(switchCard)

        // RSSI Card
        val rssiCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 20, 32, 20)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        
        tvRssiAvg = TextView(requireContext()).apply {
            text = "📡 RSSI: --"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFF00"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rssiCard.addView(tvRssiAvg)
        
        tvRobotRssi = TextView(requireContext()).apply {
            text = "🤖 Robot: --"
            textSize = 14f
            setTextColor(Color.parseColor("#00FF00"))
            setPadding(0, 8, 0, 0)
        }
        rssiCard.addView(tvRobotRssi)
        
        tvVictimRssi = TextView(requireContext()).apply {
            text = "🚫 Victim: --"
            textSize = 14f
            setTextColor(Color.parseColor("#FF0000"))
            setPadding(0, 4, 0, 0)
        }
        rssiCard.addView(tvVictimRssi)
        
        mainLayout.addView(rssiCard)

        // Control buttons
        val controlGrid = createControlButtons()
        mainLayout.addView(controlGrid)

        // ========== Observe ViewModel States ==========
        observeViewModel()

        return mainLayout
    }
    
    /**
     * ViewModel state'lerini dinle
     */
    private fun observeViewModel() {
        // Bağlantı durumu
        lifecycleScope.launch {
            viewModel.isConnected.collect { connected ->
                if (connected) {
                    statusText.text = "✅ SERVER BAĞLANDI"
                    statusText.setTextColor(Color.parseColor("#00E676"))
                } else {
                    statusText.text = "❌ SERVER BAĞLANTISI YOK"
                    statusText.setTextColor(Color.parseColor("#FF5252"))
                }
            }
        }
        
        // Robot durumu
        lifecycleScope.launch {
            viewModel.robotStatus.collect { status ->
                status?.let {
                    // Mod güncelle
                    switchAuto.isChecked = (it.mode == "AUTO")
                    
                    // RSSI verisini güncelle
                    it.rssi?.let { rssi ->
                        // Ortalama RSSI
                        tvRssiAvg.text = "📡 RSSI: ${rssi.average?.let { "%.1f dBm".format(it) } ?: "--"}"
                        
                        // Robot RSSI (her anchor için)
                        val robotRssi = rssi.robot?.entries?.joinToString(", ") { 
                            "${it.key}: ${it.value}"
                        } ?: "--"
                        tvRobotRssi.text = "🤖 Robot: $robotRssi"
                        
                        // Victim RSSI (her anchor için)
                        val victimRssi = rssi.victim?.entries?.joinToString(", ") { 
                            "${it.key}: ${it.value}"
                        } ?: "--"
                        tvVictimRssi.text = "🚫 Victim: $victimRssi"
                    } ?: run {
                        // RSSI verisi yoksa
                        tvRssiAvg.text = "📡 RSSI: --"
                        tvRobotRssi.text = "🤖 Robot: --"
                        tvVictimRssi.text = "🚫 Victim: --"
                    }
                    
                    // Harita pozisyonu güncelle (eğer varsa)
                    it.robot_position?.let { pos ->
                        // RobotMapView'de pozisyon güncelleme methodunu çağırabilirsin
                    }
                }
            }
        }
        
        // Harita verileri
        lifecycleScope.launch {
            viewModel.mapData.collect { devices ->
                // Server'dan gelen harita verilerini işle
                devices.forEach { (name, device) ->
                    when (device.type) {
                        "ROBOT" -> {
                            // Robot pozisyonunu güncelle
                        }
                        "VICTIM" -> {
                            mapView.addVictim()
                        }
                        "OBSTACLE" -> {
                            mapView.addObstacle()
                        }
                    }
                }
            }
        }
        
        // Hata mesajları
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createControlButtons(): GridLayout {
        return GridLayout(requireContext()).apply {
            rowCount = 3
            columnCount = 3
            setPadding(24, 24, 24, 24)
            
            // Empty space
            addView(View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            })

            // Forward (şu an geri gidiyor → R yap)
            addView(createButton("▲", "F", false))
            
            addView(View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            })

            // Left (şu an sağa gidiyor → F yap)
            addView(createButton("◄", "L", false))
            
            // Stop
            addView(createButton("■", "S", true))

            // Right (şu an sola gidiyor → B yap)
            addView(createButton("►", "R", false))
            
            // Empty
            addView(View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            })

            // Backward (şu an ileri gidiyor → L yap)
            addView(createButton("▼", "B", false))
            
            // Empty
            addView(View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            })
        }
    }

    private fun createButton(label: String, command: String, isStopButton: Boolean): Button {
        return Button(requireContext()).apply {
            text = label
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 200
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            
            // Use gradient backgrounds
            setBackgroundResource(
                if (isStopButton) R.drawable.button_stop_gradient
                else R.drawable.button_control_gradient
            )
            
            // Elevation effect
            elevation = 8f
            stateListAnimator = null
            
            // ✅ ViewModel ile komut gönder (HTTP POST)
            setOnClickListener {
                viewModel.sendCommand(command)
            }
        }
    }
}

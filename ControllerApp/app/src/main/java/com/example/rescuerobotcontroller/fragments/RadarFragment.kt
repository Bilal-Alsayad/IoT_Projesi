package com.example.rescuerobotcontroller.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rescuerobotcontroller.RadarView

class RadarFragment : Fragment() {

    private lateinit var radarView: RadarView
    private lateinit var deviceCountText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(16, 16, 16, 16)
        }

        // Title
        val title = TextView(requireContext()).apply {
            text = "📡 BLE Radar"
            textSize = 24f
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(title)

        // Radar view
        radarView = RadarView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        mainLayout.addView(radarView)

        // Device count
        deviceCountText = TextView(requireContext()).apply {
            text = "Cihaz Sayısı: 0"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        mainLayout.addView(deviceCountText)

        // Simulated data for testing
        simulateRadarData()

        return mainLayout
    }

    private fun simulateRadarData() {
        // Test data - gerçek Firebase verisi gelince kaldırılacak
        radarView.addDevice("AA:BB:CC:DD:EE:01", -55, false)
        radarView.addDevice("AA:BB:CC:DD:EE:02", -70, false)
        radarView.addDevice("AA:BB:CC:DD:EE:03", -45, true) // Victim
    }
}

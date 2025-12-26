package com.example.rescuerobotcontroller.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    data class TelemetryLog(
        val direction: String,
        val command: String,
        val timestamp: Long
    )

    private lateinit var historyContainer: LinearLayout
    private val logs = mutableListOf<TelemetryLog>()

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
            text = "📜 Robot Geçmişi"
            textSize = 24f
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(title)

        // Scrollable history list
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        historyContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(historyContainer)
        mainLayout.addView(scrollView)

        // Load sample data
        loadSampleHistory()

        return mainLayout
    }

    private fun loadSampleHistory() {
        // Test data - gerçek Firebase verisi gelince kaldırılacak
        val now = System.currentTimeMillis()
        
        addLogEntry("FORWARD", "MOVE_FORWARD", now - 60000)
        addLogEntry("LEFT", "TURN_LEFT", now - 50000)
        addLogEntry("FORWARD", "MOVE_FORWARD", now - 40000)
        addLogEntry("STOP", "STOP", now - 30000)
        addLogEntry("RIGHT", "TURN_RIGHT", now - 20000)
        addLogEntry("BACKWARD", "MOVE_BACKWARD", now - 10000)
        addLogEntry("STOP", "EMERGENCY_STOP", now - 5000)
    }

    private fun addLogEntry(direction: String, command: String, timestamp: Long) {
        logs.add(TelemetryLog(direction, command, timestamp))

        val logView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }

        // Emoji icon
        val emoji = TextView(requireContext()).apply {
            text = getDirectionEmoji(direction)
            textSize = 28f
            setPadding(0, 0, 16, 0)
        }
        logView.addView(emoji)

        // Log info
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val directionText = TextView(requireContext()).apply {
            text = direction
            textSize = 18f
            setTextColor(Color.parseColor("#00E676"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        infoLayout.addView(directionText)

        val commandText = TextView(requireContext()).apply {
            text = command
            textSize = 14f
            setTextColor(Color.parseColor("#B3B3B3"))
        }
        infoLayout.addView(commandText)

        logView.addView(infoLayout)

        // Timestamp
        val timeText = TextView(requireContext()).apply {
            text = formatTimestamp(timestamp)
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.END
        }
        logView.addView(timeText)

        historyContainer.addView(logView, 0) // Add at top
    }

    private fun getDirectionEmoji(direction: String): String {
        return when (direction) {
            "FORWARD" -> "⬆️"
            "BACKWARD" -> "⬇️"
            "LEFT" -> "⬅️"
            "RIGHT" -> "➡️"
            "STOP" -> "⏹️"
            else -> "📍"
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

package com.selim.victimapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VictimAppScreen()
        }
    }
}

@Composable
fun VictimAppScreen() {
    val context = LocalContext.current

    val currentRssi = AppState.rssi
    val isPriorityClicked = AppState.isPriorityClicked

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            val intent = Intent(context, BleForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        permissionLauncher.launch(perms)
    }

    val isFar = currentRssi < -98
    val isApproaching = currentRssi >= -98 && currentRssi < -80
    val isArrived = currentRssi >= -80

    val targetColor = when {
        isArrived -> Color(0xFF4CAF50)
        isApproaching -> Color(0xFFFFC107)
        else -> Color(0xFFB71C1C)
    }
    val backgroundColor by animateColorAsState(targetColor, label = "bgColor")

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val statusText = when {
                isArrived -> "YARDIM GELDİ"
                isApproaching -> "YARDIM GELİYOR!\nBEKLEYİN!"
                else -> "KURTARILMAYI\nBEKLENİYOR"
            }

            Text(
                text = statusText,
                fontSize = if (isArrived) 40.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isApproaching) Color.Black else Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )

            Text(
                text = "Sinyal: $currentRssi dBm",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            if (isApproaching) {
                Button(
                    onClick = {
                        if (!AppState.isPriorityClicked) {
                            val intent = Intent(context, BleForegroundService::class.java)
                            intent.action = "ACTIVATE_PRIORITY"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPriorityClicked) Color.DarkGray else Color(0xFFFF5722),
                        disabledContainerColor = Color.DarkGray
                    ),
                    enabled = !isPriorityClicked,
                    modifier = Modifier.size(220.dp).padding(10.dp),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isPriorityClicked) "AKTİVLENDİ" else "ÖNCELİK\nAKTİVLE",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
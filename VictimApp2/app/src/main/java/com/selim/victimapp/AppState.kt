package com.selim.victimapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppState {
    // Sinyal Gücü (Varsayılan -150)
    var rssi by mutableIntStateOf(-150)

    // Öncelik Butonuna Basıldı mı?
    var isPriorityClicked by mutableStateOf(false)
}
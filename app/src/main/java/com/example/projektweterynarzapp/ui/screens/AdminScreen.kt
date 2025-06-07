package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Prosty placeholder dla panelu lekarza/administratora.
 * Tutaj możesz dodać przyciski do zarządzania terminami, statystykami itd.
 */
@Composable
fun AdminScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Panel lekarza / administratora",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: przejdź do listy wizyt */ }) {
            Text("Zarządzaj wizytami")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: statystyki, zarządzanie użytkownikami */ }) {
            Text("Statystyki kliniki")
        }
    }
}

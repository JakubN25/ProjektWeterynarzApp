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
 * Panel administracyjny dostępny tylko dla roli = admin lub doctor
 * Dodano możliwość przejścia do ekranu zarządzania grafikami lekarzy.
 */
@Composable
fun AdminScreen(
    onManageSchedules: () -> Unit,
    onManageOffers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Panel administracyjny",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onManageSchedules) {
            Text("Zarządzaj grafikami lekarzy")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onManageOffers) {
            Text("Zarządzaj ofertą wizyt") }
    }
}


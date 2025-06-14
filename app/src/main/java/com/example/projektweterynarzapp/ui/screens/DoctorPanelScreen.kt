package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun DoctorPanelScreen(
    onMyAppointments: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Panel Doktora",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onMyAppointments, modifier = Modifier.fillMaxWidth()) {
            Text("Moje wizyty")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Historia wizyt")
        }
    }
}


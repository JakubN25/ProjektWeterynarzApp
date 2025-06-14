package com.example.projektweterynarzapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen() {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun reload() {
        isLoading = true
        // uruchom jako coroutine
        CoroutineScope(Dispatchers.Main).launch {
            bookings = authRepo.getBookings().sortedBy { "${it.date} ${it.hour}" }
            isLoading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(Unit) {
        isLoading = true
        bookings = authRepo.getBookings()
            .sortedBy { "${it.date} ${it.hour}" } // yyyy-MM-dd HH:mm – sortowanie rosnąco
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Moje wizyty") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (bookings.isEmpty()) {
                Text("Brak wizyt", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(bookings) { booking ->
                        BookingCard(booking)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zwierzak: ${booking.petName} (${booking.petSpecies})", style = MaterialTheme.typography.titleMedium)
            Text("Rodzaj: ${booking.visitType}")
            Text("Data: ${booking.date} ${booking.hour}")
            Text("Lokalizacja: ${booking.location}")
            Text("Lekarz: ${booking.doctorName}")
        }
    }
}

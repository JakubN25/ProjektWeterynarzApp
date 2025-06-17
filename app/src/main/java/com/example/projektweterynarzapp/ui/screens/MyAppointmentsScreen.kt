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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookingToDelete by remember { mutableStateOf<Booking?>(null) }
    var cancelInfo by remember { mutableStateOf<String?>(null) }

    fun reload() {
        isLoading = true
        CoroutineScope(Dispatchers.Main).launch {
            bookings = authRepo.getBookings().sortedBy { "${it.date} ${it.hour}" }
            isLoading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    // Dodaj pobieranie aktualnej daty i godziny
    fun getCurrentDateTimeString(): String {
        val now = java.time.LocalDateTime.now()
        return "${now.toLocalDate()} ${now.toLocalTime().toString().substring(0,5)}"
    }

    val currentDateTime = getCurrentDateTimeString()

    // Podziel wizyty na nadchodzące i przeszłe
    val upcomingBookings = bookings.filter {
        // Daty i godziny muszą być porównywane jako Stringi: "YYYY-MM-DD HH:mm"
        val bookingDateTime = "${it.date} ${it.hour}"
        bookingDateTime >= currentDateTime
    }
    val pastBookings = bookings.filter {
        val bookingDateTime = "${it.date} ${it.hour}"
        bookingDateTime < currentDateTime
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
                if (cancelInfo != null) {
                    Text(cancelInfo!!, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn {
                    if (upcomingBookings.isNotEmpty()) {
                        item {
                            Text(
                                "Nadchodzące wizyty",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(upcomingBookings) { booking ->
                            BookingCard(
                                booking = booking,
                                onCancel = {
                                    bookingToDelete = booking
                                    showDeleteDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        item {
                            Text("Brak nadchodzących wizyt.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (pastBookings.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Przeszłe wizyty",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(pastBookings) { booking ->
                            BookingCard(booking = booking)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

        }

        // Dialog potwierdzający anulowanie wizyty
        if (showDeleteDialog && bookingToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Anulowanie wizyty") },
                text = { Text("Czy na pewno chcesz anulować tę wizytę? Tej operacji nie można cofnąć.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            coroutineScope.launch {
                                val booking = bookingToDelete!!
                                if (booking.id.isNotBlank()) {
                                    authRepo.deleteUserBooking(booking.userId, booking.id)
                                    val doctorBookings = authRepo.getDoctorBookings(booking.doctorId)
                                    val doctorBookingId = doctorBookings.find {
                                        it.userId == booking.userId && it.date == booking.date && it.hour == booking.hour && it.petId == booking.petId
                                    }?.id
                                    if (!doctorBookingId.isNullOrBlank()) {
                                        authRepo.deleteDoctorBooking(booking.doctorId, doctorBookingId)
                                    }
                                    cancelInfo = "Wizyta została anulowana."
                                    reload()
                                }
                            }
                        }
                    ) { Text("Tak, anuluj") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text("Nie")
                    }
                }
            )
        }
    }
}

@Composable
fun BookingCard(booking: Booking, onCancel: (() -> Unit)? = null) {
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
            Spacer(modifier = Modifier.height(8.dp))
            // Przycisk anulowania tylko dla nadchodzących wizyt
            onCancel?.let {
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(38.dp)) {
                    Text("Anuluj wizytę")
                }
            }
        }
    }
}



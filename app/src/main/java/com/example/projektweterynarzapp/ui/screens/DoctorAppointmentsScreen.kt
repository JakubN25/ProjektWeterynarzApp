import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.Booking
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentsScreen(
    doctorId: String,
    onBack: () -> Unit
) {
    val repo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var userEmailMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookingToDelete by remember { mutableStateOf<Booking?>(null) }

    // Pobierz mapę userId->email i wizyty doktora
    LaunchedEffect(doctorId) {
        isLoading = true
        val users = repo.getAllUsers() // user: User(uid, email, ...)
        userEmailMap = users.associate { it.uid to it.email }
        val allBookings = repo.getDoctorBookings(doctorId)
        bookings = allBookings.sortedBy { "${it.date} ${it.hour}" }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje wizyty") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (bookings.isEmpty()) {
                Text("Brak wizyt", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(bookings) { booking ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                // DATA
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Data: ", style = MaterialTheme.typography.bodyMedium)
                                    Text(booking.date, style = MaterialTheme.typography.titleMedium)
                                }
                                // GODZINA
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Godzina: ", style = MaterialTheme.typography.bodyMedium)
                                    Text(booking.hour, style = MaterialTheme.typography.titleMedium)
                                }
                                // EMAIL
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Email właściciela: ", style = MaterialTheme.typography.bodyMedium)
                                    Text(userEmailMap[booking.userId] ?: booking.userId, style = MaterialTheme.typography.titleMedium)
                                }
                                // ZWIERZAK
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Zwierzak: ", style = MaterialTheme.typography.bodyMedium)
                                    Text("${booking.petName} (${booking.petSpecies})", style = MaterialTheme.typography.titleMedium)
                                }
                                // RODZAJ WIZYTY
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rodzaj wizyty: ", style = MaterialTheme.typography.bodyMedium)
                                    Text(booking.visitType, style = MaterialTheme.typography.titleMedium)
                                }

                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        bookingToDelete = booking
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Anuluj wizytę")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog potwierdzający usuwanie
        if (showDeleteDialog && bookingToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Potwierdź anulowanie wizyty") },
                text = { Text("Czy na pewno chcesz anulować tę wizytę? Tej operacji nie można cofnąć.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val booking = bookingToDelete!!
                                // Szukamy id wizyty w bookings użytkownika
                                val userBookings = repo.getPatientBookings(booking.userId)
                                val userBookingId = userBookings.find {
                                    it.doctorId == doctorId && it.hour == booking.hour && it.date == booking.date && it.petId == booking.petId
                                }?.id
                                // Usuwamy wizytę u doktora i u użytkownika
                                if (booking.id.isNotBlank()) {
                                    repo.deleteDoctorBooking(doctorId, booking.id)
                                }
                                if (!userBookingId.isNullOrBlank()) {
                                    repo.deleteUserBooking(booking.userId, userBookingId)
                                }
                                // Odśwież
                                bookings = repo.getDoctorBookings(doctorId).sortedBy { "${it.date} ${it.hour}" }
                                showDeleteDialog = false
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




import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.User
import com.example.projektweterynarzapp.data.models.Booking
import com.example.projektweterynarzapp.data.models.Pet
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorVisitHistoryScreen(
    onBack: () -> Unit
) {
    val repo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Pobieranie listy użytkowników
    LaunchedEffect(Unit) {
        loading = true
        users = repo.getAllUsers()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedUser == null) "Pacjenci" else "Historia wizyt") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedUser != null) {
                            selectedUser = null
                            bookings = emptyList()
                        } else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (selectedUser == null) {
            // LISTA UŻYTKOWNIKÓW + PRZYCISK ODTWÓRZ HISTORIĘ
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text("Lista użytkowników", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                }
                items(users) { user ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(user.email)
                            Text(
                                "${user.firstName} ${user.lastName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                loading = true
                                selectedUser = user
                                coroutineScope.launch {
                                    bookings = repo.getPatientBookings(user.uid)
                                    pets = repo.getUserPets(user.uid)
                                    loading = false
                                }
                            }
                        ) {
                            Text("Pokaż historię wizyt")
                        }
                    }
                }
            }
        } else {
            // HISTORIA WIZYT DLA UŻYTKOWNIKA
            val today = LocalDate.now()
            val (future, past) = bookings.partition {
                try {
                    LocalDate.parse(it.date).isAfter(today) || LocalDate.parse(it.date)
                        .isEqual(today)
                } catch (_: Exception) {
                    false
                }
            }
            val petMap = pets.associateBy { it.id }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. KARTA DANYCH KONTAKTOWYCH
                item {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Dane kontaktowe pacjenta", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("Imię i nazwisko: ${selectedUser?.firstName} ${selectedUser?.lastName}")
                            Text("E-mail: ${selectedUser?.email}")
                            Text("Telefon: ${selectedUser?.phone}")
                            Text("Adres: ${selectedUser?.address}, ${selectedUser?.city}")
                        }
                    }
                }
                items(pets) { pet ->
                    ExpandablePetCard(pet = pet)
                }

                // 2. HISTORIA WIZYT
                item {
                    Text("Historia wizyt: ${selectedUser?.email}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Text("Przyszłe wizyty", fontWeight = FontWeight.Bold)
                }
                if (future.isEmpty()) {
                    item {
                        Text(
                            "Brak przyszłych wizyt",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    items(future.sortedBy { it.date + it.hour }) { booking ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Zwierzak: ${booking.petName} (${booking.petSpecies})")
                                Text("Data: ${booking.date} ${booking.hour}")
                                Text("Typ wizyty: ${booking.visitType}")
                                Text("Lekarz: ${booking.doctorName}")
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Przeszłe wizyty", fontWeight = FontWeight.Bold)
                }
                if (past.isEmpty()) {
                    item {
                        Text(
                            "Brak przeszłych wizyt",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    items(past.sortedByDescending { it.date + it.hour }) { booking ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Zwierzak: ${booking.petName} (${booking.petSpecies})")
                                Text("Data: ${booking.date} ${booking.hour}")
                                Text("Typ wizyty: ${booking.visitType}")
                                Text("Lekarz: ${booking.doctorName}")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ExpandablePetCard(pet: Pet) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Zwierzak: ${pet.name} (${pet.species})", style = MaterialTheme.typography.titleMedium)
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text("• Rasa: ${pet.breed}")
                Text("• Wiek: ${pet.age}")
                Text("• Waga: ${pet.weight}")
                Text("• Płeć: ${pet.gender}")
            }
        }
    }
}



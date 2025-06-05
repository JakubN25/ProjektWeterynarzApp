// ui/screens/BookingDetailsScreen.kt
package com.example.projektweterynarzapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.Pet
import com.example.projektweterynarzapp.ui.navigation.Screen
import kotlinx.coroutines.launch

/**
 * Ekran: „Uzupełnij szczegóły wizyty”.
 * Oczekuje w trasie trzech argumentów:
 *   - location (String),
 *   - date (String, format yyyy-MM-dd),
 *   - hour (String, np. "10:40").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    location: String,
    date: String,   // oczekujemy formatu yyyy-MM-dd
    hour: String,   // np. "08:00"
    navController: NavHostController
) {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Lista zwierzaków z Firestore
    var petList by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoadingPets by remember { mutableStateOf(true) }

    // Pobrane raz
    LaunchedEffect(Unit) {
        isLoadingPets = true
        petList = authRepo.getPets()
        isLoadingPets = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Szczegóły wizyty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$location | $date | $hour",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BookingDetailsForm(
                petList = petList,
                isLoadingPets = isLoadingPets,
                onConfirm = { chosenPet, visitType, chosenDoctor ->
                    // TODO: tutaj zapisz wizytę do bazy danych:
                    // - lokal: location
                    // - data: date
                    // - godzina: hour
                    // - pet: chosenPet
                    // - rodzaj: visitType
                    // - lekarz: chosenDoctor
                    Toast.makeText(
                        context,
                        "Potwierdzono wizytę:\nZwierzak: ${chosenPet?.name}\n" +
                                "Rodzaj: $visitType\nLekarz: $chosenDoctor\n" +
                                "Data: $date $hour\nLokal: $location",
                        Toast.LENGTH_LONG
                    ).show()
                    // Po potwierdzeniu możemy np. wyczyścić stack i wrócić do Home:
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun BookingDetailsForm(
    petList: List<Pet>,
    isLoadingPets: Boolean,
    onConfirm: (selectedPet: Pet?, visitType: String, doctor: String) -> Unit
) {
    // Stany dla poszczególnych dropdownów:
    var expandedPet by remember { mutableStateOf(false) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }

    var expandedVisitType by remember { mutableStateOf(false) }
    val visitTypes = listOf("Kontrola (20 min)", "Szczepienie (30 min)", "Zabieg (120 min)")
    var selectedVisitType by remember { mutableStateOf<String?>(null) }

    var expandedDoctor by remember { mutableStateOf(false) }
    // Na razie tylko placeholder
    val doctorList = listOf("Brak dostępnych lekarzy")
    var selectedDoctor by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Uzupełnij szczegóły wizyty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ---- Wybierz zwierzaka ----
            Text(text = "Wybierz zwierzaka", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedTextField(
                    value = selectedPet?.let { "${it.name} (${it.species})" } ?: "",
                    onValueChange = { /* brak edycji */ },
                    placeholder = {
                        if (isLoadingPets) {
                            Text("Ładowanie…")
                        } else {
                            Text(if (petList.isEmpty()) "Brak dodanych zwierząt" else "Wybierz zwierzaka")
                        }
                    },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedPet) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable {
                                if (petList.isNotEmpty()) expandedPet = !expandedPet
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (petList.isNotEmpty()) expandedPet = true
                        }
                )
                DropdownMenu(
                    expanded = expandedPet,
                    onDismissRequest = { expandedPet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    petList.forEach { pet ->
                        DropdownMenuItem(
                            text = { Text(text = "${pet.name} (${pet.species})") },
                            onClick = {
                                selectedPet = pet
                                expandedPet = false
                            }
                        )
                    }
                }
            }
            if (!isLoadingPets && petList.isEmpty()) {
                Text(
                    text = "Jeśli nie masz jeszcze dodanego zwierzaka, dodaj go w Moje Konto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Rodzaj wizyty ----
            Text(text = "Rodzaj wizyty", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedTextField(
                    value = selectedVisitType ?: "",
                    onValueChange = { },
                    placeholder = { Text("Wybierz rodzaj wizyty") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedVisitType) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable {
                                expandedVisitType = !expandedVisitType
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedVisitType = true }
                )
                DropdownMenu(
                    expanded = expandedVisitType,
                    onDismissRequest = { expandedVisitType = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    visitTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(text = type) },
                            onClick = {
                                selectedVisitType = type
                                expandedVisitType = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Wybierz lekarza ----
            Text(text = "Wybierz lekarza", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedTextField(
                    value = selectedDoctor ?: "",
                    onValueChange = { },
                    placeholder = { Text("Wybierz lekarza") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedDoctor) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable {
                                expandedDoctor = !expandedDoctor
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDoctor = true }
                )
                DropdownMenu(
                    expanded = expandedDoctor,
                    onDismissRequest = { expandedDoctor = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    doctorList.forEach { doc ->
                        DropdownMenuItem(
                            text = { Text(text = doc) },
                            onClick = {
                                selectedDoctor = doc
                                expandedDoctor = false
                            }
                        )
                    }
                }
            }
            Text(
                text = "Lista lekarzy pokazuje tylko tych, którzy zajmują się wybranym gatunkiem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Przycisk „Potwierdź wizytę” ----
            Button(
                onClick = {
                    onConfirm(selectedPet, selectedVisitType ?: "", selectedDoctor ?: "")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Potwierdź wizytę")
            }
        }
    }
}


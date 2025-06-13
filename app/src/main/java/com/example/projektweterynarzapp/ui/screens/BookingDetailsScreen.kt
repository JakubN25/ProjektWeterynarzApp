package com.example.projektweterynarzapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.projektweterynarzapp.data.models.Booking
import com.example.projektweterynarzapp.data.models.Pet
import com.example.projektweterynarzapp.data.models.User
import com.example.projektweterynarzapp.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.projektweterynarzapp.data.models.DoctorSchedule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.time.LocalDate


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
    date: String,
    hour: String,
    navController: NavHostController
) {
    val authRepo = remember { AuthRepository() }
    val scheduleRepo = remember { AuthRepository.ScheduleRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- Formularz state lifted ---
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedVisitType by remember { mutableStateOf<String?>(null) }
    var selectedDoctor by remember { mutableStateOf<User?>(null) }

    val visitTypes = listOf(
        "Kontrola (20 min)",
        "Szczepienie (40 min)",
        "Zabieg (120 min)"
    )
    // Mapa typ->czas trwania
    val visitTypeDurations = mapOf(
        "Kontrola (20 min)" to 20,
        "Szczepienie (40 min)" to 40,
        "Zabieg (120 min)" to 120
    )

    // --- Ładowanie danych ---
    var petList by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoadingPets by remember { mutableStateOf(true) }
    var doctorList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingDoctors by remember { mutableStateOf(true) }
    var doctorSchedules by remember { mutableStateOf<List<DoctorSchedule>>(emptyList()) }
    var isLoadingSchedules by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // pets
        isLoadingPets = true
        petList = authRepo.getPets()
        isLoadingPets = false

        // doctors
        isLoadingDoctors = true
        doctorList = authRepo.getDoctors()
        isLoadingDoctors = false

        // schedules
        isLoadingSchedules = true
        doctorSchedules = scheduleRepo.getAllDoctorsSchedules()
        isLoadingSchedules = false
    }

    // --- Generowanie slotów 20-minutowych ---
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeSlots by remember {
        mutableStateOf(
            generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(20) }
                .takeWhile { it <= LocalTime.of(20, 0) }
                .map { it.format(timeFormatter) }
                .toList()
        )
    }

    // --- Obliczanie długości wizyty i desiredSlots ---
    val visitDuration = remember(selectedVisitType) {
        visitTypeDurations[selectedVisitType] ?: 20
    }
    val desiredSlots = remember(hour, visitDuration) {
        val startIdx = timeSlots.indexOf(hour).takeIf { it >= 0 } ?: return@remember emptyList<String>()
        (0 until (visitDuration / 20)).mapNotNull { i ->
            timeSlots.getOrNull(startIdx + i)
        }
    }

    // --- Budowanie mapy doctorId -> zajęte sloty ---
    var occupiedSlotsByDoctor by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    LaunchedEffect(doctorList, date) {
        val db = FirebaseFirestore.getInstance()
        val map = mutableMapOf<String, MutableSet<String>>()
        doctorList.forEach { doc ->
            val taken = mutableSetOf<String>()
            try {
                val snap = db.collection("users")
                    .document(doc.uid)
                    .collection("bookings")
                    .whereEqualTo("date", date)
                    .get()
                    .await()
                snap.documents.mapNotNull { it.toObject(Booking::class.java) }
                    .forEach { b ->
                        val idx = timeSlots.indexOf(b.hour).takeIf { it >= 0 } ?: return@forEach
                        val span = (b.duration / 20) - 1
                        for (i in 0..span.coerceAtLeast(0)) {
                            timeSlots.getOrNull(idx + i)?.let(taken::add)
                        }
                    }
            } catch (e: Exception) {
                Log.e("BookingDetails", "fetch occupied: ${e.localizedMessage}")
            }
            map[doc.uid] = taken
        }
        occupiedSlotsByDoctor = map
    }

    // --- Filtrowanie dostępnych lekarzy ---
    val dayName = LocalDate.parse(date, DateTimeFormatter.ISO_DATE).dayOfWeek.name
    val schedulesMap = remember(doctorSchedules) {
        doctorSchedules.associateBy { it.doctorId }
    }
    val availableDoctors = remember(doctorList, schedulesMap, occupiedSlotsByDoctor, desiredSlots) {
        doctorList.filter { doc ->
            val tr = schedulesMap[doc.uid]?.schedules?.get(dayName) ?: return@filter false
            val start = LocalTime.parse(tr.start, timeFormatter)
            val end = LocalTime.parse(tr.end, timeFormatter)
            // 1) wszystkie desiredSlots mieszczą się w grafiku
            val fitsSchedule = desiredSlots.all { slot ->
                val t = LocalTime.parse(slot, timeFormatter)
                !t.isBefore(start) && t.plusMinutes(20).minusMinutes(1).isBefore(end)
            }
            // 2) żaden z desiredSlots nie jest zajęty
            val free = desiredSlots.none { occupiedSlotsByDoctor[doc.uid]?.contains(it) == true }
            fitsSchedule && free
        }
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
                },
                navigationIcon = {
                    // ← Strzałka „wstecz” – wracamy do wyboru terminu
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
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
                doctorList        = availableDoctors,
                isLoadingDoctors = isLoadingDoctors,
                // w pliku BookingDetailsScreen.kt, wewnątrz onConfirm:

                onConfirm = { chosenPet, visitType, chosenDoctor ->
                    // 1) Najpierw potwierdzenie dla użytkownika
                    Toast.makeText(
                        context,
                        "Potwierdzono wizytę:\nZwierzak: ${chosenPet?.name}\n" +
                                "Rodzaj: $visitType\nLekarz: $chosenDoctor\n" +
                                "Data: $date $hour\nLokal: $location",
                        Toast.LENGTH_LONG
                    ).show()

                    // 2) Nawigacja z powrotem do Home
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }

                    // 3) I równolegle – zapis w bazie
                    val docId   = chosenDoctor?.uid ?: ""
                    val docName = "${chosenDoctor?.firstName} ${chosenDoctor?.lastName}".trim()
                    coroutineScope.launch {
                        val ok = authRepo.addBooking(
                            location   = location,
                            date       = date,
                            hour       = hour,
                            petId      = chosenPet?.id ?: "",
                            petName    = chosenPet?.name ?: "",
                            petSpecies = chosenPet?.species ?: "",
                            visitType  = visitType,
                            doctorId   = docId,
                            doctorName = docName
                        )
                        Log.d("BookingDetails", "addBooking returned: $ok")
                        withContext(Dispatchers.Main) {
                            if (!ok) {
                                Toast.makeText(context, "Nie udało się zapisać wizyty", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsForm(
    petList: List<Pet>,
    isLoadingPets: Boolean,
    doctorList: List<User>,
    isLoadingDoctors: Boolean,
    onConfirm: (selectedPet: Pet?, visitType: String, doctor: User?) -> Unit
) {
    var expandedPet by remember { mutableStateOf(false) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }

    var expandedVisitType by remember { mutableStateOf(false) }
    val visitTypes = listOf("Kontrola (20 min)", "Szczepienie (40 min)", "Zabieg (120 min)")
    var selectedVisitType by remember { mutableStateOf<String?>(null) }

    var expandedDoctor by remember { mutableStateOf(false) }
    var selectedDoctor by remember { mutableStateOf<User?>(null) }

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
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPet?.let { "${it.name} (${it.species})" } ?: "",
                    onValueChange = {},
                    placeholder = {
                        when {
                            isLoadingPets       -> Text("Ładowanie…")
                            petList.isEmpty()   -> Text("Brak dodanych zwierząt")
                            else                -> Text("Wybierz zwierzaka")
                        }
                    },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedPet) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(
                    Modifier
                        .matchParentSize()
                        .clickable { if (petList.isNotEmpty()) expandedPet = true }
                )
                DropdownMenu(
                    expanded = expandedPet,
                    onDismissRequest = { expandedPet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Anuluj") },
                        onClick = {
                            selectedPet = null
                            expandedPet = false
                        }
                    )
                    Divider()
                    petList.forEach { pet ->
                        DropdownMenuItem(
                            text = { Text("${pet.name} (${pet.species})") },
                            onClick = {
                                selectedPet = pet
                                expandedPet = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Rodzaj wizyty ----
            Text(text = "Rodzaj wizyty", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedVisitType ?: "",
                    onValueChange = {},
                    placeholder = { Text("Wybierz rodzaj wizyty") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedVisitType) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(
                    Modifier
                        .matchParentSize()
                        .clickable { expandedVisitType = true }
                )
                DropdownMenu(
                    expanded = expandedVisitType,
                    onDismissRequest = { expandedVisitType = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Anuluj") },
                        onClick = {
                            selectedVisitType = null
                            expandedVisitType = false
                        }
                    )
                    Divider()
                    visitTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
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
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDoctor
                        ?.let { "${it.firstName} ${it.lastName}".trim() }
                        .orEmpty(),
                    onValueChange = {},
                    placeholder = {
                        when {
                            isLoadingDoctors     -> Text("Ładowanie…")
                            doctorList.isEmpty() -> Text("Brak dostępnych lekarzy")
                            else                 -> Text("Wybierz lekarza")
                        }
                    },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedDoctor) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(
                    Modifier
                        .matchParentSize()
                        .clickable {
                            if (!isLoadingDoctors && doctorList.isNotEmpty()) {
                                expandedDoctor = true
                            }
                        }
                )
                DropdownMenu(
                    expanded = expandedDoctor,
                    onDismissRequest = { expandedDoctor = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Anuluj") },
                        onClick = {
                            selectedDoctor = null
                            expandedDoctor = false
                        }
                    )
                    Divider()
                    doctorList.forEach { docUser ->
                        val label = "${docUser.firstName} ${docUser.lastName}".trim()
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedDoctor = docUser
                                expandedDoctor = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isFormValid = selectedPet != null && selectedVisitType != null && selectedDoctor != null

            Button(
                onClick = {
                    if (isFormValid) {
                        onConfirm(selectedPet, selectedVisitType!!, selectedDoctor!!)
                    }
                },
                enabled = isFormValid, // ← tylko jeśli wszystko uzupełnione
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Potwierdź wizytę")
            }
        }
    }
}

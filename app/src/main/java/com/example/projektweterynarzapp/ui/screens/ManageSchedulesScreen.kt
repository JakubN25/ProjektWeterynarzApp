package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.AuthRepository.ScheduleRepository
import com.example.projektweterynarzapp.data.models.DoctorSchedule
import com.example.projektweterynarzapp.data.models.TimeRange
import com.example.projektweterynarzapp.data.models.User
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSchedulesScreen(
    onBack: () -> Unit,
    authRepo: AuthRepository = remember { AuthRepository() },
    schedRepo: ScheduleRepository = remember { ScheduleRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // stan ekranu
    var doctors by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedDoctor by remember { mutableStateOf<User?>(null) }
    var scheduleMap by remember { mutableStateOf<Map<String, TimeRange>>(emptyMap()) }

    // 1) Załaduj lekarzy raz po wejściu na ekran
    LaunchedEffect(Unit) {
        doctors = authRepo.getDoctors()
    }

    // 2) Gdy wybierzesz lekarza, pobierz jego grafik
    LaunchedEffect(selectedDoctor) {
        scheduleMap = selectedDoctor
            ?.let { schedRepo.getSchedule(it.uid)?.schedules }
            ?: emptyMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grafiki lekarzy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- Dropdown lekarza ---
            var expanded by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = selectedDoctor?.let { "${it.firstName} ${it.lastName}" } ?: "",
                onValueChange = { /* readOnly */ },
                label = { Text("Wybierz lekarza") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Rozwiń",
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                doctors.forEach { doc ->
                    DropdownMenuItem(
                        text = { Text("${doc.firstName} ${doc.lastName}") },
                        onClick = {
                            selectedDoctor = doc
                            expanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Formularz dni Mon–Fri ---
            DayOfWeek.values()
                .filter { it.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value }
                .forEach { day ->
                    // Pobieramy aktualne wartości z mapy (mogą być puste)
                    val current = scheduleMap[day.name]
                    val start = current?.start.orEmpty()
                    val end   = current?.end  .orEmpty()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = day.name, modifier = Modifier.weight(1f))

                        OutlinedTextField(
                            value = start,
                            onValueChange = { newStart ->
                                // Zaktualizuj tylko start, zostaw end bez zmian
                                scheduleMap = scheduleMap.toMutableMap().also {
                                    it[day.name] = TimeRange(start = newStart, end = end)
                                }
                            },
                            label = { Text("Od") },
                            placeholder = { Text("--:--") },
                            modifier = Modifier.width(100.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        OutlinedTextField(
                            value = end,
                            onValueChange = { newEnd ->
                                // Zaktualizuj tylko end, zostaw start bez zmian
                                scheduleMap = scheduleMap.toMutableMap().also {
                                    it[day.name] = TimeRange(start = start, end = newEnd)
                                }
                            },
                            label = { Text("Do") },
                            placeholder = { Text("--:--") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

            Spacer(modifier = Modifier.weight(1f))

            // --- Zapis ---
            Button(
                onClick = {
                    selectedDoctor?.let { doc ->
                        val newSchedule = DoctorSchedule(
                            doctorId = doc.uid,
                            schedules = scheduleMap
                        )
                        coroutineScope.launch {
                            val ok = schedRepo.saveSchedule(newSchedule)
                            if (ok) {
                                snackbarHostState.showSnackbar("Zapisano grafik")
                            } else {
                                snackbarHostState.showSnackbar("Błąd podczas zapisu")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz grafik")
            }
        }
    }
}

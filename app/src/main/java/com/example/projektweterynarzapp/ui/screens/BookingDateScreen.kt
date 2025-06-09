// ui/screens/BookingDateScreen.kt
package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projektweterynarzapp.ui.navigation.Screen
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.DoctorSchedule
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateScreen(
    location: String,
    navController: NavHostController
) {
    val authRepo = remember { AuthRepository() }
    val scheduleRepo = remember { AuthRepository.ScheduleRepository() }
    val context = LocalContext.current

    // --- Załaduj wszystkie grafiki ---
    var doctorSchedules by remember { mutableStateOf<List<DoctorSchedule>>(emptyList()) }
    LaunchedEffect(Unit) {
        doctorSchedules = scheduleRepo.getAllDoctorsSchedules()
    }

    // --- Data picker ---
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        initialDisplayedMonthMillis = todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val pickedDate = Instant
                    .ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                // nie przed dzisiaj
                if (pickedDate.isBefore(today)) return false
                // wyklucz weekend
                return when (pickedDate.dayOfWeek) {
                    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
                    else -> true
                }
            }
        }
    )

    // --- Generowanie slotów co 20 minut od 08:00 do 20:00 ---
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeSlots by remember {
        mutableStateOf(
            generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(20) }
                .takeWhile { it <= LocalTime.of(20, 0) }
                .map { it.format(timeFormatter) }
                .toList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wybierz termin – $location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                navigationIcon = {
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
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Wybierz datę",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Wybierz datę")
            }

            Spacer(Modifier.height(16.dp))

            selectedDate?.let { date ->
                val formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val displayDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val dayName = date.dayOfWeek.name

                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(timeSlots) { slot ->
                        val slotTime = LocalTime.parse(slot, timeFormatter)
                        val isToday = date == LocalDate.now()
                        val nowTime = LocalTime.now()
                        val isPastTime = isToday && slotTime.isBefore(nowTime)

                        // czy choć jeden lekarz jest dostępny o tej godzinie
                        val isAnyDoctorAvailable = doctorSchedules.any { ds ->
                            ds.schedules[dayName]?.let { tr ->
                                val start = LocalTime.parse(tr.start, timeFormatter)
                                val end = LocalTime.parse(tr.end, timeFormatter)
                                !slotTime.isBefore(start) && slotTime.isBefore(end)
                            } ?: false
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isPastTime && isAnyDoctorAvailable) {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(location, formattedDate, slot)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isPastTime && isAnyDoctorAvailable
                        ) {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { epoch ->
                            selectedDate = Instant
                                .ofEpochMilli(epoch)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Anuluj")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

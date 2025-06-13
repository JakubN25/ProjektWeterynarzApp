package com.example.projektweterynarzapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projektweterynarzapp.ui.navigation.Screen
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.DoctorSchedule
import com.example.projektweterynarzapp.data.models.Booking
import com.example.projektweterynarzapp.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import com.example.projektweterynarzapp.data.models.Branch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateScreen(
    location: String,
    navController: NavHostController
) {
    val authRepo = remember { AuthRepository() }
    val scheduleRepo = remember { AuthRepository.ScheduleRepository() }

    val branchId = Branch.fromName(location)?.id
    // --- Stan DatePicker Dialog ---
    var showDatePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val todayMillis = today
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        initialDisplayedMonthMillis = todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val pickedDate = Instant
                    .ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                // nie przed dzisiaj i nie weekend
                return !pickedDate.isBefore(today) &&
                        pickedDate.dayOfWeek != DayOfWeek.SATURDAY &&
                        pickedDate.dayOfWeek != DayOfWeek.SUNDAY
            }
        }
    )

    // --- Stan aplikacji ---
    var doctorList by remember { mutableStateOf<List<User>>(emptyList()) }
    var doctorSchedules by remember { mutableStateOf<List<DoctorSchedule>>(emptyList()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var occupiedSlotsByDoctor by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeSlots by remember {
        mutableStateOf(
            generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(20) }
                .takeWhile { it <= LocalTime.of(20, 0) }
                .map { it.format(timeFormatter) }
                .toList()
        )
    }

    // --- Ładujemy harmonogramy lekarzy raz ---
    LaunchedEffect(location) {
        // pobieramy lekarzy tylko z naszego branchu
        branchId?.let {
            doctorList = authRepo.getDoctorsByBranch(it)
        }
        // pobieramy wszystkie harmonogramy, a następnie filtrujemy po id lekarzy
        val allSchedules = scheduleRepo.getAllDoctorsSchedules()
        doctorSchedules = allSchedules.filter { ds ->
            doctorList.any { it.uid == ds.doctorId }
        }
    }


    // --- Gdy zmienia się data lub harmonogramy, pobieramy bookingi i budujemy occupiedSlots ---
    LaunchedEffect(selectedDate, doctorSchedules) {
        val dateKey = selectedDate?.format(DateTimeFormatter.ISO_DATE) ?: return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()

        val map = mutableMapOf<String, MutableSet<String>>()
        doctorSchedules.forEach { ds ->
            val taken = mutableSetOf<String>()
            try {
                val snap = db.collection("users")
                    .document(ds.doctorId)
                    .collection("bookings")
                    .whereEqualTo("date", dateKey)
                    .get()
                    .await()

                snap.documents.mapNotNull { it.toObject(Booking::class.java) }
                    .forEach { b ->
                        val startIndex = timeSlots.indexOf(b.hour).takeIf { it >= 0 } ?: return@forEach
                        val span = (b.duration / 20) - 1
                        for (i in 0..span.coerceAtLeast(0)) {
                            timeSlots.getOrNull(startIndex + i)?.let { taken.add(it) }
                        }
                    }
            } catch (e: Exception) {
                Log.e("BookingDateScreen", "Błąd pobierania bookingów: ${e.localizedMessage}")
            }
            map[ds.doctorId] = taken
        }
        occupiedSlotsByDoctor = map
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

            // Przycisk otwierający DatePicker
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
                val displayDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val dayName = date.dayOfWeek.name

                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val isToday = date == today
                val nowTime = LocalTime.now()

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
                        val isPastTime = isToday && slotTime.isBefore(nowTime)

                        val fitsSchedule = doctorSchedules.any { ds ->
                            ds.schedules[dayName]?.let { tr ->
                                val start = LocalTime.parse(tr.start, timeFormatter)
                                val end = LocalTime.parse(tr.end, timeFormatter)
                                !slotTime.isBefore(start) && slotTime.isBefore(end)
                            } ?: false
                        }

                        // Zwraca true, jeśli istnieje lekarz, który
                        // 1) pracuje w tym slotcie i
                        // 2) nie ma go w occupiedSlotsByDoctor
                        val isFreeForSomeDoctor = doctorSchedules.any { ds ->
                            // 1) Lekarz pracuje o tej godzinie?
                            ds.schedules[dayName]?.let { tr ->
                                val start = LocalTime.parse(tr.start, timeFormatter)
                                val end   = LocalTime.parse(tr.end,   timeFormatter)
                                !slotTime.isBefore(start) && slotTime.isBefore(end)
                            } ?: false
                                    // 2) i nie jest zajęty?
                                    && !(occupiedSlotsByDoctor[ds.doctorId]?.contains(slot) == true)
                        }

                        val enabled = !isPastTime && isFreeForSomeDoctor

                        OutlinedButton(
                            onClick = {
                                if (enabled) {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(
                                            location,
                                            date.format(DateTimeFormatter.ISO_DATE),
                                            slot
                                        )
                                    )
                                }
                            },
                            enabled = enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(text = slot)
                        }
                    }
                }
            }
        }
    }

    // DatePickerDialog
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
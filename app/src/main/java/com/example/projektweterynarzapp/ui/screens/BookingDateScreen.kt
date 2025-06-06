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

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

/**
 * Ekran: „Wybierz termin wizyty – {location}”
 *
 * Po kliknięciu godziny nawigujemy do ekranu BookingDetails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateScreen(
    location: String,
    navController: NavHostController
) {
    val authRepo = remember { AuthRepository() }
    val context = LocalContext.current

    // Czy pokazujemy dialog kalendarza?
    var showDatePicker by remember { mutableStateOf(false) }

    // Wybrana data
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // State dla DatePickerDialog
    val datePickerState = rememberDatePickerState()

    // Lista godzin co 20 min: 08:00..17:40
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeSlots: List<String> by remember {
        mutableStateOf(
            generateSequence(LocalTime.of(8, 0)) { prev -> prev.plusMinutes(20) }
                .takeWhile { it <= LocalTime.of(17, 40) }
                .map { it.format(timeFormatter) }
                .toList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wybierz termin wizyty – $location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                navigationIcon = {
                    // ← Strzałka „wstecz” – wracamy do wyboru lokalu
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

            // 1) Przycisk „Wybierz datę”
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
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wybierz datę")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2) Po wybraniu daty wyświetlamy siatkę godzin
            selectedDate?.let { date ->
                val formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                // Uwaga: tutaj formatujemy datę w formacie ISO (yyyy-MM-dd),
                // żeby łatwo przekazać ją jako argument w trasie

                Text(
                    text = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
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
                        OutlinedButton(
                            onClick = {
                                // 3) Po kliknięciu godziny nawigujemy:
                                // parametry: location / date / hour
                                navController.navigate(
                                    Screen.BookingDetails
                                        .createRoute(location, formattedDate, slot)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
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

        // 4) Dialog: DatePicker
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val epochMillis = datePickerState.selectedDateMillis
                        if (epochMillis != null) {
                            selectedDate = Instant
                                .ofEpochMilli(epochMillis)
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

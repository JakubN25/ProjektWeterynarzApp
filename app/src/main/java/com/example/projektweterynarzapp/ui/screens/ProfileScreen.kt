// ui/screens/ProfileScreen.kt
package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.User
import kotlinx.coroutines.launch

/**
 * Parametr:
 *  onNavigateBack – wywoływane, gdy użytkownik chce wrócić do ekranu Home
 */
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit
) {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = authRepo.getCurrentUser()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Stany pól edycyjnych
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    // Przechowamy także cały obiekt User (od Firestore), żeby przy aktualizacji nie utracić pól role, created, email
    var userObject by remember { mutableStateOf<User?>(null) }

    // Na wejściu wczytujemy istniejące dane z Firestore
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            val uid = currentUser.uid
            val fetchedUser = authRepo.getUserProfile(uid)
            if (fetchedUser != null) {
                userObject = fetchedUser
                firstName = fetchedUser.firstName
                lastName = fetchedUser.lastName
                phone = fetchedUser.phone
                address = fetchedUser.address
                city = fetchedUser.city
            } else {
                errorMessage = "Nie udało się pobrać profilu."
            }
        } else {
            errorMessage = "Brak zalogowanego użytkownika."
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mój Profil",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Imię") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nazwisko") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Numer telefonu") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Adres (ulica i nr)") },
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Miasto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Sprawdzenie, czy mamy obecny obiekt userObject
                    val baseUser = userObject
                    if (baseUser != null) {
                        isLoading = true
                        coroutineScope.launch {
                            // Tworzymy nowy obiekt User, kopiując istniejące pola, ale nadpisując te edytowalne
                            val updatedUser = baseUser.copy(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                city = city.trim()
                            )
                            val success = authRepo.updateUserProfile(updatedUser)
                            isLoading = false
                            if (!success) {
                                errorMessage = "Nie udało się zaktualizować danych."
                            } else {
                                // Opcjonalnie: wyświetl toast lub message “Zapisano pomyślnie”
                            }
                        }
                    } else {
                        errorMessage = "Dane użytkownika są niekompletne."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Zapisz zmiany")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Wróć")
            }
        }
    }
}

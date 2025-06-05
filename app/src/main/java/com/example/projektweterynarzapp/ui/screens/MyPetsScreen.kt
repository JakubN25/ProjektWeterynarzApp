package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.Pet
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetsScreen(onNavigateBack: () -> Boolean) {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    // Pola formularza „Dodaj nowe zwierzę”
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }

    // Stan dla „rozmiaru” w formularzu dodawania:
    var size by remember { mutableStateOf("") }
    var expandedSize by remember { mutableStateOf(false) }
    val sizeOptions = listOf("Mały", "Średni", "Duży")

    // Lista pets + ładowanie
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Funkcja pobierania listy zwierzaków
    fun loadPets() {
        coroutineScope.launch {
            isLoading = true
            pets = authRepo.getPets()
            isLoading = false
        }
    }

    // Po wejściu na ekran od razu ładuj listę
    LaunchedEffect(Unit) {
        loadPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje zwierzęta") }
                // Usunięto navigationIcon – cofanie nie jest dostępne
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Dodaj nowe zwierzę",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ---- Imię zwierzaka ----
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Imię zwierzaka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // ---- Gatunek ----
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Gatunek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // ---- Rozmiar (usuń etykietę – zostaje tylko dropdown) ----
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { /* brak ręcznej edycji */ },
                        placeholder = { Text("Wybierz rozmiar") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expandedSize) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { expandedSize = !expandedSize }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSize = true }
                    )
                    DropdownMenu(
                        expanded = expandedSize,
                        onDismissRequest = { expandedSize = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sizeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    size = option
                                    expandedSize = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ---- Wiek ----
                OutlinedTextField(
                    value = ageText,
                    onValueChange = {
                        // przyjmujemy tylko cyfry
                        if (it.all { char -> char.isDigit() }) {
                            ageText = it
                        }
                    },
                    label = { Text("Wiek") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ---- Przycisk „Dodaj zwierzaka” ----
                Button(
                    onClick = {
                        // Walidacja: wszystkie pola muszą być uzupełnione
                        val age = ageText.toIntOrNull() ?: -1
                        if (name.isBlank() || species.isBlank() || size.isBlank() || age < 0) {
                            errorMessage = "Proszę wypełnić wszystkie pola prawidłowo."
                            return@Button
                        }
                        errorMessage = null

                        coroutineScope.launch {
                            val success = authRepo.addPet(
                                Pet(
                                    id = "",  // Firestore sam wygeneruje
                                    name = name.trim(),
                                    species = species.trim(),
                                    size = size,
                                    age = age
                                )
                            )
                            if (success) {
                                // Wyczyszczenie formularza
                                name = ""
                                species = ""
                                size = ""
                                ageText = ""
                                loadPets()
                            } else {
                                errorMessage = "Dodanie zwierzaka nie powiodło się."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj zwierzaka")
                }
                Spacer(modifier = Modifier.height(24.dp))

                // ---- Lista istniejących zwierzaków ----
                Text(
                    text = "Moje zwierzęta:",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (pets.isEmpty()) {
                        Text(
                            text = "Brak dodanych zwierzaków.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn {
                            items(pets) { pet ->
                                PetRow(
                                    pet = pet,
                                    onUpdate = { updatedPet ->
                                        coroutineScope.launch {
                                            val ok = authRepo.updatePet(updatedPet)
                                            if (ok) loadPets()
                                        }
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            val ok = authRepo.deletePet(pet.id)
                                            if (ok) loadPets()
                                        }
                                    }
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}


@Composable
private fun PetRow(
    pet: Pet,
    onUpdate: (Pet) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(pet.name) }
    var species by remember { mutableStateOf(pet.species) }
    var ageText by remember { mutableStateOf(pet.age.toString()) }

    // Nowe stany dla rozmiaru w trybie edycji
    var size by remember { mutableStateOf(pet.size) }
    var expandedSize by remember { mutableStateOf(false) }
    val sizeOptions = listOf("Mały", "Średni", "Duży")

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isEditing) {
            // Tryb tylko‐do‐odczytu – wyświetlamy wszystkie pola, w tym rozmiar
            Text("Imię: ${pet.name}", style = MaterialTheme.typography.bodyLarge)
            Text("Gatunek: ${pet.species}", style = MaterialTheme.typography.bodyLarge)
            Text("Rozmiar: ${pet.size}", style = MaterialTheme.typography.bodyLarge)
            Text("Wiek: ${pet.age}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                TextButton(onClick = { isEditing = true }) {
                    Text("Edytuj")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDelete) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            // Tryb edycji – pozwalamy zmienić name, species, size i age
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Imię") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Gatunek") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            // --- Dropdown „Rozmiar” bez dodatkowej etykiety ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = size,
                    onValueChange = { /* brak ręcznej edycji */ },
                    placeholder = { Text("Wybierz rozmiar") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedSize) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { expandedSize = !expandedSize }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSize = true }
                )
                DropdownMenu(
                    expanded = expandedSize,
                    onDismissRequest = { expandedSize = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sizeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                size = option
                                expandedSize = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = ageText,
                onValueChange = {
                    if (it.all { c -> c.isDigit() }) {
                        ageText = it
                    }
                },
                label = { Text("Wiek") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row {
                Button(onClick = {
                    val age = ageText.toIntOrNull() ?: -1
                    if (name.isBlank() || species.isBlank() || size.isBlank() || age < 0) {
                        errorMessage = "Proszę poprawić dane."
                        return@Button
                    }
                    errorMessage = null
                    onUpdate(
                        pet.copy(
                            name = name.trim(),
                            species = species.trim(),
                            size = size,
                            age = age
                        )
                    )
                    isEditing = false
                }) {
                    Text("Zapisz")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    // przywrócenie wartości sprzed edycji
                    name = pet.name
                    species = pet.species
                    size = pet.size
                    ageText = pet.age.toString()
                    errorMessage = null
                    isEditing = false
                }) {
                    Text("Anuluj")
                }
            }
        }
    }
}

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.data.models.Pet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetsScreen(onNavigateBack: () -> Boolean) {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    // Pola formularza „Dodaj nowe zwierzę”
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }

    // Stany dla list rozwijanych
    var expandedSex by remember { mutableStateOf(false) }
    var expandedSpecies by remember { mutableStateOf(false) }
    val sexOptions = listOf("Samiec", "Samica")
    val speciesOptions = listOf("Pies", "Kot", "Papuga", "Jeż", "Mysz", "Królik", "Chomik", "Świnka morska", "Wąż")

    // Lista pets + ładowanie
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadPets() {
        coroutineScope.launch {
            isLoading = true
            pets = authRepo.getPets()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje zwierzęta") }
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Dodaj nowe zwierzę",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Imię zwierzaka") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ZMIANA: Pole "Gatunek" jako dropdown
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = species,
                            onValueChange = { /* no-op */ },
                            label = { Text("Gatunek") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedSpecies) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { expandedSpecies = !expandedSpecies }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedSpecies = true }
                        )
                        DropdownMenu(
                            expanded = expandedSpecies,
                            onDismissRequest = { expandedSpecies = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            speciesOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        species = option
                                        expandedSpecies = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = { Text("Rasa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() || c == '.' }) {
                                weightText = it
                            }
                        },
                        label = { Text("Waga (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) ageText = it },
                        label = { Text("Wiek (lata)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sex,
                            onValueChange = { /* brak ręcznej edycji */ },
                            placeholder = { Text("Wybierz płeć") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedSex) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { expandedSex = !expandedSex }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedSex = true }
                        )
                        DropdownMenu(
                            expanded = expandedSex,
                            onDismissRequest = { expandedSex = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sexOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sex = option
                                        expandedSex = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Button(
                        onClick = {
                            val age = ageText.toIntOrNull() ?: -1
                            val weight = weightText.toDoubleOrNull() ?: -1.0
                            if (name.isBlank() || species.isBlank() || breed.isBlank() || sex.isBlank() || age < 0 || weight <= 0) {
                                errorMessage = "Proszę wypełnić wszystkie pola prawidłowo."
                                return@Button
                            }
                            errorMessage = null

                            coroutineScope.launch {
                                val success = authRepo.addPet(
                                    Pet(
                                        id = "",
                                        name = name.trim(),
                                        species = species.trim(),
                                        breed = breed.trim(),
                                        age = age,
                                        weight = weight,
                                        gender = sex
                                    )
                                )
                                if (success) {
                                    name = ""; species = ""; breed = ""; ageText = ""; weightText = ""; sex = ""
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
                }

                item {
                    Text(
                        text = "Moje zwierzęta:",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    if (pets.isEmpty()) {
                        item {
                            Text(
                                text = "Brak dodanych zwierzaków.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
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
    var breed by remember { mutableStateOf(pet.breed) }
    var ageText by remember { mutableStateOf(pet.age.toString()) }
    var weightText by remember { mutableStateOf(pet.weight.toString()) }
    var sex by remember { mutableStateOf(pet.gender) }

    var expandedSex by remember { mutableStateOf(false) }
    var expandedSpecies by remember { mutableStateOf(false) }
    val sexOptions = listOf("Samiec", "Samica")
    val speciesOptions = listOf("Pies", "Kot", "Papuga", "Jeż", "Mysz", "Królik", "Chomik", "Świnka morska", "Wąż")
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isEditing) {
            Text("Imię: ${pet.name}", style = MaterialTheme.typography.bodyLarge)
            Text("Gatunek: ${pet.species}", style = MaterialTheme.typography.bodyLarge)
            Text("Rasa: ${pet.breed}", style = MaterialTheme.typography.bodyLarge)
            Text("Waga: ${pet.weight} kg", style = MaterialTheme.typography.bodyLarge)
            Text("Płeć: ${pet.gender}", style = MaterialTheme.typography.bodyLarge)
            Text("Wiek: ${pet.age} lat", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                TextButton(onClick = { isEditing = true }) { Text("Edytuj") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDelete) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Imię") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            // ZMIANA: Pole "Gatunek" jako dropdown w edycji
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = species,
                    onValueChange = { /* no-op */ },
                    label = { Text("Gatunek") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedSpecies) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { expandedSpecies = !expandedSpecies }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSpecies = true }
                )
                DropdownMenu(
                    expanded = expandedSpecies,
                    onDismissRequest = { expandedSpecies = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    speciesOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                species = option
                                expandedSpecies = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Rasa") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weightText = it },
                label = { Text("Waga (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = ageText,
                onValueChange = { if (it.all { c -> c.isDigit() }) ageText = it },
                label = { Text("Wiek (lata)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = sex,
                    onValueChange = { /* no-op */ },
                    placeholder = { Text("Wybierz płeć") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedSex) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { expandedSex = !expandedSex }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSex = true }
                )
                DropdownMenu(
                    expanded = expandedSex,
                    onDismissRequest = { expandedSex = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sexOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { sex = option; expandedSex = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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
                    val weight = weightText.toDoubleOrNull() ?: -1.0
                    if (name.isBlank() || species.isBlank() || breed.isBlank() || sex.isBlank() || age < 0 || weight <= 0) {
                        errorMessage = "Proszę poprawić dane."
                        return@Button
                    }
                    errorMessage = null
                    onUpdate(
                        pet.copy(
                            name = name.trim(),
                            species = species.trim(),
                            breed = breed.trim(),
                            age = age,
                            weight = weight,
                            gender = sex
                        )
                    )
                    isEditing = false
                }) {
                    Text("Zapisz")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    name = pet.name; species = pet.species; breed = pet.breed; ageText = pet.age.toString(); weightText = pet.weight.toString(); sex = pet.gender;
                    errorMessage = null
                    isEditing = false
                }) {
                    Text("Anuluj")
                }
            }
        }
    }
}
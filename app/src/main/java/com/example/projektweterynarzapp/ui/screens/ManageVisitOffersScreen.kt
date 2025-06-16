package com.example.projektweterynarzapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Data class reprezentująca ofertę wizyty
 */
data class VisitOffer(
    val id: String = "",
    val name: String = "",
    val duration: Int = 0,
    val price: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageVisitOffersScreen(
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var offers by remember { mutableStateOf<List<VisitOffer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var editOffer by remember { mutableStateOf<VisitOffer?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDuration by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }

    // pola formularza
    var newName by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }

    // Ładowanie ofert z Firestore
    fun loadOffers() {
        coroutineScope.launch {
            try {
                loading = true
                val snapshot = db.collection("visitTypes").get().await()
                offers = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val duration = doc.getLong("duration")?.toInt() ?: return@mapNotNull null
                    val price = doc.getLong("price")?.toInt() ?: 0
                    VisitOffer(id = doc.id, name = name, duration = duration, price = price)
                }
            } catch (e: Exception) {
                Log.e("ManageOffers", "Error loading offers", e)
            } finally {
                loading = false
            }
        }
    }


    LaunchedEffect(Unit) {
        loadOffers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zarządzanie ofertą wizyt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp)
        ) {
            // Tabela ofert
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nazwa", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Czas (min)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.7f))
                Text("Cena (PLN)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.7f))
                Spacer(modifier = Modifier.weight(0.6f))
            }
            Divider(Modifier.padding(vertical = 8.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn (
                    modifier = Modifier.weight(1f)
                )
                {
                    items(offers) { offer ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(offer.name, modifier = Modifier.weight(1f))
                            Text(offer.duration.toString(), modifier = Modifier.weight(0.7f))
                            Text(offer.price.toString(), modifier = Modifier.weight(0.7f))
                            Row(modifier = Modifier.weight(0.6f)) {
                                IconButton(onClick = {
                                    // Rozpocznij edycję
                                    editOffer = offer
                                    editName = offer.name
                                    editDuration = offer.duration.toString()
                                    editPrice = offer.price.toString()
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                                }
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        try {
                                            db.collection("visitTypes").document(offer.id).delete().await()
                                            loadOffers()
                                        } catch (e: Exception) {
                                            Log.e("ManageOffers", "Error deleting offer", e)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Usuń")
                                }
                            }
                        }
                        Divider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (editOffer == null){
                Text("Dodaj nową ofertę", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nazwa wizyty") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newDuration,
                    onValueChange = { newDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("Czas trwania (min)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it.filter { c -> c.isDigit() } },
                    label = { Text("Cena (PLN)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val dur = newDuration.toIntOrNull() ?: return@launch
                            val price = newPrice.toIntOrNull() ?: return@launch
                            val data = mapOf(
                                "name" to newName,
                                "duration" to dur,
                                "price" to price
                            )
                            try {
                                db.collection("visitTypes").add(data).await()
                                loadOffers()
                                newName = ""
                                newDuration = ""
                                newPrice = ""
                            } catch (e: Exception) {
                                Log.e("ManageOffers", "Error adding offer", e)
                            }
                        }
                    },
                    enabled = newName.isNotBlank() && newDuration.isNotBlank() && newPrice.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Dodaj ofertę")
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text("Edytuj ofertę", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nazwa wizyty") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editDuration,
                    onValueChange = { editDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("Czas trwania (min)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it.filter { c -> c.isDigit() } },
                    label = { Text("Cena (PLN)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val dur = editDuration.toIntOrNull() ?: return@launch
                                val price = editPrice.toIntOrNull() ?: return@launch
                                val data = mapOf(
                                    "name" to editName,
                                    "duration" to dur,
                                    "price" to price
                                )
                                try {
                                    db.collection("visitTypes").document(editOffer!!.id).update(data).await()
                                    loadOffers()
                                    editOffer = null
                                    editName = ""
                                    editDuration = ""
                                    editPrice = ""
                                } catch (e: Exception) {
                                    Log.e("ManageOffers", "Error updating offer", e)
                                }
                            }
                        },
                        enabled = editName.isNotBlank() && editDuration.isNotBlank() && editPrice.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Zapisz zmiany")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            editOffer = null
                            editName = ""
                            editDuration = ""
                            editPrice = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Anuluj")
                    }
                }
            }


        }
    }
}
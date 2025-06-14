package com.example.projektweterynarzapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


data class UserAdmin(
    val id: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "",
    val status: String = "Aktywny"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()


    var users by remember { mutableStateOf<List<UserAdmin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    var showRoleDialog by remember { mutableStateOf(false) }
    var roleUser by remember { mutableStateOf<UserAdmin?>(null) }
    var newRole by remember { mutableStateOf("") }
    var expandedUserId by remember { mutableStateOf<String?>(null) }

    // Ładowanie użytkowników
    LaunchedEffect(Unit) {
        try {
            val snapshot = db.collection("users").get().await()
            users = snapshot.documents.mapNotNull { doc ->
                val email = doc.getString("email") ?: return@mapNotNull null
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val role = doc.getString("role") ?: ""
                val status = doc.getString("status") ?: "Aktywny"
                UserAdmin(
                    id = doc.id,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    role = role,
                    status = status
                )
            }
        } catch (e: Exception) {
            Log.e("ManageUsers", "Error loading users", e)
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zarządzanie użytkownikami") },
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
            Text("Szukaj użytkownika", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Wpisz email lub rolę...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Nagłówki kolumn
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Email", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.2f))
                Text("Imię", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.2f))
                Text("Rola", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Status", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Divider(Modifier.padding(vertical = 6.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    val filteredUsers = users.filter {
                        it.email.contains(search, ignoreCase = true) ||
                                it.role.contains(search, ignoreCase = true)
                    }
                    items(filteredUsers) { user ->
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        expandedUserId = if (expandedUserId == user.id) null else user.id
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    user.email,
                                    modifier = Modifier.weight(2.2f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${user.firstName} ${user.lastName}",
                                    modifier = Modifier.weight(2.2f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                Text(
                                    user.role,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                Text(
                                    user.status,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                            }
                            // Rozwijane akcje:
                            if (expandedUserId == user.id) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            roleUser = user
                                            showRoleDialog = true
                                            newRole = user.role
                                        },
                                        modifier = Modifier.height(36.dp)
                                    ) { Text("Rola", maxLines = 1) }

                                    OutlinedButton(
                                        onClick = { /* TODO: blokuj */ },
                                        modifier = Modifier.height(36.dp)
                                    ) { Text("Zablokuj", maxLines = 1) }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    auth.sendPasswordResetEmail(user.email)
                                                    Toast.makeText(
                                                        context,
                                                        "Wysłano email resetujący hasło do: ${user.email}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Błąd przy wysyłaniu emaila: ${e.localizedMessage}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(36.dp)
                                    ) { Text("Reset", maxLines = 1) }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    db.collection("users").document(user.id).delete().await()
                                                    users = users.filter { it.id != user.id }
                                                } catch (e: Exception) {
                                                    Log.e("ManageUsers", "Error deleting user", e)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Usuń")
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        // Dialog do zmiany roli
        if (showRoleDialog && roleUser != null) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = { Text("Zmień rolę dla ${roleUser!!.email}") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newRole,
                            onValueChange = { newRole = it },
                            label = { Text("Nowa rola (np. admin, user, doctor)") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                db.collection("users").document(roleUser!!.id)
                                    .update("role", newRole)
                                    .await()
                                users = users.map {
                                    if (it.id == roleUser!!.id) it.copy(role = newRole) else it
                                }
                                showRoleDialog = false
                            } catch (e: Exception) {
                                Log.e("ManageUsers", "Error updating role", e)
                            }
                        }
                    }) {
                        Text("Zapisz")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRoleDialog = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }
    }
}

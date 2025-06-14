package com.example.projektweterynarzapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.projektweterynarzapp.data.AuthRepository

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onSuccessfulLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val authRepo = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var globalResetInfo by remember { mutableStateOf<String?>(null) }
    var globalResetError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Logowanie", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onNavigateToHome) {
                Text(text = "<-  Powrót do strony głównej")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Komunikat po resecie hasła
            if (globalResetInfo != null) {
                Text(globalResetInfo!!, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (globalResetError != null) {
                Text(globalResetError!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val user = authRepo.login(email.trim(), password.trim())
                        isLoading = false
                        if (user != null) {
                            onSuccessfulLogin()
                        } else {
                            errorMessage = "Niepoprawny email lub hasło."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Zaloguj się")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateToRegister) {
                Text("Nie masz konta? Zarejestruj się")
            }
            TextButton(onClick = { showResetDialog = true }) {
                Text("Nie pamiętasz hasła? Zresetuj je")
            }
        }

        // --- Dialog resetowania hasła ---
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResetDialog = false
                    resetEmail = ""
                },
                title = { Text("Resetowanie hasła") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = resetEmail.isNotBlank(),
                        onClick = {
                            // Zresetuj dialog i pokaż komunikat po zamknięciu
                            showResetDialog = false
                            coroutineScope.launch {
                                val ok = authRepo.sendPasswordReset(resetEmail.trim())
                                resetEmail = ""
                                globalResetInfo = if (ok)
                                    "Link do resetu hasła został wysłany na Twój email"
                                else {
                                    globalResetError = "Nie udało się wysłać linku. Sprawdź adres email."
                                    null
                                }
                                if (ok) globalResetError = null
                            }
                        }
                    ) {
                        Text("Wyślij link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        resetEmail = ""
                    }) {
                        Text("Anuluj")
                    }
                }
            )
        }
    }
}


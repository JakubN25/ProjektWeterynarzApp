package com.example.projektweterynarzapp.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

@Composable
fun DrawerContent(
    currentUser: FirebaseUser?,
    currentUserRole: String?,
    onHomeSelected: () -> Unit,
    onLoginSelected: () -> Unit,
    onRegisterSelected: () -> Unit,
    onProfileSelected: () -> Unit,
    onPetsSelected: () -> Unit,
    onBookingSelected: () -> Unit,
    onAppointmentsSelected: () -> Unit, // ← dodano nowy callback
    onAdminSelected: () -> Unit,
    onLogoutSelected: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        DrawerItem(text = "Strona główna", onClick = onHomeSelected)
        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        if (currentUser == null) {
            DrawerItem(text = "Zaloguj się", onClick = onLoginSelected)
            Spacer(Modifier.height(12.dp))
            DrawerItem(text = "Zarejestruj się", onClick = onRegisterSelected)
        } else {
            Text(
                text = "Witaj, ${currentUser.email}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider()
            Spacer(Modifier.height(8.dp))

            DrawerItem(text = "Mój profil", onClick = onProfileSelected)
            Spacer(Modifier.height(12.dp))

            // ← NOWA POZYCJA DLA “Moje zwierzęta”
            DrawerItem(text = "Moje zwierzęta", onClick = onPetsSelected)
            Spacer(Modifier.height(12.dp))

            DrawerItem(text = "Umów wizytę", onClick = onBookingSelected)
            Spacer(Modifier.height(12.dp))

            // ← NOWA POZYCJA DLA “Moje wizyty”
            DrawerItem(text = "Moje wizyty", onClick = onAppointmentsSelected)
            Spacer(Modifier.height(12.dp))

            // ← TYLKO DLA ADMINA LUB LEKARZA
            if (currentUserRole == "admin") {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(text = "Panel administracyjny", onClick = onAdminSelected)
                Spacer(Modifier.height(12.dp))
            }

            DrawerItem(text = "Wyloguj", onClick = onLogoutSelected)
        }
    }
}

@Composable
private fun DrawerItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

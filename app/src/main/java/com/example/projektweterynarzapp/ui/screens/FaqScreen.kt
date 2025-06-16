package com.example.projektweterynarzapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen() {
    // Lista pytań i odpowiedzi zdefiniowana bezpośrednio w ekranie
    val faqItems = remember {
        listOf(
            "Jakie są godziny otwarcia?" to "Nasza klinika jest otwarta od poniedziałku do piątku w godzinach 9:00 - 20:00.",
            "Czy muszę umawiać wizytę?" to "Tak, zalecamy wcześniejsze umówienie wizyty, aby uniknąć oczekiwania. W nagłych przypadkach przyjmujemy bez umówienia, ale pacjenci z rezerwacją mają pierwszeństwo.",
            "Jak mogę umówić wizytę?" to "Wizytę można umówić telefonicznie, mailowo lub poprzez formularz rezerwacji który znajduje się w naszej aplikacji mobilnej i na naszej stronie internetowej.",
            "Czy wykonujecie zabiegi chirurgiczne?" to "Tak, wykonujemy szeroki zakres zabiegów chirurgicznych. Prosimy o kontakt w celu omówienia szczegółów i umówienia konsultacji przedzabiegowej.",
            "Jak przygotować zwierzę do wizyty?" to "Przed wizytą prosimy o niekarmienie zwierzęcia przez co najmniej 6 godzin, zwłaszcza jeśli planowane są badania krwi lub znieczulenie. Zapewnij dostęp do świeżej wody. Na wizytę prosimy przyjść z książeczką zdrowia pupila.",
            "Jakie formy płatności są akceptowane?" to "Akceptujemy płatności gotówką, kartą płatniczą (kredytową i debetową) oraz płatności mobilne BLIK.",
            "Co w przypadku nagłego wypadku poza godzinami pracy?" to "W nagłych przypadkach poza standardowymi godzinami pracy prosimy o kontakt z najbliższą całodobową kliniką weterynaryjną. Niestety, obecnie nie prowadzimy dyżuru nocnego.",
            "Czy w klinice można kupić karmę lub leki?" to "Tak, w naszej klinice dostępna jest szeroka gama specjalistycznych karm weterynaryjnych oraz leków i suplementów diety. Nasi lekarze chętnie pomogą w doborze odpowiednich produktów dla Państwa pupila.",
            "Czy oferujecie usługę czipowania zwierząt?" to "Tak, oferujemy profesjonalne i bezpieczne czipowanie zwierząt. Zabieg jest szybki i niemal bezbolesny. Po zaczipowaniu pomagamy w rejestracji numeru mikroczipa w ogólnopolskiej bazie danych."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAQ - Pytania i odpowiedzi") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(faqItems) { faqItem ->
                FaqCard(question = faqItem.first, answer = faqItem.second)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FaqCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Zwiń" else "Rozwiń"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
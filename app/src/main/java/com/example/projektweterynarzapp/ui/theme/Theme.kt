package com.example.projektweterynarzapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ProjektWeterynarzAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        // -------------- CIEMNY MOTYW --------------
        darkColorScheme(
            primary = DarkGreen200,      // jaśniejszy odcień zieleni w trybie dark
            onPrimary = Black,           // tekst na primary w dark
            secondary = Green300,
            onSecondary = Black,
            background = DarkBackground,
            onBackground = White,
            surface = DarkBackground,
            onSurface = White,
            // inne kolory można dołożyć analogicznie wg potrzeby
        )
    } else {
        // -------------- JASNY MOTYW --------------
        lightColorScheme(
            primary = Green500,
            onPrimary = OnGreen,
            primaryContainer = Green300, // ewentualnie
            onPrimaryContainer = Black,  // ewentualnie

            secondary = Green300,
            onSecondary = OnGreen,

            background = White,
            onBackground = Black,

            surface = White,
            onSurface = Black,

            error = Color(0xFFB00020),
            onError = White
            // możesz dodać też tertiary, errorContainer itp. w razie potrzeby
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

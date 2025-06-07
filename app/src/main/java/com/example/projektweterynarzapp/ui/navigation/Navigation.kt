package com.example.projektweterynarzapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.projektweterynarzapp.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
    object Admin : Screen("admin_screen")
    object Profile : Screen("profile_screen")
    object Pets : Screen("pets_screen")
    object Booking : Screen("booking_screen")
    object BookingDate : Screen("booking_date/{location}") {
        fun createRoute(location: String) = "booking_date/$location"
    }
    // NOWA trasa: ekran szczegółowego formularza (location, date, hour jako argumenty)
    object BookingDetails : Screen("booking_details/{location}/{date}/{hour}") {
        fun createRoute(location: String, date: String, hour: String): String {
            // Uwaga: jeśli date lub hour mają spacje lub inne znaki, warto je URL‐encode’ować,
            // ale dla uproszczenia zostawiamy prostą interpolację.
            return "booking_details/$location/$date/$hour"
        }
    }
}

@Composable
fun Navigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onSuccessfulLogin = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccessfulRegister = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Admin.route) {
            AdminScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ← NOWY composable dla ekranu Moje Zwierzęta:
        composable(Screen.Pets.route) {
            MyPetsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Booking.route) {
            BookingScreen(
                onSelectLocation = { location ->
                    // przejdź do ekranu wyboru daty
                    navController.navigate(Screen.BookingDate.createRoute(location))
                },
            )
        }

        // ekran wyboru terminu – przyjmujemy argument "location"
        composable(
            route = Screen.BookingDate.route,
            arguments = listOf(
                navArgument("location") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // odczytujemy przekazywany argument
            val location = backStackEntry.arguments?.getString("location") ?: ""
            BookingDateScreen(
                location = location,
                navController = navController
            )
        }
        composable(
            route = Screen.BookingDetails.route,
            arguments = listOf(
                navArgument("location") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("hour") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val location = backStackEntry.arguments?.getString("location") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val hour = backStackEntry.arguments?.getString("hour") ?: ""
            BookingDetailsScreen(
                location = location,
                date = date,
                hour = hour,
                navController = navController
            )
        }
    }
}

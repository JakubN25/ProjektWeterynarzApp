package com.example.projektweterynarzapp.ui.navigation

import DoctorAppointmentsScreen
import DoctorVisitHistoryScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.projektweterynarzapp.ui.screens.*
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
    object Admin : Screen("admin_screen")
    object Profile : Screen("profile_screen")
    object Pets : Screen("pets_screen")
    object Booking : Screen("booking_screen")
    object MyAppointments : Screen("my_appointments_screen")
    object PriceList : Screen("price_list_screen")
    object Faq : Screen("faq_screen")
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
    object ManageSchedules : Screen("manage_schedules")
    object ManageOffers : Screen("manage_offers")
    object ManageUsers : Screen("manage_users")
    object DoctorPanel : Screen("doctor_panel")
    object DoctorAppointments : Screen("doctor_appointments/{doctorId}") {
        fun createRoute(doctorId: String) = "doctor_appointments/$doctorId"
    }

    object DoctorVisitHistory : Screen("doctor_visit_history")


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
                onNavigateBack = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
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
            AdminScreen(
                onManageSchedules = {
                    navController.navigate(Screen.ManageSchedules.route)
                },
                onManageOffers = {
                    navController.navigate(Screen.ManageOffers.route)
                },
                onManageUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                }
            )
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

        composable(Screen.MyAppointments.route) {
            MyAppointmentsScreen()
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
        composable(Screen.ManageSchedules.route) {
            ManageSchedulesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageOffers.route) {
            ManageVisitOffersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DoctorPanel.route) {
            DoctorPanelScreen(
                onMyAppointments = {
                    val doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    navController.navigate(Screen.DoctorAppointments.createRoute(doctorId))
                }
                ,
                onHistory = { navController.navigate(Screen.DoctorVisitHistory.route) }
            )
        }
        composable(
            route = Screen.DoctorAppointments.route,
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorAppointmentsScreen(
                doctorId = doctorId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DoctorVisitHistory.route) {
            DoctorVisitHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PriceList.route) {
            PriceListScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Faq.route) {
            FaqScreen()
        }

    }
}

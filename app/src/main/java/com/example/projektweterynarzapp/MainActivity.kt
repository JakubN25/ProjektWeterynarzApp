package com.example.projektweterynarzapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

import kotlinx.coroutines.launch

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.ui.navigation.DrawerContent
import com.example.projektweterynarzapp.ui.navigation.Navigation
import com.example.projektweterynarzapp.ui.navigation.Screen
import com.example.projektweterynarzapp.ui.theme.ProjektWeterynarzAppTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue



class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            ProjektWeterynarzAppTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val firebaseAuth = FirebaseAuth.getInstance()
                val authRepo = remember { AuthRepository() }

                // 1) Stan dla aktualnego użytkownika
                var currentUser by remember { mutableStateOf(firebaseAuth.currentUser) }

                // 2) Stan dla roli (nullable)
                var userRole by remember { mutableStateOf<String?>(null) }

                // 3) Nasłuchiwacz zmian w currentUser (logowanie / wylogowanie)
                DisposableEffect(firebaseAuth) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    firebaseAuth.addAuthStateListener(listener)
                    onDispose { firebaseAuth.removeAuthStateListener(listener) }
                }

                // 4) Gdy currentUser się zmieni, pobieramy rolę z Firestore
                LaunchedEffect(currentUser) {
                    userRole = currentUser
                        ?.let { authRepo.getUserProfile(it.uid)?.role }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DrawerContent(
                                currentUser = currentUser,
                                currentUserRole = userRole,       // teraz na pewno nie nullieje zbyt późno
                                onHomeSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLoginSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Login.route)
                                    }
                                },
                                onRegisterSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Register.route)
                                    }
                                },
                                onProfileSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Profile.route)
                                    }
                                },
                                onPetsSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Pets.route)
                                    }
                                },
                                onBookingSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Booking.route)
                                    }
                                },
                                onAdminSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Admin.route)
                                    }
                                },
                                onLogoutSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        authRepo.logout()
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("BKRJ Vet") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        },
                        content = { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                Navigation(
                                    navController = navController,
                                    startDestination = Screen.Home.route
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}



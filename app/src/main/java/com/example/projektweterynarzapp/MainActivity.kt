package com.example.projektweterynarzapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.projektweterynarzapp.data.AuthRepository
import com.example.projektweterynarzapp.ui.navigation.*
import com.example.projektweterynarzapp.ui.theme.ProjektWeterynarzAppTheme

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

                // ← Nowy stan Snackbar
                val snackbarHostState = remember { SnackbarHostState() }

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
                                currentUserRole = userRole,
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
                                        // ← Po wylogowaniu pokaż Snackbar z potwierdzeniem
                                        snackbarHostState.showSnackbar("Wylogowano")
                                    }
                                },
                                onAppointmentsSelected = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.MyAppointments.route)
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
                        // ← Snackbar z białym tłem i wyśrodkowanym tekstem
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Snackbar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(data.visuals.message)
                                    }
                                }
                            }
                        }
                        ,
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

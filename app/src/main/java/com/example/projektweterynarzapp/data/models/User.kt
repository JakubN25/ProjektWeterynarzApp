package com.example.projektweterynarzapp.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data class odwzorowująca dokument w Firestore: "users/{uid}"
 */
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "user",
    val created: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val branch: String? = null,
    val disabled: Boolean = false
)

/**
 * Reprezentuje pojedyncze “zwierzę” w Firestore: "users/{uid}/pets/{petId}"
 */
@IgnoreExtraProperties
data class Pet(
    val id: String = "",          // Firestore nadpisze id subdokumentu
    val name: String = "",
    val species: String = "",
    val breed: String = "",       // <- nowa rasa
    val age: Int = 0,             // wiek w latach
    val weight: Double = 0.0,     // waga w kg
    val gender: String = ""          // np. "Samiec" / "Samica"
)



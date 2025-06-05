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
    val city: String = ""
)

/**
 * Reprezentuje pojedyncze “zwierzę” w Firestore: "users/{uid}/pets/{petId}"
 */
@IgnoreExtraProperties
data class Pet(
    val id: String = "",          // zostawiamy puste – w repo zwykle nadpiszemy id=doc.id
    val name: String = "",
    val species: String = "",
    val size: String = "",
    val age: Int = 0
)



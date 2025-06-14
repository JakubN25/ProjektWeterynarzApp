package com.example.projektweterynarzapp.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Booking(
    @get:Exclude val id: String = "",
    val userId:    String = "",
    val location:  String = "",
    val date:      String = "",     // yyyy-MM-dd
    val hour:      String = "",     // HH:mm – początek
    val endHour:   String = "",     // HH:mm – koniec
    val duration:  Int = 0,         // czas trwania w minutach
    val petId:     String = "",
    val petName:   String = "",
    val petSpecies:String = "",
    val visitType: String = "",     // np. "Kontrola", "Szczepienie", "Zabieg"
    val doctorId:  String = "",
    val doctorName:String = "",
    val createdAt: Timestamp? = null
)


package com.example.projektweterynarzapp.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Booking(
    @get:Exclude val id: String = "",
    val userId: String = "",
    val location: String = "",
    val date: String = "",     // yyyy-MM-dd
    val hour: String = "",     // HH:mm
    val petId: String = "",
    val petName: String = "",
    val visitType: String = "",
    val doctorId:  String = "",
    val doctorName:String = "",
    val createdAt: String = Timestamp.now().toDate().toString()
)

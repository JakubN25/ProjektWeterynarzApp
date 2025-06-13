package com.example.projektweterynarzapp.data.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class VisitType(
    val id: String = "",
    val name: String = "",
    val duration: Int = 0
)

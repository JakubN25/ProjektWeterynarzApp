package com.example.projektweterynarzapp.data.models

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Reprezentuje grafik pracy lekarza dla jednego tygodnia.
 * Każdy wpis to dzień tygodnia i zakres godzin.
 */
@IgnoreExtraProperties
data class DoctorSchedule(
    val doctorId: String = "",                 // UID lekarza
    val schedules: Map<String, TimeRange> = mapOf()
)

/**
 * Zakres godzin pracy w jednym dniu.
 * dayOfWeek: "MONDAY", "TUESDAY", ..., "FRIDAY"
 * start: "08:00", end: "15:00"
 */
@IgnoreExtraProperties
data class TimeRange(
    val start: String = "",
    val end: String = ""
)

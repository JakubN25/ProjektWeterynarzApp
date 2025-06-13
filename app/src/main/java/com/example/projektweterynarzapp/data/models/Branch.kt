package com.example.projektweterynarzapp.data.models

enum class Branch(val id: String, val displayName: String) {
    CENTRUM("1", "Warszawa Centrum"),
    PRAGA(   "2", "Warszawa Praga");

    companion object {
        fun fromId(id: String?): Branch? = values().firstOrNull { it.id == id }
        fun fromName(name: String): Branch? = values().firstOrNull { it.displayName == name }
    }
}

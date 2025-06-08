package com.example.projektweterynarzapp.data

import android.util.Log
import com.example.projektweterynarzapp.data.models.Booking
import com.example.projektweterynarzapp.data.models.Pet
import com.example.projektweterynarzapp.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.text.get

/**
 * AuthRepository odpowiada za:
 *  - rejestrację / logowanie / wylogowanie / pobranie profilu (User)
 *  - oraz (rozszerzone) CRUD dla pets w Firestore pod ścieżką "users/{uid}/pets"
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ------------------------------------
    // CZĘŚĆ A: USER (profile + auth)
    // ------------------------------------

    /**
     *  Rejestracja: tworzymy konto w FirebaseAuth, a potem dokument w Firestore.
     */
    suspend fun register(
        email: String,
        password: String
    ): FirebaseUser? {
        // utworzenie w FirebaseAuth
        val result = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val firebaseUser = result.user ?: return null

        // Po udanej rejestracji – tworzony jest dokument w Firestore:
        val uid = firebaseUser.uid
        val nowStr: String = Timestamp.now()
            .toDate()
            .toString()

        val newUser = User(
            uid = uid,
            email = email,
            role = "user",
            created = nowStr,
            firstName = "",
            lastName = "",
            phone = "",
            address = "",
            city = ""
        )

        try {
            db.collection("users")
                .document(uid)
                .set(newUser)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            // jeśli zapis nie zadziała, zostanie utworzone konto w Auth, ale bez dokumentu User
        }

        return firebaseUser
    }

    /**
     *  Logowanie: logujemy w FirebaseAuth.
     */
    suspend fun login(email: String, password: String): FirebaseUser? {
        val result = try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return result.user
    }

    /** Wylogowanie */
    fun logout() {
        auth.signOut()
    }

    /** Zwraca bieżącego użytkownika lub null */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     *  Pobranie dokumentu profilu (User) z Firestore na podstawie uid
     */
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .get()
                .await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     *  Zapis / aktualizacja profilu użytkownika (User) w Firestore
     */
    suspend fun updateUserProfile(user: User): Boolean {
        return try {
            db.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ------------------------------------
    // CZĘŚĆ B: PETS – CRUD w kolekcji "users/{uid}/pets"
    // ------------------------------------

    /** Zwraca referencję do subkolekcji pets dla zalogowanego usera */
    private fun petsCollection() = db.collection("users")
        .document(auth.currentUser?.uid ?: "")
        .collection("pets")

    /**
     * Pobranie listy zwierząt użytkownika.
     */
    suspend fun getPets(): List<Pet> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = petsCollection().get().await()
            snapshot.documents.mapNotNull { doc ->
                // Mapa dokumentu na Pet, z id = doc.id
                val pet = doc.toObject(Pet::class.java)
                pet?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getPets: błąd przy pobieraniu pets", e)
            emptyList()
        }
    }

    /**
     * Dodanie nowego zwierzaka do "users/{uid}/pets".
     */
    suspend fun addPet(pet: Pet): Boolean {
        if (auth.currentUser == null) return false
        return try {
            // Dane do wrzucenia – Firestore wygeneruje id
            val data = hashMapOf(
                "name" to pet.name,
                "species" to pet.species,
                "size" to pet.size,   // <-- dorzucamy rozmiar
                "age" to pet.age
            )
            petsCollection().add(data).await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "addPet: błąd przy dodawaniu pet", e)
            false
        }
    }

    /**
     * Aktualizacja istniejącego zwierzaka (musi mieć w pet.id poprawne id dokumentu).
     */
    suspend fun updatePet(pet: Pet): Boolean {
        if (auth.currentUser == null) return false
        if (pet.id.isBlank()) return false
        return try {
            val data = hashMapOf(
                "name" to pet.name,
                "species" to pet.species,
                "size" to pet.size,   // <-- dorzucamy rozmiar
                "age" to pet.age
            )
            petsCollection().document(pet.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "updatePet: błąd przy aktualizacji pet", e)
            false
        }
    }

    /**
     * Usunięcie zwierzaka o danym id.
     */
    suspend fun deletePet(petId: String): Boolean {
        if (auth.currentUser == null) return false
        return try {
            petsCollection().document(petId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "deletePet: błąd przy usuwaniu pet", e)
            false
        }
    }

    /**
     * Pobranie listy użytkowników z rolą "doctor" z kolekcji "users".
     */
    suspend fun getDoctors(): List<User> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getDoctors: błąd przy pobieraniu doctorów", e)
            emptyList()
        }
    }

    /**
    +     * Zapisuje nową wizytę w subkolekcji "bookings" pod bieżącym użytkownikiem.
    +     */
    suspend fun addBooking(
        location: String,
        date: String,
        hour: String,
        petId: String,
        petName: String,
        petSpecies: String,
        visitType: String,
        doctorId: String,
        doctorName: String
    ): Boolean {
        val clientUid = auth.currentUser?.uid ?: return false

        // 1) przygotuj obiekt Booking z nowymi polami
        val booking = Booking(
            userId     = clientUid,
            location   = location,
            date       = date,
            hour       = hour,
            petId      = petId,
            petName    = petName,
            petSpecies = petSpecies,
            visitType  = visitType,
            doctorId   = doctorId,
            doctorName = doctorName,
            createdAt  = Timestamp.now().toDate().toString()
        )

        return try {
            // 2) dodaj wizytę pod klientem
            db.collection("users")
                .document(clientUid)
                .collection("bookings")
                .add(booking)
                .await()

            // 3) dodaj tę samą wizytę pod doktorem (po uid, bez szukania po imieniu)
            db.collection("users")
                .document(doctorId)
                .collection("bookings")
                .add(booking)
                .await()

            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "addBooking failed", e)
            false
        }
    }

    /** Zwraca referencję do subkolekcji bookings dla zalogowanego usera */
    private fun bookingsCollection() = db.collection("users")
        .document(auth.currentUser?.uid ?: "")
        .collection("bookings")


    /**
     * Pobranie listy wizyt użytkownika.
     */
    suspend fun getBookings(): List<Booking> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = bookingsCollection().get().await()
            snapshot.documents.mapNotNull { doc ->
                // Mapa dokumentu na Booking, z id = doc.id
                val booking = doc.toObject(Booking::class.java)
                booking?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getBookings: błąd przy pobieraniu wizyt", e)
            emptyList()
        }
    }



}





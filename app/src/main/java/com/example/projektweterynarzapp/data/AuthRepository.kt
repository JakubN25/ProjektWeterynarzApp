package com.example.projektweterynarzapp.data

import android.util.Log
import com.example.projektweterynarzapp.data.models.Booking
import com.example.projektweterynarzapp.data.models.Branch
import com.example.projektweterynarzapp.data.models.DoctorSchedule
import com.example.projektweterynarzapp.data.models.Pet
import com.example.projektweterynarzapp.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.text.get
import com.example.projektweterynarzapp.data.models.VisitType


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
        val user = result.user ?: return null

        // Nowy kod! Sprawdź pole disabled:
        val userDoc = db.collection("users").document(user.uid).get().await()
        if (userDoc.exists() && (userDoc.getBoolean("disabled") == true)) {
            auth.signOut()
            throw Exception("Twoje konto zostało zablokowane przez administratora.")
        }

        return user
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
    suspend fun updateDoctorBranch(doctorId: String, branch: Branch): Boolean {
        return try {
            db.collection("users")
                .document(doctorId)
                .update("branch", branch.id)  // branch.id to teraz String
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
            val data = hashMapOf(
                "name"    to pet.name,
                "species" to pet.species,
                "breed"   to pet.breed,
                "age"     to pet.age,
                "weight"  to pet.weight,
                "sex"     to pet.sex
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
        if (auth.currentUser == null || pet.id.isBlank()) return false
        return try {
            val data = hashMapOf(
                "name"    to pet.name,
                "species" to pet.species,
                "breed"   to pet.breed,
                "age"     to pet.age,
                "weight"  to pet.weight,
                "sex"     to pet.sex
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

    suspend fun getDoctorsByBranch(branchId: String): List<User> {
        return db.collection("users")
            .whereEqualTo("role", "doctor")
            .whereEqualTo("branch", branchId)   // teraz String
            .get().await()
            .documents.mapNotNull { it.toObject(User::class.java) }
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
        visitDuration: Int, // <-- DODAJ TO
        doctorId: String,
        doctorName: String
    ): Boolean {
        val clientUid = auth.currentUser?.uid ?: return false

        val duration = visitDuration // <-- Używaj tego

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalTime.parse(hour, timeFormatter)
        val endTime = startTime.plusMinutes(duration.toLong()).minusMinutes(1)
        val endHour = endTime.format(timeFormatter)

        val booking = Booking(
            userId     = clientUid,
            location   = location,
            date       = date,
            hour       = hour,
            endHour    = endHour,
            duration   = duration,
            petId      = petId,
            petName    = petName,
            petSpecies = petSpecies,
            visitType  = visitType,
            doctorId   = doctorId,
            doctorName = doctorName,
            createdAt  = Timestamp.now()
        )

        // 4) Zapis w Firestore (tak jak było)
        return try {
            db.collection("users")
                .document(clientUid)
                .collection("bookings")
                .add(booking).await()
            db.collection("users")
                .document(doctorId)
                .collection("bookings")
                .add(booking).await()
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

    class ScheduleRepository {
        private val db = FirebaseFirestore.getInstance()

        // Ścieżka: kolekcja "schedules", dokument = doctorId
        private fun scheduleDoc(doctorId: String) =
            db.collection("schedules").document(doctorId)

        suspend fun getSchedule(doctorId: String): DoctorSchedule? {
            return try {
                val snap = scheduleDoc(doctorId).get().await()
                snap.toObject(DoctorSchedule::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        suspend fun saveSchedule(schedule: DoctorSchedule): Boolean {
            return try {
                scheduleDoc(schedule.doctorId).set(schedule).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        suspend fun getAllDoctorsSchedules(): List<DoctorSchedule> {
            return try {
                val snap = db.collection("schedules").get().await()
                snap.documents.mapNotNull { it.toObject(DoctorSchedule::class.java) }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    //VISIT TYPES
    /**
     * Pobiera wszystkie typy wizyt (kolekcja "visitTypes")
     */
    suspend fun getVisitTypes(): List<VisitType> {
        return try {
            val snap = db.collection("visitTypes")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(VisitType::class.java)
                    ?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getVisitTypes failed", e)
            emptyList()
        }
    }

    /**
     * Dodaje nowy typ wizyty
     */
    suspend fun addVisitType(name: String, duration: Int): Boolean {
        return try {
            db.collection("visitTypes")
                .add(mapOf(
                    "name" to name,
                    "duration" to duration
                ))
                .await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "addVisitType failed", e)
            false
        }
    }

    // Pobranie wizyt danego doktora (z jego podkolekcji bookings)
    suspend fun getDoctorBookings(doctorId: String): List<Booking> {
        return try {
            val snap = db.collection("users")
                .document(doctorId)
                .collection("bookings")
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(Booking::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getDoctorBookings error", e)
            emptyList()
        }
    }

    suspend fun deleteDoctorBooking(doctorId: String, bookingId: String): Boolean {
        return try {
            db.collection("users")
                .document(doctorId)
                .collection("bookings")
                .document(bookingId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "deleteDoctorBooking error", e)
            false
        }
    }



    // Pobranie wszystkich userów z rolą "user"
    suspend fun getAllPatients(): List<User> {
        return try {
            val snap = db.collection("users")
                .whereEqualTo("role", "user")
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getAllPatients error", e)
            emptyList()
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snap = db.collection("users").get().await()
            snap.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getAllUsers error", e)
            emptyList()
        }
    }


    // Pobranie wizyt danego usera
    suspend fun getPatientBookings(userId: String): List<Booking> {
        return try {
            val snap = db.collection("users")
                .document(userId)
                .collection("bookings")
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(Booking::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "getPatientBookings error", e)
            emptyList()
        }
    }
    suspend fun deleteUserBooking(userId: String, bookingId: String): Boolean {
        return try {
            db.collection("users")
                .document(userId)
                .collection("bookings")
                .document(bookingId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "deleteUserBooking error", e)
            false
        }
    }

    suspend fun sendPasswordReset(email: String): Boolean {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun isProfileComplete(): Boolean {
        val user = getCurrentUser() ?: return false
        val profile = getUserProfile(user.uid) ?: return false
        return profile.firstName.isNotBlank() &&
                profile.lastName.isNotBlank() &&
                profile.phone.isNotBlank() &&
                profile.address.isNotBlank() &&
                profile.city.isNotBlank()
    }

    // AuthRepository
    suspend fun getUserPets(userId: String): List<Pet> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("pets")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val pet = doc.toObject(Pet::class.java)
                pet?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }




}





package com.example.projektweterynarzapp.data

import android.util.Log
import com.example.projektweterynarzapp.data.models.Pet
import com.example.projektweterynarzapp.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
        val now = Timestamp.now()

        val newUser = User(
            uid = uid,
            email = email,
            role = "user",
            created = now.toString(),
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
}

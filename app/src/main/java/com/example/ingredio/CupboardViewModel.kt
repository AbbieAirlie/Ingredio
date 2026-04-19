package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ingredio.data.model.Ingredient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CupboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userIngredients = MutableLiveData<List<Ingredient>>()
    val userIngredients: LiveData<List<Ingredient>> get() = _userIngredients

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private var cupboardListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun fetchUserIngredients() {
        val userId = auth.currentUser?.uid ?: return
        
        // Remove existing listener if any
        cupboardListener?.remove()

        cupboardListener = db.collection("users").document(userId).collection("cupboard")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _status.value = "Error listening to cupboard: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val ingredients = snapshot.documents.mapNotNull { document ->
                        Ingredient(
                            id = document.getLong("id")?.toInt() ?: 0,
                            name = document.getString("name") ?: "",
                            image = document.getString("image") ?: "",
                            expiryDate = document.getLong("expiryDate"),
                            expiryType = document.getString("expiryType"),
                            storageType = document.getString("storageType")
                        )
                    }
                    _userIngredients.value = ingredients
                }
            }
    }

    fun addIngredientWithDetails(
        ingredient: Ingredient,
        expiryDate: Long?,
        expiryType: String?,
        storageType: String?
    ) {
        val userId = auth.currentUser?.uid ?: return

        val ingredientData = mutableMapOf<String, Any?>(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "image" to ingredient.image,
            "expiryDate" to expiryDate,
            "expiryType" to expiryType,
            "storageType" to storageType
        )

        val docId = ingredient.name.lowercase().trim().ifEmpty { "unknown_${System.currentTimeMillis()}" }
        db.collection("users").document(userId).collection("cupboard")
            .document(docId)
            .set(ingredientData)
            .addOnSuccessListener {
                _status.value = "${ingredient.name} updated in cupboard"
            }
            .addOnFailureListener { e ->
                _status.value = "Error updating ingredient: ${e.message}"
            }
    }

    fun addIngredientToCupboard(ingredient: Ingredient) {
        val userId = auth.currentUser?.uid ?: run {
            _status.value = "User not logged in"
            return
        }

        val ingredientData = hashMapOf(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "image" to ingredient.image
        )

        val docId = ingredient.name.lowercase().trim().ifEmpty { "unknown_${System.currentTimeMillis()}" }
        db.collection("users").document(userId).collection("cupboard")
            .document(docId)
            .set(ingredientData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                _status.value = "${ingredient.name} added to cupboard"
            }
            .addOnFailureListener { e ->
                _status.value = "Error adding ingredient: ${e.message}"
            }
    }

    fun removeIngredientFromCupboard(ingredient: Ingredient) {
        val userId = auth.currentUser?.uid ?: return
        val docId = ingredient.name.lowercase().trim().ifEmpty { ingredient.id.toString() }
        db.collection("users").document(userId).collection("cupboard")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                _status.value = "Ingredient removed"
            }
    }

    override fun onCleared() {
        super.onCleared()
        cupboardListener?.remove()
    }
}
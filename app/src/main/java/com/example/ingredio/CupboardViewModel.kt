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

    fun fetchUserIngredients() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("cupboard")
            .get()
            .addOnSuccessListener { result ->
                val ingredients = result.map { document ->
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
            .addOnFailureListener { exception ->
                _status.value = "Error fetching ingredients: ${exception.message}"
            }
    }

    fun addIngredientWithDetails(
        ingredient: Ingredient,
        expiryDate: Long?,
        expiryType: String,
        storageType: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        val ingredientData = hashMapOf(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "image" to ingredient.image,
            "expiryDate" to expiryDate,
            "expiryType" to expiryType,
            "storageType" to storageType
        )

        db.collection("users").document(userId).collection("cupboard")
            .document(ingredient.id.toString())
            .set(ingredientData)
            .addOnSuccessListener {
                _status.value = "${ingredient.name} added to cupboard"
                fetchUserIngredients()
            }
            .addOnFailureListener { e ->
                _status.value = "Error adding ingredient: ${e.message}"
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

        db.collection("users").document(userId).collection("cupboard")
            .document(ingredient.id.toString())
            .set(ingredientData)
            .addOnSuccessListener {
                _status.value = "${ingredient.name} added to cupboard"
                fetchUserIngredients()
            }
            .addOnFailureListener { e ->
                _status.value = "Error adding ingredient: ${e.message}"
            }
    }

    fun removeIngredientFromCupboard(ingredientId: Int) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("cupboard")
            .document(ingredientId.toString())
            .delete()
            .addOnSuccessListener {
                _status.value = "Ingredient removed"
                fetchUserIngredients()
            }
    }
}
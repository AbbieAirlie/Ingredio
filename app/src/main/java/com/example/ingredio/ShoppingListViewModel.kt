package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ingredio.data.model.Ingredient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShoppingListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _shoppingListItems = MutableLiveData<List<Ingredient>>()
    val shoppingListItems: LiveData<List<Ingredient>> get() = _shoppingListItems

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private var shoppingListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun fetchShoppingList() {
        val userId = auth.currentUser?.uid ?: return
        
        shoppingListener?.remove()

        shoppingListener = db.collection("users").document(userId).collection("shoppingList")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _status.value = "Error listening to shopping list: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { document ->
                        Ingredient(
                            id = document.getLong("id")?.toInt() ?: 0,
                            name = document.getString("name") ?: "",
                            image = document.getString("image") ?: ""
                        )
                    }
                    _shoppingListItems.value = items
                }
            }
    }

    fun addIngredientToShoppingList(ingredient: Ingredient) {
        val userId = auth.currentUser?.uid ?: run {
            _status.value = "User not logged in"
            return
        }

        val ingredientData = hashMapOf(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "image" to ingredient.image
        )

        db.collection("users").document(userId).collection("shoppingList")
            .document(ingredient.id.toString())
            .set(ingredientData)
            .addOnSuccessListener {
                _status.value = "${ingredient.name} added to shopping list"
            }
            .addOnFailureListener { e ->
                _status.value = "Error adding to shopping list: ${e.message}"
            }
    }

    fun removeIngredientFromShoppingList(ingredientId: Int) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("shoppingList")
            .document(ingredientId.toString())
            .delete()
            .addOnSuccessListener {
                _status.value = "Item removed"
            }
    }

    override fun onCleared() {
        super.onCleared()
        shoppingListener?.remove()
    }
}
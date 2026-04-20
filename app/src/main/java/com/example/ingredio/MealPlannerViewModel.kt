package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ingredio.data.model.MealPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MealPlannerViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    private var dateMealPlanListener: ListenerRegistration? = null
    private var allMealPlanListener: ListenerRegistration? = null

    private val _mealPlans = MutableLiveData<List<MealPlan>>()
    val mealPlans: LiveData<List<MealPlan>> get() = _mealPlans

    private val _allMealPlans = MutableLiveData<List<MealPlan>>()
    val allMealPlans: LiveData<List<MealPlan>> get() = _allMealPlans

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchMealPlans(date: String) {
        val userId = auth.currentUser?.uid ?: return
        
        dateMealPlanListener?.remove()

        dateMealPlanListener = db.collection("users").document(userId)
            .collection("mealPlans")
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _error.value = "Failed to fetch meal plans: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val list = snapshots.map { 
                        it.toObject(MealPlan::class.java).copy(id = it.id)
                    }
                    _mealPlans.value = list
                }
            }
    }

    fun fetchAllMealPlans() {
        val userId = auth.currentUser?.uid ?: return
        
        allMealPlanListener?.remove()

        allMealPlanListener = db.collection("users").document(userId)
            .collection("mealPlans")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _error.value = "Failed to fetch all meal plans: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.map { 
                        it.toObject(MealPlan::class.java).copy(id = it.id)
                    }
                    _allMealPlans.value = list
                }
            }
    }

    fun addMealPlan(mealPlan: MealPlan) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("mealPlans")
            .add(mealPlan)
            .addOnFailureListener { e ->
                _error.value = "Failed to add meal plan: ${e.message}"
            }
    }

    fun deleteMealPlan(mealPlan: MealPlan) {
        val userId = auth.currentUser?.uid ?: return
        if (mealPlan.id.isEmpty()) return

        db.collection("users").document(userId)
            .collection("mealPlans").document(mealPlan.id)
            .delete()
            .addOnFailureListener { e ->
                _error.value = "Failed to delete meal plan: ${e.message}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        dateMealPlanListener?.remove()
        allMealPlanListener?.remove()
    }
}

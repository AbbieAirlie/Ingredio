package com.example.ingredio.data.model

import com.google.firebase.firestore.Exclude

data class MealPlan(
    val id: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val recipeId: Int? = null,
    val recipeTitle: String? = null,
    val recipeImage: String? = null,
    val customText: String? = null,
    val mealType: String = "Lunch" // e.g., Breakfast, Lunch, Dinner, Snack
) {
    @get:Exclude
    val isCustom: Boolean get() = customText != null && recipeId == null
}

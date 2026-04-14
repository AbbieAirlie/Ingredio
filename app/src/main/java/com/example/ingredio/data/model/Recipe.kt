package com.example.ingredio.data.model

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    @SerializedName("results") val results: List<Recipe>
)

data class Recipe(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("sourceUrl") val sourceUrl: String? = null,
    @SerializedName("extendedIngredients") val extendedIngredients: List<Ingredient>? = null
)
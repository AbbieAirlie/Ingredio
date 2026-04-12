package com.example.ingredio.data.model

import com.google.gson.annotations.SerializedName

data class IngredientResponse(
    @SerializedName("results") val results: List<Ingredient>
)

data class Ingredient(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String
) {
    val imageUrl: String
        get() = "https://spoonacular.com/cdn/ingredients_100x100/$image"
}
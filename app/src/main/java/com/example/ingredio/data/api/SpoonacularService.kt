package com.example.ingredio.data.api

import com.example.ingredio.data.model.IngredientResponse
import com.example.ingredio.data.model.RecipeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SpoonacularService {
    @GET("recipes/complexSearch")
    fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("diet") diet: String? = null,
        @Query("intolerances") intolerances: String? = null,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true
    ): Call<RecipeResponse>

    @GET("recipes/complexSearch")
    fun searchRecipesByIngredients(
        @Query("apiKey") apiKey: String,
        @Query("includeIngredients") ingredients: String,
        @Query("diet") diet: String? = null,
        @Query("intolerances") intolerances: String? = null,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("sort") sort: String = "max-used-ingredients"
    ): Call<RecipeResponse>

    @GET("food/ingredients/search")
    fun searchIngredients(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int = 10
    ): Call<IngredientResponse>
}
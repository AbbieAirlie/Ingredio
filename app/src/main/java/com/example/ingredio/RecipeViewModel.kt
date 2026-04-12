package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ingredio.data.api.SpoonacularService
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.data.model.IngredientResponse
import com.example.ingredio.data.model.Recipe
import com.example.ingredio.data.model.RecipeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> get() = _recipes

    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> get() = _ingredients

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val spoonacularService: SpoonacularService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularService::class.java)
    }

    private val apiKey = "cd1c0340258f4c5bad29f95c40644e2e"

    fun searchRecipes(query: String) {
        spoonacularService.searchRecipes(apiKey, query).enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (response.isSuccessful) {
                    _recipes.value = response.body()?.results ?: emptyList()
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                _error.value = "Failure: ${t.message}"
            }
        })
    }

    fun searchIngredients(query: String) {
        spoonacularService.searchIngredients(apiKey, query).enqueue(object : Callback<IngredientResponse> {
            override fun onResponse(call: Call<IngredientResponse>, response: Response<IngredientResponse>) {
                if (response.isSuccessful) {
                    _ingredients.value = response.body()?.results ?: emptyList()
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<IngredientResponse>, t: Throwable) {
                _error.value = "Failure: ${t.message}"
            }
        })
    }
}
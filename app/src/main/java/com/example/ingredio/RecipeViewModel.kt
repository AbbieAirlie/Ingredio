package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ingredio.data.api.SpoonacularService
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.data.model.IngredientResponse
import com.example.ingredio.data.model.Recipe
import com.example.ingredio.data.model.RecipeResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> get() = _recipes

    private val _savedRecipes = MutableLiveData<List<Recipe>>()
    val savedRecipes: LiveData<List<Recipe>> get() = _savedRecipes

    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> get() = _ingredients

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Simple in-memory cache
    private var lastQuery: String? = null
    private var lastDiet: String? = null
    private var lastIntolerances: String? = null
    private var cachedRecipes: List<Recipe>? = null

    private var lastIngredientQuery: List<String>? = null
    private var lastIngredientDiet: String? = null
    private var lastIngredientIntolerances: String? = null
    private var cachedIngredientRecipes: List<Recipe>? = null

    fun saveRecipe(recipe: Recipe) {
        val userId = auth.currentUser?.uid ?: return
        
        // Optimistic update
        val currentSaved = _savedRecipes.value?.toMutableList() ?: mutableListOf()
        if (currentSaved.none { it.id == recipe.id }) {
            currentSaved.add(recipe)
            _savedRecipes.value = currentSaved
        }

        db.collection("users").document(userId)
            .collection("savedRecipes").document(recipe.id.toString())
            .set(recipe)
            .addOnFailureListener { e ->
                _error.value = "Failed to save recipe: ${e.message}"
                fetchSavedRecipes() // Rollback on failure
            }
    }

    fun fetchSavedRecipes() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("savedRecipes")
            .get()
            .addOnSuccessListener { result ->
                val savedList = result.map { it.toObject(Recipe::class.java) }
                _savedRecipes.value = savedList
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to fetch saved recipes: ${e.message}"
            }
    }

    fun deleteRecipe(recipe: Recipe) {
        val userId = auth.currentUser?.uid ?: return
        
        // Optimistic update
        val currentSaved = _savedRecipes.value?.toMutableList() ?: mutableListOf()
        currentSaved.removeAll { it.id == recipe.id }
        _savedRecipes.value = currentSaved

        db.collection("users").document(userId)
            .collection("savedRecipes").document(recipe.id.toString())
            .delete()
            .addOnFailureListener { e ->
                _error.value = "Failed to delete recipe: ${e.message}"
                fetchSavedRecipes() // Rollback on failure
            }
    }

    private val spoonacularService: SpoonacularService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularService::class.java)
    }

    private val apiKey = "cd1c0340258f4c5bad29f95c40644e2e"

    fun searchRecipes(query: String, diet: String? = null, intolerances: String? = null) {
        // Check cache
        if (query == lastQuery && diet == lastDiet && intolerances == lastIntolerances && cachedRecipes != null) {
            _recipes.value = cachedRecipes
            return
        }

        spoonacularService.searchRecipes(apiKey, query, diet, intolerances).enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    // Update cache
                    lastQuery = query
                    lastDiet = diet
                    lastIntolerances = intolerances
                    cachedRecipes = results
                    _recipes.value = results
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                _error.value = "Failure: ${t.message}"
            }
        })
    }

    fun searchRecipesByIngredients(ingredients: List<String>, diet: String? = null, intolerances: String? = null) {
        // Check cache
        if (ingredients == lastIngredientQuery && diet == lastIngredientDiet && intolerances == lastIngredientIntolerances && cachedIngredientRecipes != null) {
            _recipes.value = cachedIngredientRecipes
            return
        }

        val ingredientQuery = ingredients.joinToString(",")
        spoonacularService.searchRecipesByIngredients(apiKey, ingredientQuery, diet, intolerances).enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    // Update cache
                    lastIngredientQuery = ingredients
                    lastIngredientDiet = diet
                    lastIngredientIntolerances = intolerances
                    cachedIngredientRecipes = results
                    _recipes.value = results
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
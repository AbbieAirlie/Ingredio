package com.example.ingredio

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.ingredio.data.api.SpoonacularService
import com.example.ingredio.data.model.Recipe
import com.example.ingredio.data.model.RecipeResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: RecipeViewModel

    @MockK
    private lateinit var db: FirebaseFirestore

    @MockK
    private lateinit var auth: FirebaseAuth

    @MockK
    private lateinit var spoonacularService: SpoonacularService

    @MockK
    private lateinit var recipeObserver: Observer<List<Recipe>>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = RecipeViewModel(db, auth, spoonacularService)
        viewModel.recipes.observeForever(recipeObserver)
        every { recipeObserver.onChanged(any()) } just Runs
    }

    @Test
    fun `searchRecipes updates recipes LiveData on success`() {
        // Given
        val query = "pasta"
        val recipes = listOf(Recipe(id = 1, title = "Pasta"))
        val responseBody = RecipeResponse(results = recipes)
        val call = mockk<Call<RecipeResponse>>()
        
        every { spoonacularService.searchRecipes(any(), query, null, null) } returns call
        
        // Mock the enqueue behavior
        every { call.enqueue(any()) } answers {
            val callback = it.invocation.args[0] as Callback<RecipeResponse>
            callback.onResponse(call, Response.success(responseBody))
        }

        // When
        viewModel.searchRecipes(query)

        // Then
        verify { recipeObserver.onChanged(recipes) }
    }

    @Test
    fun `searchRecipes uses cache for same query`() {
        // Given
        val query = "pasta"
        val recipes = listOf(Recipe(id = 1, title = "Pasta"))
        val responseBody = RecipeResponse(results = recipes)
        val call = mockk<Call<RecipeResponse>>()
        
        every { spoonacularService.searchRecipes(any(), query, null, null) } returns call
        every { call.enqueue(any()) } answers {
            val callback = it.invocation.args[0] as Callback<RecipeResponse>
            callback.onResponse(call, Response.success(responseBody))
        }

        // When
        viewModel.searchRecipes(query) // First call
        viewModel.searchRecipes(query) // Second call (should be cached)

        // Then
        // Should only be called once by service, but twice by observer (once for first success, once for cache)
        verify(exactly = 1) { spoonacularService.searchRecipes(any(), query, null, null) }
        verify(exactly = 2) { recipeObserver.onChanged(recipes) }
    }
}

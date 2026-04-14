package com.example.ingredio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.databinding.BrowseRecipesBinding

class BrowseRecipesFragment : Fragment() {

    private var _binding: BrowseRecipesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recipeAdapter: RecipeAdapter
    private val viewModel: RecipeViewModel by viewModels()
    private val cupboardViewModel: CupboardViewModel by viewModels()
    private val shoppingViewModel: ShoppingListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BrowseRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupFilters()

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Fetch ingredients to get recommendations
        cupboardViewModel.fetchUserIngredients()
        viewModel.fetchSavedRecipes()
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, _ ->
            performSearch()
        }
    }

    private fun performSearch() {
        val query = binding.editTextSearch.text.toString()
        val diet = getSelectedDiet()
        val intolerances = getSelectedIntolerances()

        if (query.isNotEmpty()) {
            binding.textViewRecommendationStatus.visibility = View.GONE
            viewModel.searchRecipes(query, diet, intolerances)
        } else {
            val ingredients = cupboardViewModel.userIngredients.value?.map { it.name } ?: emptyList()
            if (ingredients.isNotEmpty()) {
                binding.textViewRecommendationStatus.visibility = View.VISIBLE
                viewModel.searchRecipesByIngredients(ingredients, diet, intolerances)
            }
        }
    }

    private fun getSelectedDiet(): String? {
        val diets = mutableListOf<String>()
        if (binding.chipVegetarian.isChecked) diets.add("vegetarian")
        if (binding.chipVegan.isChecked) diets.add("vegan")
        return if (diets.isNotEmpty()) diets.joinToString(",") else null
    }

    private fun getSelectedIntolerances(): String? {
        val intolerances = mutableListOf<String>()
        if (binding.chipGlutenFree.isChecked) intolerances.add("gluten")
        if (binding.chipDairyFree.isChecked) intolerances.add("dairy")
        if (binding.chipNutFree.isChecked) {
            intolerances.add("peanut")
            intolerances.add("tree nut")
        }
        return if (intolerances.isNotEmpty()) intolerances.joinToString(",") else null
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            recipes = emptyList(),
            onRecipeClick = { recipe ->
                recipe.sourceUrl?.let { url ->
                    val bundle = Bundle().apply {
                        putString("url", url)
                    }
                    findNavController().navigate(R.id.action_browseRecipesFragment_to_recipeDetailFragment, bundle)
                } ?: run {
                    Toast.makeText(context, getString(R.string.no_url_available), Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRecipe = { recipe ->
                val isCurrentlySaved = viewModel.savedRecipes.value?.any { it.id == recipe.id } == true
                if (isCurrentlySaved) {
                    viewModel.deleteRecipe(recipe)
                    Toast.makeText(context, "Recipe removed", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveRecipe(recipe)
                    Toast.makeText(context, "Recipe saved!", Toast.LENGTH_SHORT).show()
                }
            },
            onAddAllToShoppingList = { recipe ->
                recipe.extendedIngredients?.forEach { ingredient ->
                    shoppingViewModel.addIngredientToShoppingList(ingredient)
                }
                Toast.makeText(context, "Added ingredients to shopping list", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.updateRecipes(recipes)
        }

        viewModel.savedRecipes.observe(viewLifecycleOwner) { savedRecipes ->
            val savedIds = savedRecipes.map { it.id }.toSet()
            recipeAdapter.updateSavedIds(savedIds)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        cupboardViewModel.userIngredients.observe(viewLifecycleOwner) { ingredients ->
            if (ingredients.isNotEmpty()) {
                if (binding.editTextSearch.text.isEmpty()) {
                    binding.textViewRecommendationStatus.visibility = View.VISIBLE
                    performSearch()
                }
            } else {
                binding.textViewRecommendationStatus.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
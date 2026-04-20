package com.example.ingredio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.databinding.FragmentSavedRecipesBinding

class SavedRecipesFragment : Fragment() {

    private var _binding: FragmentSavedRecipesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recipeAdapter: RecipeAdapter
    private val viewModel: RecipeViewModel by viewModels()
    private val shoppingViewModel: ShoppingListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.fetchSavedRecipes()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            recipes = emptyList(),
            onRecipeClick = { recipe ->
                recipe.sourceUrl?.let { url ->
                    val bundle = Bundle().apply {
                        putString("url", url)
                    }
                    findNavController().navigate(R.id.action_savedRecipesFragment_to_recipeDetailFragment, bundle)
                } ?: run {
                    Toast.makeText(context, getString(R.string.no_url_available), Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRecipe = { recipe ->
                viewModel.deleteRecipe(recipe)
                Toast.makeText(context, getString(R.string.recipe_removed), Toast.LENGTH_SHORT).show()
            },
            onAddAllToShoppingList = { recipe ->
                recipe.extendedIngredients?.forEach { ingredient ->
                    shoppingViewModel.addIngredientToShoppingList(ingredient)
                }
                Toast.makeText(context, getString(R.string.recipe_added_to_shopping), Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewSavedRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.savedRecipes.observe(viewLifecycleOwner) { recipes ->
            if (recipes.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewSavedRecipes.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewSavedRecipes.visibility = View.VISIBLE
                
                val savedIds = recipes.map { it.id }.toSet()
                recipeAdapter.updateSavedIds(savedIds)
                recipeAdapter.updateRecipes(recipes)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
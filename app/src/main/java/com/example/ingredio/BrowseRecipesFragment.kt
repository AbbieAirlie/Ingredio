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

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearch.text.toString()
                if (query.isNotEmpty()) {
                    viewModel.searchRecipes(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(emptyList()) { recipe ->
            recipe.sourceUrl?.let { url ->
                val bundle = Bundle().apply {
                    putString("url", url)
                }
                findNavController().navigate(R.id.action_browseRecipesFragment_to_recipeDetailFragment, bundle)
            } ?: run {
                Toast.makeText(context, "No URL available for this recipe", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerViewRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.updateRecipes(recipes)
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
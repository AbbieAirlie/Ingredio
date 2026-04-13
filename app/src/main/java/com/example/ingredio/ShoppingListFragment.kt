package com.example.ingredio

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
import com.example.ingredio.databinding.MenuShoppingListBinding

class ShoppingListFragment : Fragment() {

    private var _binding: MenuShoppingListBinding? = null
    private val binding get() = _binding!!

    private val shoppingViewModel: ShoppingListViewModel by viewModels()
    private val searchViewModel: RecipeViewModel by viewModels()

    private lateinit var shoppingAdapter: IngredientAdapter
    private lateinit var searchAdapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModels()

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Search logic
        binding.editTextSearchShopping.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearchShopping.text.toString()
                if (query.isNotEmpty()) {
                    searchViewModel.searchIngredients(query)
                }
                true
            } else {
                false
            }
        }

        shoppingViewModel.fetchShoppingList()
    }

    private fun setupRecyclerViews() {
        // Shopping List
        shoppingAdapter = IngredientAdapter(emptyList(), getString(R.string.remove_button)) { ingredient ->
            shoppingViewModel.removeIngredientFromShoppingList(ingredient.id)
        }
        binding.recyclerViewShoppingList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shoppingAdapter
        }

        // Search Results List
        searchAdapter = IngredientAdapter(emptyList(), getString(R.string.add_button)) { ingredient ->
            shoppingViewModel.addIngredientToShoppingList(ingredient)
            binding.editTextSearchShopping.text.clear()
            binding.recyclerViewSearchResults.visibility = View.GONE
        }
        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun observeViewModels() {
        shoppingViewModel.shoppingListItems.observe(viewLifecycleOwner) { items ->
            shoppingAdapter.updateIngredients(items)
            binding.textviewEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        searchViewModel.ingredients.observe(viewLifecycleOwner) { searchResults ->
            if (searchResults.isNotEmpty()) {
                searchAdapter.updateIngredients(searchResults)
                binding.recyclerViewSearchResults.visibility = View.VISIBLE
            } else {
                binding.recyclerViewSearchResults.visibility = View.GONE
            }
        }

        shoppingViewModel.status.observe(viewLifecycleOwner) { status ->
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
import com.example.ingredio.databinding.AddItemsBinding

class AddItemsFragment : Fragment() {

    private var _binding: AddItemsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by viewModels()
    private val cupboardViewModel: CupboardViewModel by viewModels()
    private lateinit var ingredientAdapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.editTextSearchIngredients.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearchIngredients.text.toString()
                if (query.isNotEmpty()) {
                    viewModel.searchIngredients(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientAdapter(
            ingredients = emptyList(),
            button1Text = getString(R.string.add_button),
            onButton1Click = { ingredient ->
                cupboardViewModel.addIngredientToCupboard(ingredient)
            }
        )
        binding.recyclerViewIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.ingredients.observe(viewLifecycleOwner) { ingredients ->
            ingredientAdapter.updateIngredients(ingredients)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        cupboardViewModel.status.observe(viewLifecycleOwner) { statusMessage ->
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
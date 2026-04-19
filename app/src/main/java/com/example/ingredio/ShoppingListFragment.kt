package com.example.ingredio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.databinding.MenuShoppingListBinding

class ShoppingListFragment : Fragment() {

    private var _binding: MenuShoppingListBinding? = null
    private val binding get() = _binding!!

    private val shoppingViewModel: ShoppingListViewModel by viewModels()
    private val searchViewModel: RecipeViewModel by viewModels()

    private val cupboardViewModel: CupboardViewModel by viewModels()

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
        shoppingAdapter = IngredientAdapter(
            ingredients = emptyList(),
            mainButtonText = getString(R.string.remove_button),
            secondaryButtonText = getString(R.string.move_to_cupboard),
            onMainButtonClick = { ingredient ->
                shoppingViewModel.removeIngredientFromShoppingList(ingredient.id)
            },
            onSecondaryButtonClick = { ingredient ->
                showMoveToCupboardDialog(ingredient)
            }
        )
        binding.recyclerViewShoppingList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shoppingAdapter
        }

        // Search Results List
        searchAdapter = IngredientAdapter(
            ingredients = emptyList(),
            mainButtonText = getString(R.string.add_button),
            onMainButtonClick = { ingredient ->
                shoppingViewModel.addIngredientToShoppingList(ingredient)
                binding.editTextSearchShopping.text.clear()
                binding.recyclerViewSearchResults.visibility = View.GONE
            }
        )
        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun showMoveToCupboardDialog(ingredient: Ingredient) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_ingredient_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val btnPickDate = dialogView.findViewById<android.widget.Button>(R.id.button_pick_date)
        val rgExpiryType = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroup_expiry_type)
        val spinnerStorage = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_storage_type)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.button_cancel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.button_confirm_add)

        btnConfirm.text = getString(R.string.add_to_cupboard_button)

        var selectedTimestamp: Long? = null

        btnPickDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedTimestamp = calendar.timeInMillis
                btnPickDate.text = getString(R.string.date_format_display, day, month + 1, year)
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            if (selectedTimestamp == null) {
                Toast.makeText(context, getString(R.string.please_select_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expiryType = if (rgExpiryType.checkedRadioButtonId == R.id.radioButton_use_by) getString(R.string.use_by) else getString(R.string.best_before)
            val storageType = spinnerStorage.selectedItem.toString()

            cupboardViewModel.addIngredientWithDetails(ingredient, selectedTimestamp!!, expiryType, storageType)
            shoppingViewModel.removeIngredientFromShoppingList(ingredient.id)
            dialog.dismiss()
        }

        dialog.show()
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
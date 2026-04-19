package com.example.ingredio

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.databinding.MenuCupboardBinding
import java.util.Calendar

class menuCupboard : Fragment() {

    private var _binding: MenuCupboardBinding? = null
    private val binding get() = _binding!!

    private val cupboardViewModel: CupboardViewModel by viewModels()
    private val searchViewModel: RecipeViewModel by viewModels()

    private val shoppingViewModel: ShoppingListViewModel by viewModels()

    private lateinit var cupboardAdapter: IngredientAdapter
    private lateinit var searchAdapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuCupboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModels()

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        // Search logic
        binding.editTextSearchIngredients.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearchIngredients.text.toString()
                if (query.isNotEmpty()) {
                    searchViewModel.searchIngredients(query)
                }
                true
            } else {
                false
            }
        }

        cupboardViewModel.fetchUserIngredients()
    }

    private fun setupRecyclerViews() {
        // Cupboard List
        cupboardAdapter = IngredientAdapter(
            ingredients = emptyList(),
            mainButtonText = getString(R.string.move_to_shopping_list),
            onMainButtonClick = { ingredient ->
                shoppingViewModel.addIngredientToShoppingList(ingredient)
                cupboardViewModel.removeIngredientFromCupboard(ingredient)
            },
            onEditClick = { ingredient ->
                showEditDetailsDialog(ingredient)
            },
            onDeleteClick = { ingredient ->
                cupboardViewModel.removeIngredientFromCupboard(ingredient)
            }
        )
        binding.recyclerViewCupboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cupboardAdapter
        }

        // Search Results List
        searchAdapter = IngredientAdapter(
            ingredients = emptyList(),
            mainButtonText = getString(R.string.add_button),
            onMainButtonClick = { ingredient ->
                showAddDetailsDialog(ingredient)
            }
        )
        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun showEditDetailsDialog(ingredient: Ingredient) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_ingredient_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val btnPickDate = dialogView.findViewById<Button>(R.id.button_pick_date)
        val rgExpiryType = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroup_expiry_type)
        val rbUseBy = dialogView.findViewById<RadioButton>(R.id.radioButton_use_by)
        val rbBestBefore = dialogView.findViewById<RadioButton>(R.id.radioButton_best_before)
        val spinnerStorage = dialogView.findViewById<Spinner>(R.id.spinner_storage_type)
        val btnCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.button_confirm_add)

        btnConfirm.text = getString(R.string.update_button)
        
        var selectedTimestamp: Long? = ingredient.expiryDate

        // Pre-fill existing data
        if (ingredient.expiryDate != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = ingredient.expiryDate
            btnPickDate.text = getString(R.string.date_format_display, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
        }

        if (ingredient.expiryType == getString(R.string.use_by)) {
            rbUseBy.isChecked = true
        } else if (ingredient.expiryType == getString(R.string.best_before)) {
            rbBestBefore.isChecked = true
        }

        val storageOptions = resources.getStringArray(R.array.storage_types)
        val storageIndex = storageOptions.indexOf(ingredient.storageType)
        if (storageIndex >= 0) {
            spinnerStorage.setSelection(storageIndex)
        }

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (selectedTimestamp != null) calendar.timeInMillis = selectedTimestamp!!
            
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedTimestamp = calendar.timeInMillis
                btnPickDate.text = getString(R.string.date_format_display, day, month + 1, year)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            if (selectedTimestamp == null) {
                Toast.makeText(context, getString(R.string.please_select_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expiryType = if (rgExpiryType.checkedRadioButtonId == R.id.radioButton_use_by) getString(R.string.use_by) else getString(R.string.best_before)
            val storageType = spinnerStorage.selectedItem.toString()

            val updatedIngredient = ingredient.copy(
                expiryDate = selectedTimestamp!!,
                expiryType = expiryType,
                storageType = storageType
            )
            cupboardViewModel.addIngredientWithDetails(updatedIngredient, selectedTimestamp!!, expiryType, storageType)
            NotificationUtils.scheduleExpirationNotification(requireContext(), updatedIngredient)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddDetailsDialog(ingredient: Ingredient) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_ingredient_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val btnPickDate = dialogView.findViewById<Button>(R.id.button_pick_date)
        val rgExpiryType = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroup_expiry_type)
        val spinnerStorage = dialogView.findViewById<Spinner>(R.id.spinner_storage_type)
        val btnCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.button_confirm_add)

        var selectedTimestamp: Long? = null

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedTimestamp = calendar.timeInMillis
                btnPickDate.text = getString(R.string.date_format_display, day, month + 1, year)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            if (selectedTimestamp == null) {
                Toast.makeText(context, getString(R.string.please_select_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expiryType = if (rgExpiryType.checkedRadioButtonId == R.id.radioButton_use_by) getString(R.string.use_by) else getString(R.string.best_before)
            val storageType = spinnerStorage.selectedItem.toString()

            val updatedIngredient = ingredient.copy(
                expiryDate = selectedTimestamp!!,
                expiryType = expiryType,
                storageType = storageType
            )
            cupboardViewModel.addIngredientWithDetails(updatedIngredient, selectedTimestamp!!, expiryType, storageType)
            NotificationUtils.scheduleExpirationNotification(requireContext(), updatedIngredient)
            
            binding.editTextSearchIngredients.text.clear()
            binding.recyclerViewSearchResults.visibility = View.GONE
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModels() {
        // Cupboard Data
        cupboardViewModel.userIngredients.observe(viewLifecycleOwner) { ingredients ->
            cupboardAdapter.updateIngredients(ingredients)
            binding.textviewSecond.visibility = if (ingredients.isEmpty()) View.VISIBLE else View.GONE
        }

        // Search Results Data
        searchViewModel.ingredients.observe(viewLifecycleOwner) { searchResults ->
            if (searchResults.isNotEmpty()) {
                searchAdapter.updateIngredients(searchResults)
                binding.recyclerViewSearchResults.visibility = View.VISIBLE
            } else {
                binding.recyclerViewSearchResults.visibility = View.GONE
            }
        }

        cupboardViewModel.status.observe(viewLifecycleOwner) { status ->
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
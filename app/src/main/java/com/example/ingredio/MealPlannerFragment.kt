package com.example.ingredio

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.data.model.MealPlan
import com.example.ingredio.databinding.FragmentMealPlannerBinding
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.*

class MealPlannerFragment : Fragment() {

    private var _binding: FragmentMealPlannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MealPlannerViewModel by viewModels()
    private val recipeViewModel: RecipeViewModel by viewModels()
    private lateinit var mealAdapter: MealPlanAdapter
    
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDateStr: String = dateFormatter.format(Date())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        observeViewModel()

        binding.fabAddMeal.setOnClickListener {
            showAddMealDialog()
        }

        recipeViewModel.fetchSavedRecipes()
        viewModel.fetchAllMealPlans()
        viewModel.fetchMealPlans(selectedDateStr)
    }

    private fun setupRecyclerView() {
        mealAdapter = MealPlanAdapter(
            onDeleteClick = { mealPlan ->
                viewModel.deleteMealPlan(mealPlan)
            }
        )
        binding.recyclerViewMeals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mealAdapter
        }
    }

    private fun setupCalendar() {
        binding.calendarView.setSelectedDate(CalendarDay.today())
        binding.calendarView.setOnDateChangedListener { _, day, _ ->
            val calendar = Calendar.getInstance()
            calendar.set(day.year, day.month - 1, day.day)
            selectedDateStr = dateFormatter.format(calendar.time)
            binding.textViewSelectedDate.text = getString(R.string.selected_date_format, selectedDateStr)
            viewModel.fetchMealPlans(selectedDateStr)
        }
        binding.textViewSelectedDate.text = getString(R.string.selected_date_format, selectedDateStr)
    }

    private fun observeViewModel() {
        viewModel.mealPlans.observe(viewLifecycleOwner) { meals ->
            mealAdapter.submitList(meals)
        }

        viewModel.allMealPlans.observe(viewLifecycleOwner) { allMeals ->
            updateCalendarDecorators(allMeals)
        }
    }

    private fun updateCalendarDecorators(allMeals: List<MealPlan>) {
        binding.calendarView.removeDecorators()
        
        val dayToMeals = mutableMapOf<CalendarDay, MutableList<MealPlan>>()

        for (meal in allMeals) {
            try {
                val date = dateFormatter.parse(meal.date)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    val day = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                    
                    dayToMeals.getOrPut(day) { mutableListOf() }.add(meal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val mealTypeOrder = listOf("breakfast", "lunch", "dinner", "snack")

        // Add a MultiEventDecorator for each day that has meals
        for ((day, meals) in dayToMeals) {
            val sortedColors = meals
                .sortedBy { meal ->
                    val index = mealTypeOrder.indexOf(meal.mealType.lowercase())
                    if (index == -1) 99 else index
                }
                .map { meal ->
                    when (meal.mealType.lowercase()) {
                        "breakfast" -> Color.BLUE
                        "lunch" -> Color.GREEN
                        "dinner" -> Color.RED
                        else -> Color.GRAY
                    }
                }
            
            binding.calendarView.addDecorator(MultiEventDecorator(day, sortedColors))
        }
    }

    private fun showAddMealDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_meal, null)
        val editTextCustom = dialogView.findViewById<EditText>(R.id.editTextCustomMeal)
        val spinnerRecipes = dialogView.findViewById<Spinner>(R.id.spinnerRecipes)
        val spinnerMealType = dialogView.findViewById<Spinner>(R.id.spinnerMealType)

        val mealTypes = arrayOf(
            getString(R.string.meal_type_breakfast),
            getString(R.string.meal_type_lunch),
            getString(R.string.meal_type_dinner),
            getString(R.string.meal_type_snack)
        )
        spinnerMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, mealTypes)

        val savedRecipes = recipeViewModel.savedRecipes.value ?: emptyList()
        val recipeTitles = mutableListOf(getString(R.string.custom_meal_none))
        recipeTitles.addAll(savedRecipes.map { it.title })
        spinnerRecipes.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, recipeTitles)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_meal_plan)
            .setView(dialogView)
            .setPositiveButton(R.string.save_meal) { _, _ ->
                val mealType = spinnerMealType.selectedItem.toString()
                val recipeIndex = spinnerRecipes.selectedItemId.toInt()
                
                val mealPlan = if (recipeIndex == 0) {
                    val customText = editTextCustom.text.toString()
                    MealPlan(date = selectedDateStr, customText = customText, mealType = mealType)
                } else {
                    val recipe = savedRecipes[recipeIndex - 1]
                    MealPlan(
                        date = selectedDateStr,
                        recipeId = recipe.id,
                        recipeTitle = recipe.title,
                        recipeImage = recipe.image,
                        mealType = mealType
                    )
                }
                viewModel.addMealPlan(mealPlan)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

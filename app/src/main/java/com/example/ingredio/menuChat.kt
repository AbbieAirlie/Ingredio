package com.example.ingredio

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ingredio.data.model.Ingredient
import androidx.lifecycle.ViewModelProvider

import com.example.ingredio.data.model.Recipe
import com.example.ingredio.RecipeDetailFragment
import com.example.ingredio.RecipeViewModel

class menuChat : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private val recipeViewModel: RecipeViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    private val cupboardViewModel: CupboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Chat"
            setDisplayHomeAsUpEnabled(true)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewChat)
        val editText = findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = findViewById<ImageButton>(R.id.buttonSend)

        adapter = ChatAdapter(
            messages = mutableListOf(),
            onRecipeClick = { recipe ->
                recipe.sourceUrl?.let { url ->
                    val fragment = RecipeDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString("url", url)
                        }
                    }
                    supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onSaveRecipe = { recipe ->
                val savedRecipes = recipeViewModel.savedRecipes.value ?: emptyList()
                if (savedRecipes.any { it.id == recipe.id }) {
                    recipeViewModel.deleteRecipe(recipe)
                    android.widget.Toast.makeText(this, "Recipe removed from saved!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    recipeViewModel.saveRecipe(recipe)
                    android.widget.Toast.makeText(this, "Recipe saved!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            recyclerView.scrollToPosition(messages.size - 1)
        }

        recipeViewModel.savedRecipes.observe(this) { saved ->
            val savedIds = saved.map { it.id }.toSet()
            adapter.updateSavedRecipeIds(savedIds)
        }

        recipeViewModel.fetchSavedRecipes()

        viewModel.showExpiryDialog.observe(this) { ingredient ->
            if (ingredient != null) {
                showAddDetailsDialog(ingredient)
                viewModel.onDialogShown()
            }
        }

        cupboardViewModel.status.observe(this) { status ->
            if (!status.isNullOrEmpty()) {
                android.widget.Toast.makeText(this, status, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        buttonSend.setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                editText.text.clear()
            }
        }
    }

    private fun showAddDetailsDialog(ingredient: Ingredient) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ingredient_details, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val btnPickDate = dialogView.findViewById<android.widget.Button>(R.id.button_pick_date)
        val rgExpiryType = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroup_expiry_type)
        val spinnerStorage = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_storage_type)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.button_cancel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.button_confirm_add)

        var selectedTimestamp: Long? = null

        btnPickDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedTimestamp = calendar.timeInMillis
                btnPickDate.text = "$day/${month + 1}/$year"
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            if (selectedTimestamp == null) {
                android.widget.Toast.makeText(this, getString(R.string.please_select_date), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expiryType = if (rgExpiryType.checkedRadioButtonId == R.id.radioButton_use_by) getString(R.string.use_by) else getString(R.string.best_before)
            val storageType = spinnerStorage.selectedItem.toString()

            cupboardViewModel.addIngredientWithDetails(ingredient, selectedTimestamp!!, expiryType, storageType)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_chat -> {
                viewModel.clearChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

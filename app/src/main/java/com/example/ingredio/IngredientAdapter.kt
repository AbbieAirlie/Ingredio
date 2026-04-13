package com.example.ingredio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.databinding.ItemIngredientBinding

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IngredientAdapter(
    private var ingredients: List<Ingredient>,
    private val buttonText: String = "Add",
    private val onButtonClick: (Ingredient) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.binding.textViewIngredientName.text = ingredient.name
        holder.binding.buttonAddIngredient.text = buttonText
        
        Glide.with(holder.itemView.context)
            .load(ingredient.imageUrl)
            .into(holder.binding.imageViewIngredient)

        // Show expiry and storage info if available
        if (ingredient.expiryDate != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(ingredient.expiryDate))
            holder.binding.textViewIngredientDetails.text = "${ingredient.expiryType}: $dateStr (${ingredient.storageType})"
            holder.binding.textViewIngredientDetails.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.textViewIngredientDetails.visibility = android.view.View.GONE
        }

        holder.binding.buttonAddIngredient.setOnClickListener {
            onButtonClick(ingredient)
        }
    }

    override fun getItemCount(): Int = ingredients.size

    fun updateIngredients(newIngredients: List<Ingredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }
}
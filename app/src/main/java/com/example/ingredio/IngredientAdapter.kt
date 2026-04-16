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
    private val mainButtonText: String = "Add",
    private val secondaryButtonText: String? = null,
    private val onMainButtonClick: (Ingredient) -> Unit,
    private val onSecondaryButtonClick: ((Ingredient) -> Unit)? = null,
    private val onEditClick: ((Ingredient) -> Unit)? = null,
    private val onDeleteClick: ((Ingredient) -> Unit)? = null
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
        
        // Main Action Button (Add or Move)
        holder.binding.buttonAction1.text = mainButtonText
        holder.binding.buttonAction1.setOnClickListener { onMainButtonClick(ingredient) }

        // Secondary Action Button (e.g., Move to Shopping List)
        if (secondaryButtonText != null && onSecondaryButtonClick != null) {
            holder.binding.buttonAction2.text = secondaryButtonText
            holder.binding.buttonAction2.visibility = android.view.View.VISIBLE
            holder.binding.buttonAction2.setOnClickListener { onSecondaryButtonClick.invoke(ingredient) }
        } else {
            holder.binding.buttonAction2.visibility = android.view.View.GONE
        }

        // Edit Icon
        if (onEditClick != null) {
            holder.binding.buttonEdit.visibility = android.view.View.VISIBLE
            holder.binding.buttonEdit.setOnClickListener { onEditClick.invoke(ingredient) }
        } else {
            holder.binding.buttonEdit.visibility = android.view.View.GONE
        }

        // Delete Icon
        if (onDeleteClick != null) {
            holder.binding.buttonDelete.visibility = android.view.View.VISIBLE
            holder.binding.buttonDelete.setOnClickListener { onDeleteClick.invoke(ingredient) }
        } else {
            holder.binding.buttonDelete.visibility = android.view.View.GONE
        }

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
    }

    override fun getItemCount(): Int = ingredients.size

    fun updateIngredients(newIngredients: List<Ingredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }
}
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
    private val button1Text: String = "Add",
    private val button2Text: String? = null,
    private val onButton1Click: (Ingredient) -> Unit,
    private val onButton2Click: ((Ingredient) -> Unit)? = null
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
        holder.binding.buttonAction1.text = button1Text
        
        if (button2Text != null && onButton2Click != null) {
            holder.binding.buttonAction2.text = button2Text
            holder.binding.buttonAction2.visibility = android.view.View.VISIBLE
            holder.binding.buttonAction2.setOnClickListener { onButton2Click.invoke(ingredient) }
        } else {
            holder.binding.buttonAction2.visibility = android.view.View.GONE
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

        holder.binding.buttonAction1.setOnClickListener {
            onButton1Click(ingredient)
        }
    }

    override fun getItemCount(): Int = ingredients.size

    fun updateIngredients(newIngredients: List<Ingredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }
}
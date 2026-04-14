package com.example.ingredio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ingredio.data.model.MealPlan
import com.example.ingredio.databinding.ItemMealPlanBinding

class MealPlanAdapter(
    private val onDeleteClick: (MealPlan) -> Unit
) : ListAdapter<MealPlan, MealPlanAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealViewHolder(private val binding: ItemMealPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mealPlan: MealPlan) {
            binding.textViewMealType.text = mealPlan.mealType.uppercase()
            
            if (mealPlan.recipeId != null) {
                binding.textViewMealTitle.text = mealPlan.recipeTitle
                Glide.with(binding.imageViewMeal.context)
                    .load(mealPlan.recipeImage)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.imageViewMeal)
            } else {
                binding.textViewMealTitle.text = mealPlan.customText
                binding.imageViewMeal.setImageResource(android.R.drawable.ic_menu_edit)
            }

            binding.buttonDeleteMeal.setOnClickListener {
                onDeleteClick(mealPlan)
            }
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<MealPlan>() {
        override fun areItemsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem == newItem
        }
    }
}

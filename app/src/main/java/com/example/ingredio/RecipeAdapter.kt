package com.example.ingredio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ingredio.data.model.Recipe

class RecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit,
    private val onSaveRecipe: ((Recipe) -> Unit)? = null,
    private val onAddAllToShoppingList: ((Recipe) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var savedRecipeIds: Set<Int> = emptySet()

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeImage: ImageView = view.findViewById(R.id.recipeImage)
        val recipeTitle: TextView = view.findViewById(R.id.recipeTitle)
        val btnSave: ImageView = view.findViewById(R.id.button_save_recipe)
        val btnAddAll: View = view.findViewById(R.id.button_add_all_shopping)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.recipeTitle.text = recipe.title
        Glide.with(holder.itemView.context)
            .load(recipe.image)
            .into(holder.recipeImage)

        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }

        if (onSaveRecipe != null) {
            holder.btnSave.visibility = View.VISIBLE
            
            val isSaved = savedRecipeIds.contains(recipe.id)
            if (isSaved) {
                holder.btnSave.setImageResource(R.drawable.ic_check_circle)
            } else {
                holder.btnSave.setImageResource(R.drawable.ic_empty_box)
            }

            holder.btnSave.setOnClickListener {
                val newSavedIds = savedRecipeIds.toMutableSet()
                if (isSaved) {
                    newSavedIds.remove(recipe.id)
                    holder.btnSave.setImageResource(R.drawable.ic_empty_box)
                } else {
                    newSavedIds.add(recipe.id)
                    holder.btnSave.setImageResource(R.drawable.ic_check_circle)
                }
                savedRecipeIds = newSavedIds
                onSaveRecipe.invoke(recipe)
            }
        } else {
            holder.btnSave.visibility = View.GONE
        }

        if (onAddAllToShoppingList != null) {
            holder.btnAddAll.visibility = View.VISIBLE
            holder.btnAddAll.setOnClickListener {
                onAddAllToShoppingList.invoke(recipe)
            }
        } else {
            holder.btnAddAll.visibility = View.GONE
        }
    }

    override fun getItemCount() = recipes.size

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    fun updateSavedIds(newSavedIds: Set<Int>) {
        savedRecipeIds = newSavedIds
        notifyDataSetChanged()
    }
}
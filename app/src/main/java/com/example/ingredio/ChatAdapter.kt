package com.example.ingredio

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.ingredio.data.model.ChatMessage
import com.example.ingredio.data.model.Recipe

class ChatAdapter(
    private var messages: List<ChatMessage>,
    private val onRecipeClick: (Recipe) -> Unit,
    private val onSaveRecipe: (Recipe) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val cardMessage: CardView = view.findViewById(R.id.cardMessage)
        val layout: LinearLayout = view as LinearLayout
        val recyclerRecipes: RecyclerView = view.findViewById(R.id.recyclerRecipes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.textMessage.text = message.content
        
        val params = holder.cardMessage.layoutParams as LinearLayout.LayoutParams
        if (message.fromUser) {
            holder.layout.gravity = Gravity.END
            holder.cardMessage.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.ingredio_green))
            params.marginStart = 100
            params.marginEnd = 0
            holder.recyclerRecipes.visibility = View.GONE
        } else {
            holder.layout.gravity = Gravity.START
            holder.cardMessage.setCardBackgroundColor(android.graphics.Color.WHITE)
            params.marginStart = 0
            params.marginEnd = 100
            
            if (message.recipes != null && message.recipes.isNotEmpty()) {
                holder.recyclerRecipes.visibility = View.VISIBLE
                holder.recyclerRecipes.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.context, RecyclerView.HORIZONTAL, false)
                // Use a smaller card width for the horizontal chat scroll
                val recipeAdapter = RecipeAdapter(message.recipes, onRecipeClick, onSaveRecipe)
                holder.recyclerRecipes.adapter = recipeAdapter
                
                // Set the recycler height to accommodate the cards
                val density = holder.itemView.context.resources.displayMetrics.density
                holder.recyclerRecipes.layoutParams.height = (280 * density).toInt() 
            } else {
                holder.recyclerRecipes.visibility = View.GONE
                holder.recyclerRecipes.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        holder.cardMessage.layoutParams = params
    }

    override fun getItemCount() = messages.size
}

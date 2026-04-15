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

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val cardMessage: CardView = view.findViewById(R.id.cardMessage)
        val layout: LinearLayout = view as LinearLayout
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
        if (message.isUser) {
            holder.layout.gravity = Gravity.END
            holder.cardMessage.setCardBackgroundColor(0xFFDCF8C6.toInt()) // Light green
            params.marginStart = 100
            params.marginEnd = 0
        } else {
            holder.layout.gravity = Gravity.START
            holder.cardMessage.setCardBackgroundColor(0xFFFFFFFF.toInt()) // White
            params.marginStart = 0
            params.marginEnd = 100
        }
        holder.cardMessage.layoutParams = params
    }

    override fun getItemCount() = messages.size
}

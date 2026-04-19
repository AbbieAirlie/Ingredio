package com.example.ingredio.data.model

data class ChatMessage(
    val content: String = "",
    val fromUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val recipes: List<Recipe>? = null
)

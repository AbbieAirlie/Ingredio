package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ingredio.data.model.ChatMessage
import com.example.ingredio.data.model.Ingredient
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val messages: LiveData<MutableList<ChatMessage>> get() = _messages

    private val systemInstructionText = "You are a helpful kitchen assistant. You can manage the user's cupboard and suggest recipes. " +
            "If the user wants to add an ingredient, you MUST include a special tag in your response: [ADD_INGREDIENT:Name,ID]. " +
            "If the user wants to remove an ingredient, use [REMOVE_INGREDIENT:ID]. " +
            "If the user wants to search for recipes, use [SEARCH_RECIPE:Query]. " +
            "Always respond politely."

    // Initialize Vertex AI for Firebase (Using Gemini 2.5 Flash)
    private val model = Firebase.vertexAI.generativeModel(
        modelName = "gemini-2.5-flash",
        systemInstruction = content { text(systemInstructionText) }
    )

    // Maintain a single chat session for context/memory
    private val chat = model.startChat()

    private val cupboardViewModel = CupboardViewModel()
    private val recipeViewModel = RecipeViewModel()

    fun sendMessage(text: String) {
        val currentMessages = _messages.value ?: mutableListOf()
        currentMessages.add(ChatMessage(text, true))
        _messages.value = currentMessages

        val aiResponseIndex = currentMessages.size
        currentMessages.add(ChatMessage("", false))
        _messages.value = currentMessages

        streamAiResponse(text, aiResponseIndex)
    }

    private fun streamAiResponse(userMessage: String, responseIndex: Int) {
        viewModelScope.launch {
            try {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val content = chunk.text ?: ""
                    appendAiMessage(responseIndex, content)
                }
                
                val fullMessage = _messages.value?.get(responseIndex)?.content ?: ""
                processCustomTags(fullMessage, responseIndex)
                
            } catch (e: Exception) {
                updateAiMessage(responseIndex, "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun processCustomTags(content: String, responseIndex: Int) {
        val addRegex = "\\[ADD_INGREDIENT:(.+),(\\d+)]".toRegex()
        val removeRegex = "\\[REMOVE_INGREDIENT:(\\d+)]".toRegex()
        val searchRegex = "\\[SEARCH_RECIPE:(.+)]".toRegex()

        addRegex.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val id = match.groupValues[2].toInt()
            viewModelScope.launch(Dispatchers.Main) {
                cupboardViewModel.addIngredientToCupboard(Ingredient(id = id, name = name))
                appendAiMessage(responseIndex, "\n(System: Added $name to cupboard)")
            }
        }

        removeRegex.findAll(content).forEach { match ->
            val id = match.groupValues[1].toInt()
            viewModelScope.launch(Dispatchers.Main) {
                cupboardViewModel.removeIngredientFromCupboard(id)
                appendAiMessage(responseIndex, "\n(System: Removed ingredient $id from cupboard)")
            }
        }

        searchRegex.findAll(content).forEach { match ->
            val query = match.groupValues[1]
            viewModelScope.launch(Dispatchers.Main) {
                recipeViewModel.searchRecipes(query)
                appendAiMessage(responseIndex, "\n(System: Searching recipes for $query)")
            }
        }
    }

    private fun appendAiMessage(index: Int, content: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentMessages = _messages.value ?: return@launch
            if (index < currentMessages.size) {
                val message = currentMessages[index]
                currentMessages[index] = message.copy(content = message.content + content)
                _messages.value = currentMessages
            }
        }
    }

    private fun updateAiMessage(index: Int, content: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentMessages = _messages.value ?: return@launch
            if (index < currentMessages.size) {
                currentMessages[index] = ChatMessage(content, false)
                _messages.value = currentMessages
            }
        }
    }
}

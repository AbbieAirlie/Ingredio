package com.example.ingredio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ingredio.data.model.ChatMessage
import com.example.ingredio.data.model.Ingredient
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val messages: LiveData<MutableList<ChatMessage>> get() = _messages

    // Get a free key at https://aistudio.google.com/
    private val geminiApiKey = "AIzaSyBrtP6lZlrrgxeU-y8-HWxN6sltn90DA1s"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = geminiApiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE)
        ),
        requestOptions = RequestOptions(apiVersion = "v1")
    )

    private val systemInstruction = "You are a helpful kitchen assistant. You can manage the user's cupboard and suggest recipes. " +
            "If the user wants to add an ingredient, you MUST include a special tag in your response: [ADD_INGREDIENT:Name,ID]. " +
            "If the user wants to remove an ingredient, use [REMOVE_INGREDIENT:ID]. " +
            "If the user wants to search for recipes, use [SEARCH_RECIPE:Query]. " +
            "Always respond politely."

    private val cupboardViewModel = CupboardViewModel()
    private val recipeViewModel = RecipeViewModel()

    fun sendMessage(text: String) {
        val currentMessages = _messages.value ?: mutableListOf()
        currentMessages.add(ChatMessage(text, true))
        _messages.value = currentMessages

        val aiResponseIndex = currentMessages.size
        currentMessages.add(ChatMessage("", false))
        _messages.value = currentMessages

        streamGeminiResponse(text, aiResponseIndex)
    }

    private fun streamGeminiResponse(userMessage: String, responseIndex: Int) {
        viewModelScope.launch {
            try {
                val chat = generativeModel.startChat(
                    history = listOf(
                        content("user") { text(systemInstruction) },
                        content("model") { text("Understood. I will act as your kitchen assistant and use the tags as requested.") }
                    )
                )
                chat.sendMessageStream(userMessage)
                    .onEach { chunk ->
                        val content = chunk.text ?: ""
                        appendAiMessage(responseIndex, content)
                    }
                    .collect()
                
                // After collection is complete, check for tool tags in the full message
                val fullMessage = _messages.value?.get(responseIndex)?.content ?: ""
                processCustomTags(fullMessage, responseIndex)
                
            } catch (e: Exception) {
                updateAiMessage(responseIndex, "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun processCustomTags(content: String, responseIndex: Int) {
        val addRegex = "\\[ADD_INGREDIENT:(.+),(\\d+)\\]".toRegex()
        val removeRegex = "\\[REMOVE_INGREDIENT:(\\d+)\\]".toRegex()
        val searchRegex = "\\[SEARCH_RECIPE:(.+)\\]".toRegex()

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

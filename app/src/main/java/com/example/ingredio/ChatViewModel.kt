package com.example.ingredio

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ingredio.data.api.SpoonacularService
import com.example.ingredio.data.model.ChatMessage
import com.example.ingredio.data.model.Ingredient
import com.example.ingredio.data.model.Recipe
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val messages: LiveData<MutableList<ChatMessage>> get() = _messages

    private val _showExpiryDialog = MutableLiveData<Ingredient?>()
    val showExpiryDialog: LiveData<Ingredient?> get() = _showExpiryDialog

    fun onDialogShown() { _showExpiryDialog.value = null }

    private var cupboardIngredients: List<Ingredient> = emptyList()

    private fun getSystemInstruction(): String {
        val ingredientList = cupboardIngredients.joinToString(", ") { it.name }.ifEmpty { "nothing" }

        return """
            You are Ingredio AI, a kitchen assistant.
            Current Cupboard: $ingredientList
            
            DIRECTIVES:
            1. Be conversational and brief (max 30 words).
            2. Use [SEARCH_RECIPE:Recipe Name] for every recipe suggestion.
            3. Use [ADD_INGREDIENT:Name] and [REMOVE_INGREDIENT:Name] for inventory.
            4. No instructions or long lists.
            
            Examples:
            - "Try a Grilled Ham and Cheese [SEARCH_RECIPE:Grilled Ham and Cheese]."
            - "Added milk. [ADD_INGREDIENT:Milk] Anything else?"
            - "Removed eggs. [REMOVE_INGREDIENT:Eggs]"
        """.trimIndent()
    }

    private fun getGenerativeModel() = Firebase.vertexAI.generativeModel(
        modelName = "gemini-2.5-flash",
        systemInstruction = content { text(getSystemInstruction()) }
    )

    private var chatSession: com.google.firebase.vertexai.Chat? = null

    private val spoonacularService: SpoonacularService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularService::class.java)
    }
    private val apiKey = "cd1c0340258f4c5bad29f95c40644e2e"

    init { loadChatHistory() }

    private fun loadChatHistory() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Sync cupboard changes
                db.collection("users").document(userId).collection("cupboard")
                    .addSnapshotListener { snapshot, _ ->
                        snapshot?.let {
                            cupboardIngredients = it.toObjects(Ingredient::class.java)
                            chatSession = getGenerativeModel().startChat(history = chatSession?.history ?: emptyList())
                        }
                    }

                // Load chat history
                val snapshot = db.collection("users").document(userId).collection("chatHistory")
                    .orderBy("timestamp", Query.Direction.ASCENDING).get().await()

                val history = snapshot.toObjects(ChatMessage::class.java)
                _messages.postValue(history.toMutableList())

                val vertexHistory = history.map { msg ->
                    content(role = if (msg.fromUser) "user" else "model") { text(msg.content) }
                }
                chatSession = getGenerativeModel().startChat(history = vertexHistory)
            } catch (_: Exception) {
                chatSession = getGenerativeModel().startChat()
            }
        }
    }

    fun sendMessage(text: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentMessages = _messages.value ?: mutableListOf()
        val userMsg = ChatMessage(text, true)
        currentMessages.add(userMsg)
        _messages.value = currentMessages

        db.collection("users").document(userId).collection("chatHistory").add(userMsg)

        val aiIndex = currentMessages.size
        currentMessages.add(ChatMessage("", false))
        _messages.value = currentMessages

        streamAiResponse(text, aiIndex)
    }

    private fun streamAiResponse(userMessage: String, index: Int) {
        viewModelScope.launch {
            try {
                val chat = chatSession ?: getGenerativeModel().startChat().also { chatSession = it }
                var fullContent = ""
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val text = chunk.text ?: ""
                    fullContent += text
                    appendAiMessage(index, text)
                }
                processCustomTags(fullContent, index)
            } catch (_: Exception) {
                updateAiMessage(index, "Error: AI response failed")
            }
        }
    }

    private fun processCustomTags(fullContent: String, index: Int) {
        val addRegex = "\\[ADD_INGREDIENT:([^]]+)]".toRegex()
        val removeRegex = "\\[REMOVE_INGREDIENT:([^]]+)]".toRegex()
        val searchRegex = "\\[SEARCH_RECIPE:([^]]+)]".toRegex()

        val cleanContent = fullContent
            .replace(addRegex, "")
            .replace(removeRegex, "")
            .replace(searchRegex, "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s([.,!?;])"), "$1")
            .trim()
            .removeSuffix(":")
            .removeSuffix(",")
            .trim()

        viewModelScope.launch(Dispatchers.Main) {
            _messages.value?.let { messages ->
                if (index < messages.size) {
                    messages[index] = messages[index].copy(content = cleanContent)
                    _messages.value = messages
                }
            }
        }

        // Add ingredients
        addRegex.findAll(fullContent).forEach { match ->
            val query = match.groupValues[1].trim()
            viewModelScope.launch(Dispatchers.IO) {
                val ingredient = try {
                    val resp = spoonacularService.searchIngredients(apiKey, query, 1).execute()
                    if (resp.isSuccessful) resp.body()?.results?.firstOrNull() ?: Ingredient(name = query)
                    else Ingredient(name = query)
                } catch (_: Exception) { Ingredient(name = query) }
                
                launch(Dispatchers.Main) {
                    addIngredientToDb(ingredient)
                    _showExpiryDialog.value = ingredient
                }
            }
        }

        // Remove ingredients
        removeRegex.findAll(fullContent).forEach { removeIngredientFromDb(it.groupValues[1].trim()) }

        // Search recipes
        val searchMatches = searchRegex.findAll(fullContent).toList()
        viewModelScope.launch(Dispatchers.IO) {
            val recipes = mutableListOf<Recipe>()
            searchMatches.forEach { match ->
                val query = match.groupValues[1].trim()
                Log.d("ChatViewModel", "Searching for recipe: $query")
                try {
                    var resp = spoonacularService.searchRecipes(
                        apiKey = apiKey,
                        query = query,
                        number = 5,
                        addRecipeInformation = true,
                        fillIngredients = true
                    ).execute()

                    if (resp.isSuccessful && (resp.body()?.results?.isEmpty() == true)) {
                        Log.d("ChatViewModel", "No results for '$query', trying fallback...")
                        // Fallback: Try a simplified query by removing some words if it's too specific
                        val words = query.split(" ")
                        if (words.size > 2) {
                            val fallbackQuery = words.filter { it.length > 3 }.joinToString(" ")
                            Log.d("ChatViewModel", "Fallback search: $fallbackQuery")
                            resp = spoonacularService.searchRecipes(
                                apiKey = apiKey,
                                query = fallbackQuery,
                                number = 5,
                                addRecipeInformation = true,
                                fillIngredients = true
                            ).execute()
                        }
                    }

                    if (resp.isSuccessful) {
                        val results = resp.body()?.results
                        Log.d("ChatViewModel", "Found ${results?.size ?: 0} recipes for '$query'")
                        results?.let { recipes.addAll(it) }
                    } else {
                        Log.e("ChatViewModel", "Recipe search error: ${resp.code()} ${resp.message()}")
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Recipe fetch failed for '$query': ${e.message}", e)
                }
            }
            
            val finalRecipes = if (recipes.isEmpty()) null else recipes
            launch(Dispatchers.Main) {
                _messages.value?.let { messages ->
                    if (index < messages.size) {
                        messages[index] = messages[index].copy(recipes = finalRecipes)
                        _messages.value = messages
                    }
                }
                saveFinalMessageToFirestore(cleanContent, finalRecipes)
            }
        }
    }

    private fun saveFinalMessageToFirestore(content: String, recipes: List<Recipe>? = null) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("chatHistory")
            .add(ChatMessage(content, false, recipes = recipes))
    }

    private fun addIngredientToDb(ingredient: Ingredient) {
        val userId = auth.currentUser?.uid ?: return
        val docId = ingredient.name.lowercase().trim().ifEmpty { "unknown_${System.currentTimeMillis()}" }
        val data = hashMapOf("id" to ingredient.id, "name" to ingredient.name, "image" to ingredient.image)
        db.collection("users").document(userId).collection("cupboard").document(docId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    private fun removeIngredientFromDb(name: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("cupboard").document(name.lowercase().trim()).delete()
    }

    private fun appendAiMessage(index: Int, content: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _messages.value?.let { messages ->
                if (index < messages.size) {
                    val msg = messages[index]
                    messages[index] = msg.copy(content = msg.content + content)
                    _messages.value = messages
                }
            }
        }
    }

    private fun updateAiMessage(index: Int, content: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _messages.value?.let { messages ->
                if (index < messages.size) {
                    messages[index] = ChatMessage(content, false)
                    _messages.value = messages
                }
            }
        }
    }

    fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.Main) {
            _messages.value = mutableListOf()
            chatSession = getGenerativeModel().startChat()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val snapshot = db.collection("users").document(userId).collection("chatHistory").get().await()
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                } catch (e: Exception) { Log.e("ChatViewModel", "Clear failed: ${e.message}") }
            }
        }
    }
}

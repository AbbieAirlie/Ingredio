package com.example.ingredio

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ingredio.data.api.SpoonacularService
import com.example.ingredio.data.model.ChatMessage
import com.example.ingredio.data.model.Ingredient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.example.ingredio.data.model.Recipe
import com.example.ingredio.data.model.RecipeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val messages: LiveData<MutableList<ChatMessage>> get() = _messages

    private val _showExpiryDialog = MutableLiveData<Ingredient?>()
    val showExpiryDialog: LiveData<Ingredient?> get() = _showExpiryDialog

    fun onDialogShown() {
        _showExpiryDialog.value = null
    }

    private var cupboardIngredients: List<Ingredient> = emptyList()

    private fun getSystemInstruction(): String {
        val ingredientList = if (cupboardIngredients.isEmpty()) {
            "nothing (empty cupboard)"
        } else {
            cupboardIngredients.joinToString(", ") { it.name }
        }

        return """
            You are Ingredio AI, a specialized kitchen assistant.
            Current Cupboard: $ingredientList
            
            CRITICAL DIRECTIVES:
            1. ALWAYS be conversational. Confirm every action you take (adding/removing) with a short sentence.
            2. For EVERY recipe you suggest, you MUST use the tag [SEARCH_RECIPE:Recipe Name] exactly as shown.
            3. The recipe name in your text MUST exactly match the name in the [SEARCH_RECIPE:Recipe Name] tag.
            4. Keep all responses brief (max 30 words).
            5. Use [ADD_INGREDIENT:Name] and [REMOVE_INGREDIENT:Name] for inventory management.
            6. Do not include instructions or long lists.
            
            Example correct responses:
            - "How about a Grilled Ham and Cheese [SEARCH_RECIPE:Grilled Ham and Cheese] or some Apple and Ham Bites [SEARCH_RECIPE:Apple and Ham Bites]?"
            - "I've added milk to your cupboard. [ADD_INGREDIENT:Milk] What else would you like to add?"
            - "I've removed the eggs for you. [REMOVE_INGREDIENT:Eggs]"
        """.trimIndent()
    }

    // Initialize Vertex AI for Firebase
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

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Set up real-time listener for cupboard
                db.collection("users").document(userId).collection("cupboard")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("ChatViewModel", "Cupboard listener failed: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        cupboardIngredients = snapshot.documents.mapNotNull { it.toObject(Ingredient::class.java) }
                        // Update chat session with new instructions when cupboard changes
                        chatSession = getGenerativeModel().startChat(history = chatSession?.history ?: emptyList())
                    }

                val snapshot = db.collection("users").document(userId).collection("chatHistory")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val history = snapshot.toObjects(ChatMessage::class.java)
                _messages.postValue(history.toMutableList())

                // Initialize chat session with history and current cupboard context
                val vertexHistory = history.map { msg ->
                    content(role = if (msg.fromUser) "user" else "model") { text(msg.content) }
                }
                chatSession = getGenerativeModel().startChat(history = vertexHistory)

            } catch (e: Exception) {
                // If history load fails, start fresh with cupboard context
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

        // Save user message
        db.collection("users").document(userId).collection("chatHistory").add(userMsg)

        val aiResponseIndex = currentMessages.size
        currentMessages.add(ChatMessage("", false))
        _messages.value = currentMessages

        streamAiResponse(text, aiResponseIndex)
    }

    private fun streamAiResponse(userMessage: String, responseIndex: Int) {
        viewModelScope.launch {
            try {
                val chat = chatSession ?: getGenerativeModel().startChat().also { chatSession = it }
                
                var fullAiContent = ""
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val content = chunk.text ?: ""
                    fullAiContent += content
                    appendAiMessage(responseIndex, content)
                }
                
                // Process tags and fetch recipes
                processCustomTags(fullAiContent, responseIndex)
                
            } catch (e: Exception) {
                updateAiMessage(responseIndex, "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun processCustomTags(fullAiContent: String, responseIndex: Int) {
        val addRegex = "\\[ADD_INGREDIENT:([^\\]]+)]".toRegex()
        val removeRegex = "\\[REMOVE_INGREDIENT:([^\\]]+)]".toRegex()
        val searchRegex = "\\[SEARCH_RECIPE:([^\\]]+)]".toRegex()

        // Clean text: Remove tags and normalize whitespace
        val cleanContent = fullAiContent
            .replace(addRegex, "")
            .replace(removeRegex, "")
            .replace(searchRegex, "")
            .replace(Regex("\\s+"), " ")
            .trim()

        viewModelScope.launch(Dispatchers.Main) {
            val currentMessages = _messages.value ?: return@launch
            if (responseIndex < currentMessages.size) {
                val msg = currentMessages[responseIndex]
                currentMessages[responseIndex] = msg.copy(content = cleanContent)
                _messages.value = currentMessages
            }
        }

        addRegex.findAll(fullAiContent).forEach { match ->
            val queryName = match.groupValues[1].trim()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = spoonacularService.searchIngredients(apiKey, queryName, 1).execute()
                    val ingredient = if (response.isSuccessful) {
                        response.body()?.results?.firstOrNull() ?: Ingredient(name = queryName)
                    } else {
                        Ingredient(name = queryName)
                    }
                    launch(Dispatchers.Main) {
                        addIngredientToDb(ingredient)
                        _showExpiryDialog.value = ingredient
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        val fallback = Ingredient(name = queryName)
                        addIngredientToDb(fallback)
                        _showExpiryDialog.value = fallback
                    }
                }
            }
        }

        removeRegex.findAll(fullAiContent).forEach { match ->
            removeIngredientFromDb(match.groupValues[1].trim())
        }

        // Handle recipe searches and save state when done
        val searchMatches = searchRegex.findAll(fullAiContent).toList()
        if (searchMatches.isEmpty()) {
            saveFinalMessageToFirestore(cleanContent, responseIndex)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val allFetchedRecipes = mutableListOf<Recipe>()
                searchMatches.forEach { match ->
                    val query = match.groupValues[1].trim()
                    try {
                        val response = spoonacularService.searchRecipes(apiKey, query, number = 1, addRecipeInformation = true).execute()
                        if (response.isSuccessful) {
                            response.body()?.results?.firstOrNull()?.let { allFetchedRecipes.add(it) }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Recipe fetch failed for $query: ${e.message}")
                    }
                }
                
                launch(Dispatchers.Main) {
                    val currentMessages = _messages.value ?: return@launch
                    if (responseIndex < currentMessages.size) {
                        currentMessages[responseIndex] = currentMessages[responseIndex].copy(recipes = allFetchedRecipes)
                        _messages.value = currentMessages
                        // Save the complete message (with recipes) to Firestore
                        saveFinalMessageToFirestore(cleanContent, responseIndex, allFetchedRecipes)
                    }
                }
            }
        }
    }

    private fun saveFinalMessageToFirestore(content: String, index: Int, recipes: List<Recipe>? = null) {
        val userId = auth.currentUser?.uid ?: return
        val aiMsg = ChatMessage(content, false, recipes = recipes)
        db.collection("users").document(userId).collection("chatHistory").add(aiMsg)
    }

    private fun addIngredientToDb(ingredient: Ingredient) {
        val userId = auth.currentUser?.uid ?: return
        val docId = ingredient.name.lowercase().trim().ifEmpty { "unknown_${System.currentTimeMillis()}" }
        val ingredientData = hashMapOf(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "image" to ingredient.image
        )
        db.collection("users").document(userId).collection("cupboard")
            .document(docId)
            .set(ingredientData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to add ingredient: ${e.message}")
            }
    }

    private fun removeIngredientFromDb(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val docId = name.lowercase().trim()
        db.collection("users").document(userId).collection("cupboard")
            .document(docId)
            .delete()
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to remove ingredient: ${e.message}")
            }
    }

    private fun searchRecipes(query: String, responseIndex: Int) {
        // This function is now handled inside processCustomTags to manage state better
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

    fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _messages.value = mutableListOf()
                chatSession = getGenerativeModel().startChat()

                viewModelScope.launch(Dispatchers.IO) {
                    val snapshot = db.collection("users").document(userId).collection("chatHistory").get().await()
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Clear chat failed: ${e.message}")
            }
        }
    }
}

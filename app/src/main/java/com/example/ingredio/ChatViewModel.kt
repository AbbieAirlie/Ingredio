package com.example.ingredio

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
            1. NEVER provide full recipes, instructions, or ingredient lists in text. 
            2. For EVERY recipe you suggest, you MUST use the tag [SEARCH_RECIPE:Recipe Name] exactly as shown.
            3. Keep all responses extremely brief (max 20 words).
            4. If the user asks for recipes, give 2-3 short suggestions with tags.
            5. Use [ADD_INGREDIENT:Name] and [REMOVE_INGREDIENT:Name] for inventory management.
            
            Example correct responses:
            - "How about a Ham & Cheese Melt [SEARCH_RECIPE:Ham and Cheese Melt] or a Cheese Omelet [SEARCH_RECIPE:Cheese Omelet]?"
            - "I've added milk to your cupboard. [ADD_INGREDIENT:Milk]"
            - "You can make Pasta Carbonara [SEARCH_RECIPE:Pasta Carbonara]."
        """.trimIndent()
    }

    // Initialize Vertex AI for Firebase
    private fun getGenerativeModel() = Firebase.vertexAI.generativeModel(
        modelName = "gemini-2.5-flash",
        systemInstruction = content { text(getSystemInstruction()) }
    )

    private var chatSession: com.google.firebase.vertexai.Chat? = null

    private val cupboardViewModel = CupboardViewModel()
    private val recipeViewModel = RecipeViewModel()

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
                        if (e != null || snapshot == null) return@addSnapshotListener
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
                
                // Save AI message to Firestore when stream completes and get reference
                val userId = auth.currentUser?.uid
                var docRef: com.google.firebase.firestore.DocumentReference? = null
                if (userId != null) {
                    val aiMsg = ChatMessage(fullAiContent, false)
                    docRef = db.collection("users").document(userId).collection("chatHistory").add(aiMsg).await()
                }

                processCustomTags(fullAiContent, responseIndex, docRef)
                
            } catch (e: Exception) {
                updateAiMessage(responseIndex, "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun processCustomTags(fullAiContent: String, responseIndex: Int, docRef: com.google.firebase.firestore.DocumentReference?) {
        val addRegex = "\\[ADD_INGREDIENT:([^\\]]+)]".toRegex()
        val removeRegex = "\\[REMOVE_INGREDIENT:([^\\]]+)]".toRegex()
        val searchRegex = "\\[SEARCH_RECIPE:([^\\]]+)]".toRegex()

        // Clean text: Remove tags and normalize whitespace
        var cleanContent = fullAiContent
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

                // Update cleaned text in Firestore using docRef
                docRef?.update("content", cleanContent)
            }
        }

        addRegex.findAll(fullAiContent).forEach { match ->
            val queryName = match.groupValues[1].trim()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = spoonacularService.searchIngredients(apiKey, queryName, 1).execute()
                    if (response.isSuccessful) {
                        val ingredient = response.body()?.results?.firstOrNull()
                        if (ingredient != null) {
                            launch(Dispatchers.Main) {
                                cupboardViewModel.addIngredientToCupboard(ingredient)
                                _showExpiryDialog.value = ingredient
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        removeRegex.findAll(fullAiContent).forEach { match ->
            val queryName = match.groupValues[1].trim()
            viewModelScope.launch(Dispatchers.Main) {
                cupboardViewModel.removeIngredientFromCupboard(Ingredient(name = queryName))
            }
        }

        searchRegex.findAll(fullAiContent).forEach { match ->
            val query = match.groupValues[1].trim()
            viewModelScope.launch(Dispatchers.Main) {
                spoonacularService.searchRecipes(apiKey, query, number = 3).enqueue(object : Callback<RecipeResponse> {
                    override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                        if (response.isSuccessful) {
                            val results = response.body()?.results ?: emptyList()
                            val currentMessages = _messages.value ?: return
                            if (responseIndex < currentMessages.size) {
                                val msg = currentMessages[responseIndex]
                                val existingRecipes = msg.recipes ?: emptyList()
                                val newRecipes = results.filter { res -> existingRecipes.none { it.id == res.id } }
                                if (newRecipes.isNotEmpty()) {
                                    val updatedRecipes = existingRecipes + newRecipes
                                    currentMessages[responseIndex] = msg.copy(recipes = updatedRecipes)
                                    _messages.value = currentMessages
                                    
                                    // Update recipes in Firestore using docRef
                                    docRef?.update("recipes", updatedRecipes)
                                }
                            }
                        }
                    }
                    override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {}
                })
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
    fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Clear UI immediately
                _messages.value = mutableListOf()
                chatSession = getGenerativeModel().startChat()

                // Delete from Firestore in background
                viewModelScope.launch(Dispatchers.IO) {
                    val snapshot = db.collection("users").document(userId).collection("chatHistory").get().await()
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

package com.example.ingredio

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.ingredio.data.model.Ingredient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CupboardViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: CupboardViewModel

    @MockK
    private lateinit var db: FirebaseFirestore

    @MockK
    private lateinit var auth: FirebaseAuth

    @MockK
    private lateinit var user: FirebaseUser

    @MockK
    private lateinit var statusObserver: Observer<String>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = CupboardViewModel(db, auth)
        viewModel.status.observeForever(statusObserver)
        every { statusObserver.onChanged(any()) } just Runs
    }

    @Test
    fun `addIngredientToCupboard sets status error when user not logged in`() {
        // Given
        every { auth.currentUser } returns null

        // When
        viewModel.addIngredientToCupboard(Ingredient(name = "Milk"))

        // Then
        verify { statusObserver.onChanged("User not logged in") }
    }

    @Test
    fun `docId generation normalizes input by trimming and lowercasing`() {
        // Given
        val userId = "test_user"
        val ingredientWithSpaces = Ingredient(id = 1, name = " Milk ", image = "milk.jpg")
        val ingredientUppercase = Ingredient(id = 2, name = "MILK", image = "milk.jpg")
        
        every { auth.currentUser } returns user
        every { user.uid } returns userId

        val collectionRef = mockk<CollectionReference>()
        val userDocRef = mockk<DocumentReference>()
        val cupboardCollRef = mockk<CollectionReference>()
        val itemDocRef = mockk<DocumentReference>()
        val task = mockk<Task<Void>>()

        every { db.collection("users") } returns collectionRef
        every { collectionRef.document(userId) } returns userDocRef
        every { userDocRef.collection("cupboard") } returns cupboardCollRef
        
        // Mocking document call for normalized ID
        every { cupboardCollRef.document("milk") } returns itemDocRef
        
        every { itemDocRef.set(any<Map<String, Any>>(), any()) } returns task
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } returns task

        // When adding " Milk "
        viewModel.addIngredientToCupboard(ingredientWithSpaces)
        // Then verify "milk" document was used
        verify { cupboardCollRef.document("milk") }

        // When adding "MILK"
        viewModel.addIngredientToCupboard(ingredientUppercase)
        // Then verify "milk" document was used again
        verify(exactly = 2) { cupboardCollRef.document("milk") }
    }

    @Test
    fun `addIngredientToCupboard calls firestore when user is logged in`() {
        // Given
        val userId = "test_user"
        val ingredient = Ingredient(id = 1, name = "Milk", image = "milk.jpg")
        every { auth.currentUser } returns user
        every { user.uid } returns userId

        val collectionRef = mockk<CollectionReference>()
        val userDocRef = mockk<DocumentReference>()
        val cupboardCollRef = mockk<CollectionReference>()
        val itemDocRef = mockk<DocumentReference>()
        val task = mockk<Task<Void>>()

        every { db.collection("users") } returns collectionRef
        every { collectionRef.document(userId) } returns userDocRef
        every { userDocRef.collection("cupboard") } returns cupboardCollRef
        every { cupboardCollRef.document("milk") } returns itemDocRef
        
        every { itemDocRef.set(any<Map<String, Any>>(), any()) } returns task
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } returns task

        // When
        viewModel.addIngredientToCupboard(ingredient)

        // Then
        verify { itemDocRef.set(match<Map<String, Any>> { it["name"] == "Milk" }, any()) }
    }
}

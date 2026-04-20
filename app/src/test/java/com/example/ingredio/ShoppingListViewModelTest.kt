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

class ShoppingListViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ShoppingListViewModel

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
        viewModel = ShoppingListViewModel(db, auth)
        viewModel.status.observeForever(statusObserver)
        every { statusObserver.onChanged(any()) } just Runs
    }

    @Test
    fun `addIngredientToShoppingList fails when user not logged in`() {
        every { auth.currentUser } returns null

        viewModel.addIngredientToShoppingList(Ingredient(name = "Apples"))

        verify { statusObserver.onChanged("User not logged in") }
    }

    @Test
    fun `removeIngredientFromShoppingList calls delete on firestore`() {
        val userId = "test_user"
        val ingredientId = 123
        every { auth.currentUser } returns user
        every { user.uid } returns userId

        val collectionRef = mockk<CollectionReference>()
        val userDocRef = mockk<DocumentReference>()
        val shoppingCollRef = mockk<CollectionReference>()
        val itemDocRef = mockk<DocumentReference>()
        val task = mockk<Task<Void>>()

        every { db.collection("users") } returns collectionRef
        every { collectionRef.document(userId) } returns userDocRef
        every { userDocRef.collection("shoppingList") } returns shoppingCollRef
        every { shoppingCollRef.document(ingredientId.toString()) } returns itemDocRef
        every { itemDocRef.delete() } returns task
        every { task.addOnSuccessListener(any()) } returns task

        viewModel.removeIngredientFromShoppingList(ingredientId)

        verify { itemDocRef.delete() }
    }
}

package com.example.ingredio

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.ingredio.data.model.MealPlan
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

class MealPlannerViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: MealPlannerViewModel

    @MockK
    private lateinit var db: FirebaseFirestore

    @MockK
    private lateinit var auth: FirebaseAuth

    @MockK
    private lateinit var user: FirebaseUser

    @MockK
    private lateinit var errorObserver: Observer<String>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = MealPlannerViewModel(db, auth)
        viewModel.error.observeForever(errorObserver)
        every { errorObserver.onChanged(any()) } just Runs
    }

    @Test
    fun `addMealPlan calls firestore when user is logged in`() {
        // Given
        val userId = "test_user"
        val mealPlan = MealPlan(recipeTitle = "Pasta", date = "2023-10-27")
        every { auth.currentUser } returns user
        every { user.uid } returns userId

        val collectionRef = mockk<CollectionReference>()
        val userDocRef = mockk<DocumentReference>()
        val mealPlansCollRef = mockk<CollectionReference>()
        val task = mockk<Task<DocumentReference>>()

        every { db.collection("users") } returns collectionRef
        every { collectionRef.document(userId) } returns userDocRef
        every { userDocRef.collection("mealPlans") } returns mealPlansCollRef
        every { mealPlansCollRef.add(mealPlan) } returns task
        every { task.addOnFailureListener(any()) } returns task

        // When
        viewModel.addMealPlan(mealPlan)

        // Then
        verify { mealPlansCollRef.add(mealPlan) }
    }

    @Test
    fun `deleteMealPlan does nothing if meal plan id is empty`() {
        // Given
        val userId = "test_user"
        every { auth.currentUser } returns user
        every { user.uid } returns userId
        val mealPlan = MealPlan(id = "")
        
        // When
        viewModel.deleteMealPlan(mealPlan)

        // Then
        verify(exactly = 0) { db.collection(any()) }
    }
}

package com.example.ingredio.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealPlanTest {

    @Test
    fun `isCustom returns true when customText is present and recipeId is null`() {
        val mealPlan = MealPlan(
            customText = "Custom Meal",
            recipeId = null
        )
        assertTrue(mealPlan.isCustom)
    }

    @Test
    fun `isCustom returns false when recipeId is present`() {
        val mealPlan = MealPlan(
            customText = "Custom Meal",
            recipeId = 123
        )
        assertFalse(mealPlan.isCustom)
    }

    @Test
    fun `isCustom returns false when both are null`() {
        val mealPlan = MealPlan(
            customText = null,
            recipeId = null
        )
        assertFalse(mealPlan.isCustom)
    }
}

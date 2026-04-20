package com.example.ingredio.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientTest {

    @Test
    fun `imageUrl returns correct spoonacular url`() {
        val ingredient = Ingredient(
            id = 1,
            name = "apple",
            image = "apple.jpg"
        )
        val expectedUrl = "https://spoonacular.com/cdn/ingredients_100x100/apple.jpg"
        assertEquals(expectedUrl, ingredient.imageUrl)
    }

    @Test
    fun `ingredient default values are correct`() {
        val ingredient = Ingredient()
        assertEquals(0, ingredient.id)
        assertEquals("", ingredient.name)
        assertEquals("", ingredient.image)
        assertEquals(null, ingredient.expiryDate)
        assertEquals(null, ingredient.expiryType)
        assertEquals(null, ingredient.storageType)
    }
}

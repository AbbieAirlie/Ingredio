package com.example.ingredio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageTest {

    @Test
    fun `chat message defaults are correct`() {
        val before = System.currentTimeMillis()
        val message = ChatMessage()
        val after = System.currentTimeMillis()

        assertEquals("", message.content)
        assertEquals(false, message.fromUser)
        assertEquals(null, message.recipes)
        assertTrue(message.timestamp in before..after)
    }

    @Test
    fun `chat message initialization works`() {
        val recipes = listOf(Recipe(id = 1, title = "Test Recipe"))
        val message = ChatMessage(
            content = "Hello",
            fromUser = true,
            timestamp = 123456L,
            recipes = recipes
        )

        assertEquals("Hello", message.content)
        assertEquals(true, message.fromUser)
        assertEquals(123456L, message.timestamp)
        assertEquals(recipes, message.recipes)
    }
}

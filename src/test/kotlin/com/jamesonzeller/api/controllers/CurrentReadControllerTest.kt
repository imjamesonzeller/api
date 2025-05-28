package com.jamesonzeller.api.currentread

import com.jamesonzeller.api.currentread.services.CurrentReadService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CurrentReadServiceTest {

    private val service = CurrentReadService()

    @Test
    fun `parses valid Goodreads description`() {
        val input = "Jameson is currently reading The Catcher in the Rye by J.D. Salinger"
        val result = service.testableParse(input)
        assertEquals("The Catcher in the Rye by J.D. Salinger", result)
    }

    @Test
    fun `falls back on invalid input`() {
        val input = "Jameson is doing something else"
        val result = service.testableParse(input)
        assertEquals("Tuesdays with Morrie by Mitch Albom", result)
    }
}
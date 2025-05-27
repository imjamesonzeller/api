package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.hamcrest.Matchers.hasItem
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType

@Import(SecurityConfig::class)
@WebMvcTest(WordSearchController::class)
class WordSearchControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `GET returns valid word search`() {
        mockMvc.get("/generate_word_search")
            .andExpect {
                status { isOk() }
                jsonPath("$.search") { exists() }
                jsonPath("$.words") { exists() }
            }
    }

    @Test
    fun `POST with valid words returns word search`() {
        val json = """{"words": ["alpha", "beta", "gamma"]}"""

        mockMvc.post("/generate_word_search") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.search") { exists() }
            jsonPath("$.words", hasItem("alpha"))
        }
    }

    @Test
    fun `POST with invalid JSON returns fallback`() {
        val badJson = """{words: [apple banana]}""" // malformed JSON

        mockMvc.post("/generate_word_search") {
            contentType = MediaType.APPLICATION_JSON
            content = badJson
        }.andExpect {
            status { isOk() }
            jsonPath("$.search") { exists() }
            jsonPath("$.words") { exists() }
        }
    }
}
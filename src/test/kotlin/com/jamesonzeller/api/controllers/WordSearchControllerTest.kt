package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.security.SecurityConfig
import com.jamesonzeller.api.wordsearchgenerator.services.WordSearchService
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(WordSearchController::class)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class WordSearchControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var wordSearchService: WordSearchService

    @Test
    fun `GET returns valid word search`() {
        val dummyGrid = listOf(
            listOf("A", "B"),
            listOf("C", "D")
        )
        val dummyWords = listOf("alpha", "beta")
        `when`(wordSearchService.generate(null)).thenReturn(dummyGrid to dummyWords)

        mockMvc.get("/generate_word_search")
            .andExpect {
                status { isOk() }
                jsonPath("$.search") { exists() }
                jsonPath("$.words") { exists() }
            }
    }

    @Test
    fun `POST with valid words returns word search`() {
        val inputWords = listOf("alpha", "beta", "gamma")
        val dummyGrid = listOf(
            listOf("A", "B"),
            listOf("C", "D")
        )
        `when`(wordSearchService.generate(inputWords)).thenReturn(dummyGrid to inputWords)

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
        val dummyGrid = listOf(
            listOf("X", "Y"),
            listOf("Z", "W")
        )
        val dummyWords = listOf("dummy1", "dummy2")
        `when`(wordSearchService.generate(null)).thenReturn(dummyGrid to dummyWords)

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
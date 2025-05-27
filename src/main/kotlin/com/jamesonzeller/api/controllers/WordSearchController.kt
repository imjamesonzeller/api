package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.wordsearchgenerator.models.WordSearch
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/generate_word_search")
class WordSearchController() {
    data class WordSearchResponse(
        val search: List<List<String>>,
        val words: List<String>
    )

    @GetMapping
    fun generateWordSearchGet(): WordSearchResponse {
        val grid = WordSearch(null)
        return WordSearchResponse(grid.generateWordSearch(), grid.words)
    }

    @PostMapping
    fun generateWordSearchPost(
        @RequestBody(required = false) body: Map<String, Any>?
    ): WordSearchResponse {
        val words = (body?.get("words") as? List<*>)?.filterIsInstance<String>()
        val grid = WordSearch(words)
        return WordSearchResponse(grid.generateWordSearch(), grid.words)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidJson(): WordSearchResponse {
        val grid = WordSearch(null)  // fallback to random words
        return WordSearchResponse(grid.generateWordSearch(), grid.words)
    }
}
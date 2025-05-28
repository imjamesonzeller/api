package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.wordsearchgenerator.models.WordSearch
import com.jamesonzeller.api.wordsearchgenerator.services.WordSearchService
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/generate_word_search")
class WordSearchController(
    private val wordSearchService: WordSearchService
) {
    data class WordSearchResponse(
        val search: List<List<String>>,
        val words: List<String>
    )

    @GetMapping
    fun generateWordSearchGet(): WordSearchResponse {
        val (grid, words) = wordSearchService.generate()
        return WordSearchResponse(grid, words)
    }

    @PostMapping
    fun generateWordSearchPost(
        @RequestBody(required = false) body: Map<String, Any>?
    ): WordSearchResponse {
        val words = (body?.get("words") as? List<*>)?.filterIsInstance<String>()
        val (grid, usedWords) = wordSearchService.generate(words)
        return WordSearchResponse(grid, usedWords)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidJson(): WordSearchResponse {
        val grid = WordSearch(null)  // fallback to random words
        return WordSearchResponse(grid.generateWordSearch(), grid.words)
    }
}
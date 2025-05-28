package com.jamesonzeller.api.wordsearchgenerator.services

import com.jamesonzeller.api.wordsearchgenerator.models.WordSearch
import org.springframework.stereotype.Service

@Service
class WordSearchService {
    fun generate(words: List<String>? = null): Pair<List<List<String>>, List<String>> {
        val generator = WordSearch(words)
        return generator.generateWordSearch() to generator.words
    }
}
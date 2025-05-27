package com.jamesonzeller.api.wordsearchgenerator.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object RandomWords {
    private val words: List<String> by lazy {
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("words.json")
            ?: throw IllegalArgumentException("Could not load words.json")
        val mapper = jacksonObjectMapper()
        mapper.readValue<List<String>>(inputStream)
    }

    fun randomWords(): List<String> {
        return List(15) { words.random() }
    }
}
package com.jamesonzeller.api.currentread.services

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class CurrentReadService {
    private val logger = LoggerFactory.getLogger(CurrentReadService::class.java)

    @Cacheable("currentRead")
    fun getCurrentRead(): String {
        val rawText = fetchText()
        return parseTitleAndAuthor(rawText)
    }

    private fun fetchText(): String {
        val url = "https://www.goodreads.com/jamesonzeller"
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        return try {
            val doc: Document = Jsoup.connect(url)
                .userAgent(userAgent)
                .get()

            val metaTag = doc.selectFirst("meta[name=description]")
            metaTag?.attr("content") ?: FALLBACK
        } catch (e: Exception) {
            logger.error("Failed to fetch or parse Goodreads page", e)
            FALLBACK
        }
    }

    private fun parseTitleAndAuthor(raw: String): String {
        return try {
            raw.split("currently reading", ignoreCase = true)[1].trim()
        } catch (e: IndexOutOfBoundsException) {
            logger.warn("Failed to parse current read from input: {}", raw)
            FALLBACK
        }
    }

    private companion object {
        private const val FALLBACK = "Tuesdays with Morrie by Mitch Albom"
    }

    fun testableParse(raw: String): String = parseTitleAndAuthor(raw)
}

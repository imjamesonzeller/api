package com.jamesonzeller.api.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cache = CaffeineCache(
            "currentRead",
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .build()
        )

        val manager = SimpleCacheManager()
        manager.setCaches(listOf(cache))
        return manager
    }
}
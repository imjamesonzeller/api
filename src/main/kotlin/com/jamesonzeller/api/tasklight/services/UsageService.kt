package com.jamesonzeller.api.tasklight.services

import com.jamesonzeller.api.tasklight.models.Usage
import com.jamesonzeller.api.tasklight.repositories.UsageRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class UsageService(
    private val usageRepository: UsageRepository
) {

    fun getTodayUsage(userId: UUID): Int {
        val today = LocalDate.now()
        return usageRepository.findByUserIdAndDate(userId, today)?.requestsMade ?: 0
    }

    fun incrementUsage(userId: UUID): Int {
        val today = LocalDate.now()
        val usage = usageRepository.findByUserIdAndDate(userId, today)
            ?.apply { requestsMade++ }
            ?: Usage(userId = userId, date = today, requestsMade = 1)

        usageRepository.save(usage)
        return usage.requestsMade
    }
}
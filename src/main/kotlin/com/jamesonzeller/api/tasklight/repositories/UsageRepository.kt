package com.jamesonzeller.api.tasklight.repositories

import com.jamesonzeller.api.tasklight.models.Usage
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface UsageRepository : JpaRepository<Usage, UUID> {
    fun findByUserIdAndDate(userID: UUID, date: LocalDate): Usage?
}
package com.jamesonzeller.api.tasklight.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "usages", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "date"])])
data class Usage(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val date: LocalDate = LocalDate.now(),

    @Column(name = "requests_made", nullable = false)
    var requestsMade: Int = 0,
)

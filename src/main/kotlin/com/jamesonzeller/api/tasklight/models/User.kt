package com.jamesonzeller.api.tasklight.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val email: String = "",

    @Column(nullable = false)
    val passwordHash: String = "",

    val createdAt: LocalDateTime = LocalDateTime.now(),
)

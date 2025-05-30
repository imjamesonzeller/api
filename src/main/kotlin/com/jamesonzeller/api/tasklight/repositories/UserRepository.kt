package com.jamesonzeller.api.tasklight.repositories

import com.jamesonzeller.api.tasklight.models.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
}
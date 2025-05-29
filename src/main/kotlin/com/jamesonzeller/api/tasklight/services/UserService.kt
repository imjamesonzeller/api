package com.jamesonzeller.api.tasklight.services

import com.jamesonzeller.api.tasklight.models.User
import com.jamesonzeller.api.tasklight.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(email: String, password: String): User {
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email already registered")
        }
        val hashed = passwordEncoder.encode(password)
        return userRepository.save(User(email = email, passwordHash = hashed))
    }

    fun login(email: String, password: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw IllegalArgumentException("Passwords do not match")
        }

        return user
    }
}
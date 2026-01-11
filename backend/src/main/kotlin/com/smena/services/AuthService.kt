package com.smena.services

import com.smena.dto.AuthResponse
import com.smena.dto.toDto
import com.smena.exceptions.InvalidInitDataException
import com.smena.models.User
import com.smena.repositories.UserRepository
import com.smena.telegram.InitDataValidator
import com.smena.telegram.TelegramUser

class AuthService(
    private val botToken: String,
    private val userRepository: UserRepository = UserRepository()
) {

    fun authenticate(initData: String): AuthResponse {
        val telegramUser = InitDataValidator.validate(initData, botToken)
            ?: throw InvalidInitDataException("Invalid initData signature")

        val user = findOrCreateUser(telegramUser)

        return AuthResponse(
            user = user.toDto(),
            teams = emptyList() // Teams will be implemented in phase 3
        )
    }

    private fun findOrCreateUser(telegramUser: TelegramUser): User {
        val existingUser = userRepository.findByTelegramId(telegramUser.id)

        return if (existingUser != null) {
            if (existingUser.needsUpdate(telegramUser)) {
                userRepository.update(existingUser.id, telegramUser)
            } else {
                existingUser
            }
        } else {
            userRepository.create(telegramUser)
        }
    }

    private fun User.needsUpdate(telegramUser: TelegramUser): Boolean {
        return firstName != telegramUser.firstName ||
                lastName != telegramUser.lastName ||
                username != telegramUser.username
    }
}

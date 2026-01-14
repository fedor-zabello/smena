package com.smena.services

import com.smena.dto.AuthResponse
import com.smena.dto.toDto
import com.smena.exceptions.InvalidInitDataException
import com.smena.models.User
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository
import com.smena.telegram.InitDataValidator
import com.smena.telegram.TelegramUser

class AuthService(
    private val botToken: String,
    private val userRepository: UserRepository = UserRepository(),
    private val teamRepository: TeamRepository = TeamRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository()
) {

    fun authenticate(initData: String): AuthResponse {
        val telegramUser = InitDataValidator.validate(initData, botToken)
            ?: throw InvalidInitDataException("Invalid initData signature")

        val user = findOrCreateUser(telegramUser)
        val teams = getUserTeams(user.id)

        return AuthResponse(
            user = user.toDto(),
            teams = teams
        )
    }

    private fun getUserTeams(userId: Long): List<com.smena.dto.TeamDto> {
        val memberships = teamMemberRepository.findAllByUserId(userId)
        return memberships.mapNotNull { membership ->
            val team = teamRepository.findById(membership.teamId) ?: return@mapNotNull null
            team.toDto(role = membership.role.name)
        }
    }

    private fun com.smena.models.Team.toDto(role: String) = com.smena.dto.TeamDto(
        id = id,
        name = name,
        role = role
    )

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

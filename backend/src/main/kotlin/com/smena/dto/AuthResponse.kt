package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val user: UserDto,
    val teams: List<TeamDto>
)

@Serializable
data class UserDto(
    val id: Long,
    val telegramId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?
)

@Serializable
data class TeamDto(
    val id: Long,
    val name: String,
    val role: String
)

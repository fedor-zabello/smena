package com.smena.dto

import com.smena.models.Team
import com.smena.models.User

fun User.toDto() = UserDto(
    id = id,
    telegramId = telegramId,
    firstName = firstName,
    lastName = lastName,
    username = username
)

fun Team.toDto(role: String) = TeamDto(
    id = id,
    name = name,
    role = role
)

fun Team.toResponse(role: String, memberCount: Int) = TeamResponse(
    id = id,
    name = name,
    inviteCode = inviteCode,
    role = role,
    memberCount = memberCount
)

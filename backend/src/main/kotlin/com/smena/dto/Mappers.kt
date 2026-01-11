package com.smena.dto

import com.smena.models.User

fun User.toDto() = UserDto(
    id = id,
    telegramId = telegramId,
    firstName = firstName,
    lastName = lastName,
    username = username
)

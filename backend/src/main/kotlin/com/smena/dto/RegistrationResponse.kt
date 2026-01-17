package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val status: String,
    val registeredAt: String,
    val updatedAt: String
)

@Serializable
data class RegistrationListResponse(
    val registrations: List<RegistrationResponse>
)

package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse(
    val id: Long,
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val role: String,
    val joinedAt: String
)

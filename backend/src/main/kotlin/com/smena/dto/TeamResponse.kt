package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamResponse(
    val id: Long,
    val name: String,
    val inviteCode: String,
    val role: String,
    val memberCount: Int
)

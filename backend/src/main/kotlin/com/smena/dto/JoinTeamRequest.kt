package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class JoinTeamRequest(
    val inviteCode: String
)

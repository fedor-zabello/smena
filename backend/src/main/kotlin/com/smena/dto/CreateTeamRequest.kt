package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTeamRequest(
    val name: String
)

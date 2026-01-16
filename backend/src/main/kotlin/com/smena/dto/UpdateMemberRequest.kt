package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMemberRequest(
    val role: String
)

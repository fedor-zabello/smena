package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRegistrationRequest(
    val status: String
)

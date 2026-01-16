package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val eventDate: String? = null,
    val eventTime: String? = null,
    val location: String? = null,
    val maxPlayers: Int? = null
)

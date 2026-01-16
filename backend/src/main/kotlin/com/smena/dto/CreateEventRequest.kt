package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateEventRequest(
    val type: String,
    val title: String? = null,
    val description: String? = null,
    val eventDate: String,
    val eventTime: String,
    val location: String? = null,
    val maxPlayers: Int? = null
)

package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val id: Long,
    val teamId: Long,
    val type: String,
    val title: String?,
    val description: String?,
    val eventDate: String,
    val eventTime: String,
    val location: String?,
    val maxPlayers: Int?,
    val registrationOpensAt: String,
    val status: String,
    val createdBy: Long?,
    val createdAt: String
)

@Serializable
data class EventListResponse(
    val upcoming: List<EventResponse>,
    val past: List<EventResponse>
)

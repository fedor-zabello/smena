package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class LineupResponse(
    val entries: List<LineupEntryResponse>
)

@Serializable
data class LineupEntryResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val lineGroup: String,
    val position: String?,
    val createdAt: String
)

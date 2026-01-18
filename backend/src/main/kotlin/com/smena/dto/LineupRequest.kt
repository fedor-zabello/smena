package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class LineupRequest(
    val entries: List<LineupEntryRequest>
)

@Serializable
data class LineupEntryRequest(
    val userId: Long,
    val lineGroup: String,
    val position: String? = null
)

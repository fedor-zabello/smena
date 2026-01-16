package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class EventType {
    GAME,
    TRAINING
}

enum class EventStatus {
    SCHEDULED,
    OPEN,
    CLOSED,
    CANCELLED
}

object Events : LongIdTable("events") {
    val teamId = long("team_id").references(Teams.id)
    val type = varchar("type", 20)
    val title = varchar("title", 255).nullable()
    val description = text("description").nullable()
    val eventDate = date("event_date")
    val eventTime = time("event_time")
    val location = varchar("location", 255).nullable()
    val maxPlayers = integer("max_players").nullable()
    val registrationOpensAt = timestamp("registration_opens_at")
    val status = varchar("status", 20).default(EventStatus.SCHEDULED.name)
    val createdBy = long("created_by").references(Users.id).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

data class Event(
    val id: Long,
    val teamId: Long,
    val type: EventType,
    val title: String?,
    val description: String?,
    val eventDate: LocalDate,
    val eventTime: LocalTime,
    val location: String?,
    val maxPlayers: Int?,
    val registrationOpensAt: Instant,
    val status: EventStatus,
    val createdBy: Long?,
    val createdAt: Instant
)

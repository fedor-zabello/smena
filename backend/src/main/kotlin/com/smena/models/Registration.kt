package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class RegistrationStatus {
    GOING,
    NOT_GOING
}

object Registrations : LongIdTable("registrations") {
    val eventId = long("event_id").references(Events.id)
    val userId = long("user_id").references(Users.id)
    val status = varchar("status", 20)
    val registeredAt = timestamp("registered_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(eventId, userId)
    }
}

data class Registration(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val status: RegistrationStatus,
    val registeredAt: Instant,
    val updatedAt: Instant
)

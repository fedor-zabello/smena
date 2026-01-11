package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class TeamRole {
    PLAYER,
    COACH,
    ADMIN
}

object TeamMembers : LongIdTable("team_members") {
    val userId = long("user_id").references(Users.id)
    val teamId = long("team_id").references(Teams.id)
    val role = varchar("role", 20)
    val joinedAt = timestamp("joined_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(userId, teamId)
    }
}

data class TeamMember(
    val id: Long,
    val userId: Long,
    val teamId: Long,
    val role: TeamRole,
    val joinedAt: Instant
)

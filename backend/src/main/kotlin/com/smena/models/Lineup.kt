package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class LineGroup {
    // Game lines (3-4 lines + goalies)
    LINE_1,
    LINE_2,
    LINE_3,
    LINE_4,
    GOALIES,

    // Training teams (Light vs Dark, 2 lines each + goalie)
    LIGHT_1,
    LIGHT_2,
    LIGHT_GOALIES,
    DARK_1,
    DARK_2,
    DARK_GOALIES;

    /** Checks if this line group is valid for the given event type */
    fun isValidFor(eventType: EventType): Boolean = this in forEventType(eventType)

    companion object {
        private val GAME_GROUPS = setOf(LINE_1, LINE_2, LINE_3, LINE_4, GOALIES)
        private val TRAINING_GROUPS = setOf(
            LIGHT_1, LIGHT_2, LIGHT_GOALIES,
            DARK_1, DARK_2, DARK_GOALIES
        )

        /** Returns valid line groups for the given event type */
        fun forEventType(eventType: EventType): Set<LineGroup> = when (eventType) {
            EventType.GAME -> GAME_GROUPS
            EventType.TRAINING -> TRAINING_GROUPS
        }
    }
}

enum class Position {
    LW,  // Left Wing
    C,   // Center
    RW,  // Right Wing
    LD,  // Left Defense
    RD,  // Right Defense
    G    // Goalie
}

object Lineups : LongIdTable("lineups") {
    val eventId = long("event_id").references(Events.id)
    val userId = long("user_id").references(Users.id)
    val lineGroup = varchar("line_group", 20)
    val position = varchar("position", 10).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(eventId, userId)
    }
}

data class Lineup(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val lineGroup: LineGroup,
    val position: Position?,
    val createdAt: Instant
)

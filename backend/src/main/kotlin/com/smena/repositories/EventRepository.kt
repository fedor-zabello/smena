package com.smena.repositories

import com.smena.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class EventRepository {

    fun findById(id: Long): Event? = transaction {
        Events.selectAll()
            .where { Events.id eq id }
            .map { it.toEvent() }
            .singleOrNull()
    }

    fun findAllByTeamId(teamId: Long): List<Event> = transaction {
        Events.selectAll()
            .where { Events.teamId eq teamId }
            .orderBy(Events.eventDate to SortOrder.ASC, Events.eventTime to SortOrder.ASC)
            .map { it.toEvent() }
    }

    fun findByTeamIdAndStatus(teamId: Long, status: EventStatus): List<Event> = transaction {
        Events.selectAll()
            .where { (Events.teamId eq teamId) and (Events.status eq status.name) }
            .orderBy(Events.eventDate to SortOrder.ASC, Events.eventTime to SortOrder.ASC)
            .map { it.toEvent() }
    }

    fun findUpcomingByTeamId(teamId: Long): List<Event> = transaction {
        val today = LocalDate.now()
        Events.selectAll()
            .where { (Events.teamId eq teamId) and (Events.eventDate greaterEq today) and (Events.status neq EventStatus.CANCELLED.name) }
            .orderBy(Events.eventDate to SortOrder.ASC, Events.eventTime to SortOrder.ASC)
            .map { it.toEvent() }
    }

    fun findPastByTeamId(teamId: Long): List<Event> = transaction {
        val today = LocalDate.now()
        Events.selectAll()
            .where { (Events.teamId eq teamId) and (Events.eventDate less today) }
            .orderBy(Events.eventDate to SortOrder.DESC, Events.eventTime to SortOrder.DESC)
            .map { it.toEvent() }
    }

    fun findScheduledReadyToOpen(): List<Event> = transaction {
        val now = Instant.now()
        Events.selectAll()
            .where { (Events.status eq EventStatus.SCHEDULED.name) and (Events.registrationOpensAt lessEq now) }
            .map { it.toEvent() }
    }

    fun create(
        teamId: Long,
        type: EventType,
        title: String?,
        description: String?,
        eventDate: LocalDate,
        eventTime: LocalTime,
        location: String?,
        maxPlayers: Int?,
        registrationOpensAt: Instant,
        createdBy: Long?
    ): Event = transaction {
        val id = Events.insertAndGetId {
            it[Events.teamId] = teamId
            it[Events.type] = type.name
            it[Events.title] = title
            it[Events.description] = description
            it[Events.eventDate] = eventDate
            it[Events.eventTime] = eventTime
            it[Events.location] = location
            it[Events.maxPlayers] = maxPlayers
            it[Events.registrationOpensAt] = registrationOpensAt
            it[Events.createdBy] = createdBy
        }
        findById(id.value)!!
    }

    fun update(
        id: Long,
        title: String?,
        description: String?,
        eventDate: LocalDate,
        eventTime: LocalTime,
        location: String?,
        maxPlayers: Int?,
        registrationOpensAt: Instant
    ): Event? = transaction {
        Events.update({ Events.id eq id }) {
            it[Events.title] = title
            it[Events.description] = description
            it[Events.eventDate] = eventDate
            it[Events.eventTime] = eventTime
            it[Events.location] = location
            it[Events.maxPlayers] = maxPlayers
            it[Events.registrationOpensAt] = registrationOpensAt
        }
        findById(id)
    }

    fun updateStatus(id: Long, status: EventStatus): Event? = transaction {
        Events.update({ Events.id eq id }) {
            it[Events.status] = status.name
        }
        findById(id)
    }

    fun delete(id: Long): Boolean = transaction {
        Events.deleteWhere { Events.id eq id } > 0
    }

    private fun ResultRow.toEvent() = Event(
        id = this[Events.id].value,
        teamId = this[Events.teamId],
        type = EventType.valueOf(this[Events.type]),
        title = this[Events.title],
        description = this[Events.description],
        eventDate = this[Events.eventDate],
        eventTime = this[Events.eventTime],
        location = this[Events.location],
        maxPlayers = this[Events.maxPlayers],
        registrationOpensAt = this[Events.registrationOpensAt],
        status = EventStatus.valueOf(this[Events.status]),
        createdBy = this[Events.createdBy],
        createdAt = this[Events.createdAt]
    )
}

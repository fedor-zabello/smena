package com.smena.repositories

import com.smena.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class LineupRepository {

    fun findById(id: Long): Lineup? = transaction {
        Lineups.selectAll()
            .where { Lineups.id eq id }
            .map { it.toLineup() }
            .singleOrNull()
    }

    fun findAllByEventId(eventId: Long): List<Lineup> = transaction {
        Lineups.selectAll()
            .where { Lineups.eventId eq eventId }
            .orderBy(Lineups.createdAt to SortOrder.ASC)
            .map { it.toLineup() }
    }

    fun create(
        eventId: Long,
        userId: Long,
        lineGroup: LineGroup,
        position: Position?
    ): Lineup = transaction {
        val id = Lineups.insertAndGetId {
            it[Lineups.eventId] = eventId
            it[Lineups.userId] = userId
            it[Lineups.lineGroup] = lineGroup.name
            it[Lineups.position] = position?.name
        }
        findById(id.value)!!
    }

    fun update(
        id: Long,
        lineGroup: LineGroup,
        position: Position?
    ): Lineup? = transaction {
        Lineups.update({ Lineups.id eq id }) {
            it[Lineups.lineGroup] = lineGroup.name
            it[Lineups.position] = position?.name
        }
        findById(id)
    }

    fun deleteAllByEventId(eventId: Long): Int = transaction {
        Lineups.deleteWhere { Lineups.eventId eq eventId }
    }

    fun delete(id: Long): Boolean = transaction {
        Lineups.deleteWhere { Lineups.id eq id } > 0
    }

    private fun ResultRow.toLineup() = Lineup(
        id = this[Lineups.id].value,
        eventId = this[Lineups.eventId],
        userId = this[Lineups.userId],
        lineGroup = LineGroup.valueOf(this[Lineups.lineGroup]),
        position = this[Lineups.position]?.let { Position.valueOf(it) },
        createdAt = this[Lineups.createdAt]
    )
}

package com.smena.repositories

import com.smena.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class RegistrationRepository {

    fun findByEventId(eventId: Long): List<Registration> = transaction {
        Registrations.selectAll()
            .where { Registrations.eventId eq eventId }
            .orderBy(Registrations.registeredAt to SortOrder.ASC)
            .map { it.toRegistration() }
    }

    fun findByEventAndUser(eventId: Long, userId: Long): Registration? = transaction {
        Registrations.selectAll()
            .where { (Registrations.eventId eq eventId) and (Registrations.userId eq userId) }
            .map { it.toRegistration() }
            .singleOrNull()
    }

    fun upsert(eventId: Long, userId: Long, status: RegistrationStatus): Registration = transaction {
        val existing = findByEventAndUser(eventId, userId)
        if (existing != null) {
            Registrations.update({ (Registrations.eventId eq eventId) and (Registrations.userId eq userId) }) {
                it[Registrations.status] = status.name
                it[Registrations.updatedAt] = Instant.now()
            }
            findByEventAndUser(eventId, userId)!!
        } else {
            val id = Registrations.insertAndGetId {
                it[Registrations.eventId] = eventId
                it[Registrations.userId] = userId
                it[Registrations.status] = status.name
            }
            findById(id.value)!!
        }
    }

    fun delete(eventId: Long, userId: Long): Boolean = transaction {
        Registrations.deleteWhere { (Registrations.eventId eq eventId) and (Registrations.userId eq userId) } > 0
    }

    private fun findById(id: Long): Registration? = transaction {
        Registrations.selectAll()
            .where { Registrations.id eq id }
            .map { it.toRegistration() }
            .singleOrNull()
    }

    private fun ResultRow.toRegistration() = Registration(
        id = this[Registrations.id].value,
        eventId = this[Registrations.eventId],
        userId = this[Registrations.userId],
        status = RegistrationStatus.valueOf(this[Registrations.status]),
        registeredAt = this[Registrations.registeredAt],
        updatedAt = this[Registrations.updatedAt]
    )
}

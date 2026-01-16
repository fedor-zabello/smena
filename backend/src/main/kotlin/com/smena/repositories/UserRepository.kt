package com.smena.repositories

import com.smena.models.User
import com.smena.models.Users
import com.smena.telegram.TelegramUser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun findById(id: Long): User? = transaction {
        Users.selectAll()
            .where { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByTelegramId(telegramId: Long): User? = transaction {
        Users.selectAll()
            .where { Users.telegramId eq telegramId }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByIds(ids: List<Long>): List<User> = transaction {
        if (ids.isEmpty()) return@transaction emptyList()
        Users.selectAll()
            .where { Users.id inList ids }
            .map { it.toUser() }
    }

    fun create(telegramUser: TelegramUser): User = transaction {
        val id = Users.insertAndGetId {
            it[telegramId] = telegramUser.id
            it[firstName] = telegramUser.firstName
            it[lastName] = telegramUser.lastName
            it[username] = telegramUser.username
        }
        findById(id.value)!!
    }

    fun update(userId: Long, telegramUser: TelegramUser): User = transaction {
        Users.update({ Users.id eq userId }) {
            it[firstName] = telegramUser.firstName
            it[lastName] = telegramUser.lastName
            it[username] = telegramUser.username
        }
        findById(userId)!!
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id].value,
        telegramId = this[Users.telegramId],
        firstName = this[Users.firstName],
        lastName = this[Users.lastName],
        username = this[Users.username],
        createdAt = this[Users.createdAt]
    )
}

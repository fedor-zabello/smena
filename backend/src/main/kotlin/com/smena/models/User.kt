package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : LongIdTable("users") {
    val telegramId = long("telegram_id").uniqueIndex()
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255).nullable()
    val username = varchar("username", 255).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

data class User(
    val id: Long,
    val telegramId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val createdAt: Instant
)

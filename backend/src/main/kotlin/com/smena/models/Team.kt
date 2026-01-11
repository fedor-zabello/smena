package com.smena.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Teams : LongIdTable("teams") {
    val name = varchar("name", 255)
    val inviteCode = varchar("invite_code", 8).uniqueIndex()
    val telegramChatId = long("telegram_chat_id").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

data class Team(
    val id: Long,
    val name: String,
    val inviteCode: String,
    val telegramChatId: Long?,
    val createdAt: Instant
)

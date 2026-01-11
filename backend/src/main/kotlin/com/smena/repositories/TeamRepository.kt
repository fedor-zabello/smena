package com.smena.repositories

import com.smena.models.Team
import com.smena.models.TeamMembers
import com.smena.models.Teams
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class TeamRepository {

    fun findById(id: Long): Team? = transaction {
        Teams.selectAll()
            .where { Teams.id eq id }
            .map { it.toTeam() }
            .singleOrNull()
    }

    fun findByInviteCode(code: String): Team? = transaction {
        Teams.selectAll()
            .where { Teams.inviteCode eq code }
            .map { it.toTeam() }
            .singleOrNull()
    }

    fun findAllByUserId(userId: Long): List<Team> = transaction {
        Teams.join(TeamMembers, JoinType.INNER, Teams.id, TeamMembers.teamId)
            .selectAll()
            .where { TeamMembers.userId eq userId }
            .map { it.toTeam() }
    }

    fun create(name: String): Team = transaction {
        val code = generateUniqueInviteCode()
        val id = Teams.insertAndGetId {
            it[Teams.name] = name
            it[inviteCode] = code
        }
        findById(id.value)!!
    }

    fun updateInviteCode(teamId: Long): Team = transaction {
        val code = generateUniqueInviteCode()
        Teams.update({ Teams.id eq teamId }) {
            it[inviteCode] = code
        }
        findById(teamId)!!
    }

    fun updateChatId(teamId: Long, chatId: Long?): Team = transaction {
        Teams.update({ Teams.id eq teamId }) {
            it[telegramChatId] = chatId
        }
        findById(teamId)!!
    }

    private fun generateUniqueInviteCode(): String {
        var code: String
        do {
            code = generateInviteCode()
        } while (findByInviteCode(code) != null)
        return code
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    private fun ResultRow.toTeam() = Team(
        id = this[Teams.id].value,
        name = this[Teams.name],
        inviteCode = this[Teams.inviteCode],
        telegramChatId = this[Teams.telegramChatId],
        createdAt = this[Teams.createdAt]
    )
}

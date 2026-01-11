package com.smena.repositories

import com.smena.models.TeamMember
import com.smena.models.TeamMembers
import com.smena.models.TeamRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class TeamMemberRepository {

    fun findById(id: Long): TeamMember? = transaction {
        TeamMembers.selectAll()
            .where { TeamMembers.id eq id }
            .map { it.toTeamMember() }
            .singleOrNull()
    }

    fun findByUserAndTeam(userId: Long, teamId: Long): TeamMember? = transaction {
        TeamMembers.selectAll()
            .where { (TeamMembers.userId eq userId) and (TeamMembers.teamId eq teamId) }
            .map { it.toTeamMember() }
            .singleOrNull()
    }

    fun findAllByTeamId(teamId: Long): List<TeamMember> = transaction {
        TeamMembers.selectAll()
            .where { TeamMembers.teamId eq teamId }
            .map { it.toTeamMember() }
    }

    fun findAllByUserId(userId: Long): List<TeamMember> = transaction {
        TeamMembers.selectAll()
            .where { TeamMembers.userId eq userId }
            .map { it.toTeamMember() }
    }

    fun create(userId: Long, teamId: Long, role: TeamRole): TeamMember = transaction {
        val id = TeamMembers.insertAndGetId {
            it[TeamMembers.userId] = userId
            it[TeamMembers.teamId] = teamId
            it[TeamMembers.role] = role.name
        }
        findById(id.value)!!
    }

    fun updateRole(id: Long, role: TeamRole): TeamMember = transaction {
        TeamMembers.update({ TeamMembers.id eq id }) {
            it[TeamMembers.role] = role.name
        }
        findById(id)!!
    }

    fun delete(id: Long): Unit = transaction {
        TeamMembers.deleteWhere { TeamMembers.id eq id }
    }

    fun deleteByUserAndTeam(userId: Long, teamId: Long): Unit = transaction {
        TeamMembers.deleteWhere { (TeamMembers.userId eq userId) and (TeamMembers.teamId eq teamId) }
    }

    private fun ResultRow.toTeamMember() = TeamMember(
        id = this[TeamMembers.id].value,
        userId = this[TeamMembers.userId],
        teamId = this[TeamMembers.teamId],
        role = TeamRole.valueOf(this[TeamMembers.role]),
        joinedAt = this[TeamMembers.joinedAt]
    )
}

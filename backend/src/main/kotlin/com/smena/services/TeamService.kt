package com.smena.services

import com.smena.dto.TeamResponse
import com.smena.dto.toResponse
import com.smena.exceptions.AlreadyTeamMemberException
import com.smena.exceptions.ForbiddenException
import com.smena.exceptions.InvalidInviteCodeException
import com.smena.exceptions.TeamNotFoundException
import com.smena.models.TeamRole
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository

class TeamService(
    private val teamRepository: TeamRepository = TeamRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository()
) {

    fun createTeam(name: String, userId: Long): TeamResponse {
        val team = teamRepository.create(name)
        teamMemberRepository.create(userId, team.id, TeamRole.ADMIN)
        return team.toResponse(role = TeamRole.ADMIN.name, memberCount = 1)
    }

    fun getUserTeams(userId: Long): List<TeamResponse> {
        val memberships = teamMemberRepository.findAllByUserId(userId)
        return memberships.map { membership ->
            val team = teamRepository.findById(membership.teamId)
                ?: throw TeamNotFoundException("Team ${membership.teamId} not found")
            val memberCount = teamMemberRepository.findAllByTeamId(team.id).size
            team.toResponse(role = membership.role.name, memberCount = memberCount)
        }
    }

    fun getTeamById(teamId: Long, userId: Long): TeamResponse {
        val team = teamRepository.findById(teamId)
            ?: throw TeamNotFoundException()

        val membership = teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        val memberCount = teamMemberRepository.findAllByTeamId(teamId).size
        return team.toResponse(role = membership.role.name, memberCount = memberCount)
    }

    fun joinTeam(inviteCode: String, userId: Long): TeamResponse {
        val team = teamRepository.findByInviteCode(inviteCode)
            ?: throw InvalidInviteCodeException()

        val existingMembership = teamMemberRepository.findByUserAndTeam(userId, team.id)
        if (existingMembership != null) {
            throw AlreadyTeamMemberException()
        }

        teamMemberRepository.create(userId, team.id, TeamRole.PLAYER)
        val memberCount = teamMemberRepository.findAllByTeamId(team.id).size
        return team.toResponse(role = TeamRole.PLAYER.name, memberCount = memberCount)
    }

    fun regenerateInviteCode(teamId: Long, userId: Long): TeamResponse {
        val membership = teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        if (membership.role != TeamRole.ADMIN) {
            throw ForbiddenException("Only team admins can regenerate invite code")
        }

        val team = teamRepository.updateInviteCode(teamId)
        val memberCount = teamMemberRepository.findAllByTeamId(teamId).size
        return team.toResponse(role = membership.role.name, memberCount = memberCount)
    }
}

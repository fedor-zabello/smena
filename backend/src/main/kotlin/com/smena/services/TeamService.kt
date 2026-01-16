package com.smena.services

import com.smena.dto.MemberResponse
import com.smena.dto.TeamResponse
import com.smena.dto.toResponse
import com.smena.exceptions.*
import com.smena.models.TeamRole
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository

class TeamService(
    private val teamRepository: TeamRepository = TeamRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository(),
    private val userRepository: UserRepository = UserRepository()
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

    fun getTeamMembers(teamId: Long, userId: Long): List<MemberResponse> {
        teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        val members = teamMemberRepository.findAllByTeamId(teamId)
        val userIds = members.map { it.userId }
        val users = userRepository.findByIds(userIds).associateBy { it.id }

        return members.map { member ->
            val user = users[member.userId]
            MemberResponse(
                id = member.id,
                userId = member.userId,
                firstName = user?.firstName ?: "",
                lastName = user?.lastName,
                username = user?.username,
                role = member.role.name,
                joinedAt = member.joinedAt.toString()
            )
        }
    }

    fun updateMemberRole(teamId: Long, targetUserId: Long, currentUserId: Long, role: String): MemberResponse {
        val currentMembership = teamMemberRepository.findByUserAndTeam(currentUserId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        if (currentMembership.role != TeamRole.ADMIN) {
            throw ForbiddenException("Only team admins can change member roles")
        }

        val newRole = try {
            TeamRole.valueOf(role)
        } catch (e: IllegalArgumentException) {
            throw InvalidRoleException("Invalid role: $role. Valid roles: PLAYER, COACH, ADMIN")
        }

        val targetMembership = teamMemberRepository.findByUserAndTeam(targetUserId, teamId)
            ?: throw MemberNotFoundException()

        // Check if demoting the last admin
        if (targetMembership.role == TeamRole.ADMIN && newRole != TeamRole.ADMIN) {
            val adminCount = teamMemberRepository.findAllByTeamId(teamId)
                .count { it.role == TeamRole.ADMIN }
            if (adminCount <= 1) {
                throw CannotRemoveLastAdminException("Cannot demote the last admin")
            }
        }

        val updatedMember = teamMemberRepository.updateRole(targetMembership.id, newRole)
        val user = userRepository.findById(targetUserId)

        return MemberResponse(
            id = updatedMember.id,
            userId = updatedMember.userId,
            firstName = user?.firstName ?: "",
            lastName = user?.lastName,
            username = user?.username,
            role = updatedMember.role.name,
            joinedAt = updatedMember.joinedAt.toString()
        )
    }

    fun removeMember(teamId: Long, targetUserId: Long, currentUserId: Long) {
        val currentMembership = teamMemberRepository.findByUserAndTeam(currentUserId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        if (currentMembership.role != TeamRole.ADMIN) {
            throw ForbiddenException("Only team admins can remove members")
        }

        val targetMembership = teamMemberRepository.findByUserAndTeam(targetUserId, teamId)
            ?: throw MemberNotFoundException()

        // Check if removing the last admin
        if (targetMembership.role == TeamRole.ADMIN) {
            val adminCount = teamMemberRepository.findAllByTeamId(teamId)
                .count { it.role == TeamRole.ADMIN }
            if (adminCount <= 1) {
                throw CannotRemoveLastAdminException()
            }
        }

        teamMemberRepository.delete(targetMembership.id)
    }

    fun leaveTeam(teamId: Long, userId: Long) {
        val membership = teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        // Check if leaving as last admin
        if (membership.role == TeamRole.ADMIN) {
            val adminCount = teamMemberRepository.findAllByTeamId(teamId)
                .count { it.role == TeamRole.ADMIN }
            if (adminCount <= 1) {
                throw CannotRemoveLastAdminException("Cannot leave as the last admin. Transfer admin role first.")
            }
        }

        teamMemberRepository.delete(membership.id)
    }
}

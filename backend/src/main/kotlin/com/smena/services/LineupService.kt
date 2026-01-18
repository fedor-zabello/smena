package com.smena.services

import com.smena.dto.LineupEntryRequest
import com.smena.dto.LineupEntryResponse
import com.smena.dto.LineupResponse
import com.smena.exceptions.*
import com.smena.models.*
import com.smena.repositories.*

class LineupService(
    private val lineupRepository: LineupRepository = LineupRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val registrationRepository: RegistrationRepository = RegistrationRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    fun getLineup(eventId: Long, userId: Long): LineupResponse {
        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        requireTeamMember(event.teamId, userId)

        val lineups = lineupRepository.findAllByEventId(eventId)
        val userIds = lineups.map { it.userId }
        val users = userRepository.findByIds(userIds).associateBy { it.id }

        val responses = lineups.mapNotNull { lineup ->
            users[lineup.userId]?.let { user ->
                lineup.toResponse(user)
            }
        }

        return LineupResponse(entries = responses)
    }

    fun saveLineup(
        eventId: Long,
        userId: Long,
        entries: List<LineupEntryRequest>
    ): LineupResponse {
        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        requireCoach(event.teamId, userId)

        // Validate all entries before saving
        validateLineupEntries(eventId, event.type, entries)

        // Delete old lineup and create new one
        lineupRepository.deleteAllByEventId(eventId)

        val createdLineups = entries.map { entry ->
            val lineGroup = parseLineGroup(entry.lineGroup)
            val position = entry.position?.let { parsePosition(it) }
            lineupRepository.create(eventId, entry.userId, lineGroup, position)
        }

        val userIds = createdLineups.map { it.userId }
        val users = userRepository.findByIds(userIds).associateBy { it.id }

        val responses = createdLineups.mapNotNull { lineup ->
            users[lineup.userId]?.let { user ->
                lineup.toResponse(user)
            }
        }

        return LineupResponse(entries = responses)
    }

    private fun validateLineupEntries(
        eventId: Long,
        eventType: EventType,
        entries: List<LineupEntryRequest>
    ) {
        // Get all registrations for this event with GOING status
        val registrations = registrationRepository.findByEventId(eventId)
            .filter { it.status == RegistrationStatus.GOING }
        val goingUserIds = registrations.map { it.userId }.toSet()

        // Validate each entry
        entries.forEach { entry ->
            // Check if user is registered with GOING status
            if (entry.userId !in goingUserIds) {
                throw InvalidLineupException(
                    "User ${entry.userId} is not registered for this event with GOING status"
                )
            }

            // Validate line group
            val lineGroup = parseLineGroup(entry.lineGroup)
            if (!lineGroup.isValidFor(eventType)) {
                throw InvalidLineGroupException(
                    "Line group ${entry.lineGroup} is not valid for event type $eventType"
                )
            }
        }
    }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")
    }

    private fun requireCoach(teamId: Long, userId: Long) {
        val membership = teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        if (membership.role != TeamRole.COACH) {
            throw ForbiddenException("Only coaches can manage lineups")
        }
    }

    private fun parseLineGroup(lineGroup: String): LineGroup {
        return try {
            LineGroup.valueOf(lineGroup.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidLineGroupException("Invalid line group: $lineGroup")
        }
    }

    private fun parsePosition(position: String): Position {
        return try {
            Position.valueOf(position.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidLineupException("Invalid position: $position. Valid positions: LW, C, RW, LD, RD, G")
        }
    }

    private fun Lineup.toResponse(user: User) = LineupEntryResponse(
        id = id,
        eventId = eventId,
        userId = userId,
        firstName = user.firstName,
        lastName = user.lastName,
        username = user.username,
        lineGroup = lineGroup.name,
        position = position?.name,
        createdAt = createdAt.toString()
    )
}

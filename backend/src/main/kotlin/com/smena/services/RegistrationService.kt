package com.smena.services

import com.smena.dto.RegistrationListResponse
import com.smena.dto.RegistrationResponse
import com.smena.exceptions.*
import com.smena.models.EventStatus
import com.smena.models.Registration
import com.smena.models.RegistrationStatus
import com.smena.models.User
import com.smena.repositories.EventRepository
import com.smena.repositories.RegistrationRepository
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.UserRepository

class RegistrationService(
    private val registrationRepository: RegistrationRepository = RegistrationRepository(),
    private val eventRepository: EventRepository = EventRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    fun getEventRegistrations(eventId: Long, userId: Long): RegistrationListResponse {
        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        requireTeamMember(event.teamId, userId)

        val registrations = registrationRepository.findByEventId(eventId)
        val userIds = registrations.map { it.userId }
        val users = userRepository.findByIds(userIds).associateBy { it.id }

        val responses = registrations.mapNotNull { registration ->
            users[registration.userId]?.let { user ->
                registration.toResponse(user)
            }
        }

        return RegistrationListResponse(registrations = responses)
    }

    fun register(eventId: Long, userId: Long, statusStr: String): RegistrationResponse {
        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        requireTeamMember(event.teamId, userId)

        if (event.status != EventStatus.OPEN) {
            throw RegistrationNotOpenException("Registration is not open for this event. Current status: ${event.status}")
        }

        val status = parseStatus(statusStr)
        val registration = registrationRepository.upsert(eventId, userId, status)
        val user = userRepository.findById(userId)
            ?: throw RuntimeException("User not found")

        return registration.toResponse(user)
    }

    fun unregister(eventId: Long, userId: Long) {
        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        requireTeamMember(event.teamId, userId)

        val deleted = registrationRepository.delete(eventId, userId)
        if (!deleted) {
            throw RegistrationNotFoundException()
        }
    }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")
    }

    private fun parseStatus(status: String): RegistrationStatus {
        return try {
            RegistrationStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidRegistrationStatusException("Invalid registration status: $status. Valid values: GOING, NOT_GOING")
        }
    }

    private fun Registration.toResponse(user: User) = RegistrationResponse(
        id = id,
        eventId = eventId,
        userId = userId,
        firstName = user.firstName,
        lastName = user.lastName,
        username = user.username,
        status = status.name,
        registeredAt = registeredAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

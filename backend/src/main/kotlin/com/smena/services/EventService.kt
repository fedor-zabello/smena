package com.smena.services

import com.smena.dto.EventListResponse
import com.smena.dto.EventResponse
import com.smena.exceptions.*
import com.smena.models.Event
import com.smena.models.EventStatus
import com.smena.models.EventType
import com.smena.models.TeamRole
import com.smena.repositories.EventRepository
import com.smena.repositories.TeamMemberRepository
import java.time.*
import java.time.format.DateTimeParseException

class EventService(
    private val eventRepository: EventRepository = EventRepository(),
    private val teamMemberRepository: TeamMemberRepository = TeamMemberRepository()
) {

    fun createEvent(
        teamId: Long,
        userId: Long,
        type: String,
        title: String?,
        description: String?,
        eventDate: String,
        eventTime: String,
        location: String?,
        maxPlayers: Int?
    ): EventResponse {
        requireCoachOrAdmin(teamId, userId)

        val eventType = parseEventType(type)
        val date = parseDate(eventDate)
        val time = parseTime(eventTime)
        val registrationOpensAt = calculateRegistrationOpensAt(date)

        val event = eventRepository.create(
            teamId = teamId,
            type = eventType,
            title = title,
            description = description,
            eventDate = date,
            eventTime = time,
            location = location,
            maxPlayers = maxPlayers,
            registrationOpensAt = registrationOpensAt,
            createdBy = userId
        )

        return event.toResponse()
    }

    fun getTeamEvents(teamId: Long, userId: Long): EventListResponse {
        requireTeamMember(teamId, userId)

        val upcoming = eventRepository.findUpcomingByTeamId(teamId).map { it.toResponse() }
        val past = eventRepository.findPastByTeamId(teamId).map { it.toResponse() }

        return EventListResponse(upcoming = upcoming, past = past)
    }

    fun getEventById(teamId: Long, eventId: Long, userId: Long): EventResponse {
        requireTeamMember(teamId, userId)

        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        if (event.teamId != teamId) {
            throw EventNotFoundException()
        }

        return event.toResponse()
    }

    fun updateEvent(
        teamId: Long,
        eventId: Long,
        userId: Long,
        title: String?,
        description: String?,
        eventDate: String?,
        eventTime: String?,
        location: String?,
        maxPlayers: Int?
    ): EventResponse {
        requireCoachOrAdmin(teamId, userId)

        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        if (event.teamId != teamId) {
            throw EventNotFoundException()
        }

        val newDate = eventDate?.let { parseDate(it) } ?: event.eventDate
        val newTime = eventTime?.let { parseTime(it) } ?: event.eventTime
        val newRegistrationOpensAt = if (eventDate != null) {
            calculateRegistrationOpensAt(newDate)
        } else {
            event.registrationOpensAt
        }

        val updatedEvent = eventRepository.update(
            id = eventId,
            title = title ?: event.title,
            description = description ?: event.description,
            eventDate = newDate,
            eventTime = newTime,
            location = location ?: event.location,
            maxPlayers = maxPlayers ?: event.maxPlayers,
            registrationOpensAt = newRegistrationOpensAt
        ) ?: throw EventNotFoundException()

        return updatedEvent.toResponse()
    }

    fun cancelEvent(teamId: Long, eventId: Long, userId: Long): EventResponse {
        requireCoachOrAdmin(teamId, userId)

        val event = eventRepository.findById(eventId)
            ?: throw EventNotFoundException()

        if (event.teamId != teamId) {
            throw EventNotFoundException()
        }

        val cancelledEvent = eventRepository.updateStatus(eventId, EventStatus.CANCELLED)
            ?: throw EventNotFoundException()

        return cancelledEvent.toResponse()
    }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")
    }

    private fun requireCoachOrAdmin(teamId: Long, userId: Long) {
        val membership = teamMemberRepository.findByUserAndTeam(userId, teamId)
            ?: throw ForbiddenException("You are not a member of this team")

        if (membership.role != TeamRole.COACH && membership.role != TeamRole.ADMIN) {
            throw ForbiddenException("Only coaches and admins can manage events")
        }
    }

    private fun parseEventType(type: String): EventType {
        return try {
            EventType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidEventTypeException("Invalid event type: $type. Valid types: GAME, TRAINING")
        }
    }

    private fun parseDate(date: String): LocalDate {
        return try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw InvalidEventDateException("Invalid date format: $date. Expected format: YYYY-MM-DD")
        }
    }

    private fun parseTime(time: String): LocalTime {
        return try {
            LocalTime.parse(time)
        } catch (e: DateTimeParseException) {
            throw InvalidEventDateException("Invalid time format: $time. Expected format: HH:MM")
        }
    }

    private fun calculateRegistrationOpensAt(eventDate: LocalDate): Instant {
        val dayBefore = eventDate.minusDays(1)
        val openTime = LocalTime.of(8, 0)
        return dayBefore.atTime(openTime).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun Event.toResponse() = EventResponse(
        id = id,
        teamId = teamId,
        type = type.name,
        title = title,
        description = description,
        eventDate = eventDate.toString(),
        eventTime = eventTime.toString(),
        location = location,
        maxPlayers = maxPlayers,
        registrationOpensAt = registrationOpensAt.toString(),
        status = status.name,
        createdBy = createdBy,
        createdAt = createdAt.toString()
    )
}

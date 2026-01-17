package com.smena.unit.services

import com.smena.exceptions.*
import com.smena.models.*
import com.smena.repositories.EventRepository
import com.smena.repositories.TeamMemberRepository
import com.smena.services.EventService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventServiceTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var teamMemberRepository: TeamMemberRepository
    private lateinit var eventService: EventService

    private val now = Instant.now()
    private val tomorrow = LocalDate.now().plusDays(1)

    @BeforeEach
    fun setUp() {
        eventRepository = mockk()
        teamMemberRepository = mockk()
        eventService = EventService(eventRepository, teamMemberRepository)
    }

    @Test
    fun `createEvent creates event for coach`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.COACH, now)
        val event = Event(
            id = 1L,
            teamId = 1L,
            type = EventType.GAME,
            title = "Match vs Bears",
            description = null,
            eventDate = tomorrow,
            eventTime = LocalTime.of(19, 0),
            location = "Ice Arena",
            maxPlayers = 20,
            registrationOpensAt = now,
            status = EventStatus.SCHEDULED,
            createdBy = 100L,
            createdAt = now
        )

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns event

        val result = eventService.createEvent(
            teamId = 1L,
            userId = 100L,
            type = "GAME",
            title = "Match vs Bears",
            description = null,
            eventDate = tomorrow.toString(),
            eventTime = "19:00",
            location = "Ice Arena",
            maxPlayers = 20
        )

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("GAME", result.type)
        assertEquals("Match vs Bears", result.title)
    }

    @Test
    fun `createEvent creates event for admin`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)
        val event = Event(
            id = 1L,
            teamId = 1L,
            type = EventType.TRAINING,
            title = null,
            description = null,
            eventDate = tomorrow,
            eventTime = LocalTime.of(20, 0),
            location = null,
            maxPlayers = null,
            registrationOpensAt = now,
            status = EventStatus.SCHEDULED,
            createdBy = 100L,
            createdAt = now
        )

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns event

        val result = eventService.createEvent(
            teamId = 1L,
            userId = 100L,
            type = "TRAINING",
            title = null,
            description = null,
            eventDate = tomorrow.toString(),
            eventTime = "20:00",
            location = null,
            maxPlayers = null
        )

        assertNotNull(result)
        assertEquals("TRAINING", result.type)
    }

    @Test
    fun `createEvent throws ForbiddenException for player`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<ForbiddenException> {
            eventService.createEvent(
                teamId = 1L,
                userId = 100L,
                type = "GAME",
                title = "Match",
                description = null,
                eventDate = tomorrow.toString(),
                eventTime = "19:00",
                location = null,
                maxPlayers = null
            )
        }
    }

    @Test
    fun `createEvent throws ForbiddenException for non-member`() {
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns null

        assertThrows<ForbiddenException> {
            eventService.createEvent(
                teamId = 1L,
                userId = 100L,
                type = "GAME",
                title = "Match",
                description = null,
                eventDate = tomorrow.toString(),
                eventTime = "19:00",
                location = null,
                maxPlayers = null
            )
        }
    }

    @Test
    fun `createEvent throws InvalidEventTypeException for invalid type`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.COACH, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<InvalidEventTypeException> {
            eventService.createEvent(
                teamId = 1L,
                userId = 100L,
                type = "INVALID_TYPE",
                title = "Match",
                description = null,
                eventDate = tomorrow.toString(),
                eventTime = "19:00",
                location = null,
                maxPlayers = null
            )
        }
    }

    @Test
    fun `getEventById returns event for team member`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)
        val event = Event(
            id = 10L,
            teamId = 1L,
            type = EventType.GAME,
            title = "Match",
            description = null,
            eventDate = tomorrow,
            eventTime = LocalTime.of(19, 0),
            location = "Arena",
            maxPlayers = null,
            registrationOpensAt = now,
            status = EventStatus.OPEN,
            createdBy = 50L,
            createdAt = now
        )

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.findById(10L) } returns event

        val result = eventService.getEventById(1L, 10L, 100L)

        assertEquals(10L, result.id)
        assertEquals("GAME", result.type)
        assertEquals("OPEN", result.status)
    }

    @Test
    fun `getEventById throws EventNotFoundException for unknown event`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.findById(999L) } returns null

        assertThrows<EventNotFoundException> {
            eventService.getEventById(1L, 999L, 100L)
        }
    }

    @Test
    fun `getEventById throws EventNotFoundException for event from different team`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)
        val event = Event(
            id = 10L,
            teamId = 999L,  // Different team
            type = EventType.GAME,
            title = "Match",
            description = null,
            eventDate = tomorrow,
            eventTime = LocalTime.of(19, 0),
            location = null,
            maxPlayers = null,
            registrationOpensAt = now,
            status = EventStatus.SCHEDULED,
            createdBy = 50L,
            createdAt = now
        )

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.findById(10L) } returns event

        assertThrows<EventNotFoundException> {
            eventService.getEventById(1L, 10L, 100L)
        }
    }

    @Test
    fun `cancelEvent changes status to CANCELLED`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)
        val event = Event(
            id = 10L,
            teamId = 1L,
            type = EventType.GAME,
            title = "Match",
            description = null,
            eventDate = tomorrow,
            eventTime = LocalTime.of(19, 0),
            location = null,
            maxPlayers = null,
            registrationOpensAt = now,
            status = EventStatus.SCHEDULED,
            createdBy = 50L,
            createdAt = now
        )
        val cancelledEvent = event.copy(status = EventStatus.CANCELLED)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { eventRepository.findById(10L) } returns event
        every { eventRepository.updateStatus(10L, EventStatus.CANCELLED) } returns cancelledEvent

        val result = eventService.cancelEvent(1L, 10L, 100L)

        assertEquals("CANCELLED", result.status)
    }

    @Test
    fun `cancelEvent throws ForbiddenException for player`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<ForbiddenException> {
            eventService.cancelEvent(1L, 10L, 100L)
        }
    }
}

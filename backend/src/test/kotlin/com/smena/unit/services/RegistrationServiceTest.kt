package com.smena.unit.services

import com.smena.exceptions.*
import com.smena.models.*
import com.smena.repositories.EventRepository
import com.smena.repositories.RegistrationRepository
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.UserRepository
import com.smena.services.RegistrationService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RegistrationServiceTest {

    private lateinit var registrationRepository: RegistrationRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var teamMemberRepository: TeamMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var registrationService: RegistrationService

    private val now = Instant.now()
    private val tomorrow = LocalDate.now().plusDays(1)

    @BeforeEach
    fun setUp() {
        registrationRepository = mockk()
        eventRepository = mockk()
        teamMemberRepository = mockk()
        userRepository = mockk()
        registrationService = RegistrationService(
            registrationRepository,
            eventRepository,
            teamMemberRepository,
            userRepository
        )
    }

    private fun createEvent(status: EventStatus = EventStatus.OPEN) = Event(
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
        status = status,
        createdBy = 50L,
        createdAt = now
    )

    private fun createUser() = User(
        id = 100L,
        telegramId = 123456L,
        firstName = "Ivan",
        lastName = "Petrov",
        username = "ivanpetrov",
        createdAt = now
    )

    private fun createMember() = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

    private fun createRegistration(status: RegistrationStatus = RegistrationStatus.GOING) = Registration(
        id = 1L,
        eventId = 10L,
        userId = 100L,
        status = status,
        registeredAt = now,
        updatedAt = now
    )

    @Test
    fun `register creates registration for team member when event is OPEN`() {
        val event = createEvent(EventStatus.OPEN)
        val member = createMember()
        val user = createUser()
        val registration = createRegistration(RegistrationStatus.GOING)

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { registrationRepository.upsert(10L, 100L, RegistrationStatus.GOING) } returns registration
        every { userRepository.findById(100L) } returns user

        val result = registrationService.register(10L, 100L, "GOING")

        assertNotNull(result)
        assertEquals(10L, result.eventId)
        assertEquals(100L, result.userId)
        assertEquals("GOING", result.status)
        assertEquals("Ivan", result.firstName)
    }

    @Test
    fun `register throws RegistrationNotOpenException when event is SCHEDULED`() {
        val event = createEvent(EventStatus.SCHEDULED)
        val member = createMember()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<RegistrationNotOpenException> {
            registrationService.register(10L, 100L, "GOING")
        }
    }

    @Test
    fun `register throws RegistrationNotOpenException when event is CANCELLED`() {
        val event = createEvent(EventStatus.CANCELLED)
        val member = createMember()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<RegistrationNotOpenException> {
            registrationService.register(10L, 100L, "GOING")
        }
    }

    @Test
    fun `register throws ForbiddenException for non-team member`() {
        val event = createEvent(EventStatus.OPEN)

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns null

        assertThrows<ForbiddenException> {
            registrationService.register(10L, 100L, "GOING")
        }
    }

    @Test
    fun `register throws EventNotFoundException for unknown event`() {
        every { eventRepository.findById(999L) } returns null

        assertThrows<EventNotFoundException> {
            registrationService.register(999L, 100L, "GOING")
        }
    }

    @Test
    fun `register throws InvalidRegistrationStatusException for invalid status`() {
        val event = createEvent(EventStatus.OPEN)
        val member = createMember()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<InvalidRegistrationStatusException> {
            registrationService.register(10L, 100L, "MAYBE")
        }
    }

    @Test
    fun `register updates existing registration (upsert)`() {
        val event = createEvent(EventStatus.OPEN)
        val member = createMember()
        val user = createUser()
        val registration = createRegistration(RegistrationStatus.NOT_GOING)

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { registrationRepository.upsert(10L, 100L, RegistrationStatus.NOT_GOING) } returns registration
        every { userRepository.findById(100L) } returns user

        val result = registrationService.register(10L, 100L, "NOT_GOING")

        assertEquals("NOT_GOING", result.status)
    }

    @Test
    fun `getEventRegistrations returns list of registrations`() {
        val event = createEvent()
        val member = createMember()
        val user = createUser()
        val registration = createRegistration()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { registrationRepository.findByEventId(10L) } returns listOf(registration)
        every { userRepository.findByIds(listOf(100L)) } returns listOf(user)

        val result = registrationService.getEventRegistrations(10L, 100L)

        assertEquals(1, result.registrations.size)
        assertEquals("GOING", result.registrations[0].status)
        assertEquals("Ivan", result.registrations[0].firstName)
    }

    @Test
    fun `getEventRegistrations throws ForbiddenException for non-member`() {
        val event = createEvent()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns null

        assertThrows<ForbiddenException> {
            registrationService.getEventRegistrations(10L, 100L)
        }
    }

    @Test
    fun `unregister deletes registration`() {
        val event = createEvent()
        val member = createMember()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { registrationRepository.delete(10L, 100L) } returns true

        registrationService.unregister(10L, 100L)

        verify { registrationRepository.delete(10L, 100L) }
    }

    @Test
    fun `unregister throws RegistrationNotFoundException when no registration exists`() {
        val event = createEvent()
        val member = createMember()

        every { eventRepository.findById(10L) } returns event
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { registrationRepository.delete(10L, 100L) } returns false

        assertThrows<RegistrationNotFoundException> {
            registrationService.unregister(10L, 100L)
        }
    }
}

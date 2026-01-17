package com.smena.unit.services

import com.smena.exceptions.InvalidInitDataException
import com.smena.models.TeamMember
import com.smena.models.TeamRole
import com.smena.models.User
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository
import com.smena.services.AuthService
import com.smena.telegram.TelegramUser
import com.smena.utils.TestInitDataGenerator
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var teamRepository: TeamRepository
    private lateinit var teamMemberRepository: TeamMemberRepository
    private lateinit var authService: AuthService

    private val botToken = TestInitDataGenerator.TEST_BOT_TOKEN

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        teamRepository = mockk()
        teamMemberRepository = mockk()
        authService = AuthService(botToken, userRepository, teamRepository, teamMemberRepository)
    }

    @Test
    fun `authenticate creates new user when not exists`() {
        val initData = TestInitDataGenerator.generate(
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov"
        )

        val newUser = User(
            id = 1L,
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov",
            createdAt = Instant.now()
        )

        every { userRepository.findByTelegramId(123456L) } returns null
        every { userRepository.create(any()) } returns newUser
        every { teamMemberRepository.findAllByUserId(1L) } returns emptyList()

        val result = authService.authenticate(initData)

        assertNotNull(result)
        assertEquals(1L, result.user.id)
        assertEquals("Ivan", result.user.firstName)
        assertEquals("Petrov", result.user.lastName)
        assertEquals("ivanpetrov", result.user.username)
        assertEquals(emptyList(), result.teams)

        verify { userRepository.findByTelegramId(123456L) }
        verify { userRepository.create(match { it.id == 123456L && it.firstName == "Ivan" }) }
    }

    @Test
    fun `authenticate returns existing user when found`() {
        val initData = TestInitDataGenerator.generate(
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov"
        )

        val existingUser = User(
            id = 1L,
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov",
            createdAt = Instant.now()
        )

        every { userRepository.findByTelegramId(123456L) } returns existingUser
        every { teamMemberRepository.findAllByUserId(1L) } returns emptyList()

        val result = authService.authenticate(initData)

        assertNotNull(result)
        assertEquals(1L, result.user.id)

        verify { userRepository.findByTelegramId(123456L) }
        verify(exactly = 0) { userRepository.create(any()) }
        verify(exactly = 0) { userRepository.update(any(), any()) }
    }

    @Test
    fun `authenticate updates user when data changed`() {
        val initData = TestInitDataGenerator.generate(
            telegramId = 123456L,
            firstName = "Ivan Updated",
            lastName = "Petrov",
            username = "ivanpetrov"
        )

        val existingUser = User(
            id = 1L,
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov",
            createdAt = Instant.now()
        )

        val updatedUser = existingUser.copy(firstName = "Ivan Updated")

        every { userRepository.findByTelegramId(123456L) } returns existingUser
        every { userRepository.update(1L, any()) } returns updatedUser
        every { teamMemberRepository.findAllByUserId(1L) } returns emptyList()

        val result = authService.authenticate(initData)

        assertEquals("Ivan Updated", result.user.firstName)
        verify { userRepository.update(1L, any()) }
    }

    @Test
    fun `authenticate throws InvalidInitDataException for invalid initData`() {
        val invalidInitData = "invalid_data"

        assertThrows<InvalidInitDataException> {
            authService.authenticate(invalidInitData)
        }
    }

    @Test
    fun `authenticate returns user teams`() {
        val initData = TestInitDataGenerator.generate(
            telegramId = 123456L,
            firstName = "Ivan"
        )

        val user = User(
            id = 1L,
            telegramId = 123456L,
            firstName = "Ivan",
            lastName = null,
            username = null,
            createdAt = Instant.now()
        )

        val team = com.smena.models.Team(
            id = 10L,
            name = "Hockey Team",
            inviteCode = "ABC123",
            telegramChatId = null,
            createdAt = Instant.now()
        )

        val membership = TeamMember(
            id = 1L,
            userId = 1L,
            teamId = 10L,
            role = TeamRole.PLAYER,
            joinedAt = Instant.now()
        )

        every { userRepository.findByTelegramId(123456L) } returns user
        every { teamMemberRepository.findAllByUserId(1L) } returns listOf(membership)
        every { teamRepository.findById(10L) } returns team

        val result = authService.authenticate(initData)

        assertEquals(1, result.teams.size)
        assertEquals(10L, result.teams[0].id)
        assertEquals("Hockey Team", result.teams[0].name)
        assertEquals("PLAYER", result.teams[0].role)
    }
}

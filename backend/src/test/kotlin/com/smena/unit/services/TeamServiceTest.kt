package com.smena.unit.services

import com.smena.exceptions.*
import com.smena.models.Team
import com.smena.models.TeamMember
import com.smena.models.TeamRole
import com.smena.models.User
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository
import com.smena.services.TeamService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TeamServiceTest {

    private lateinit var teamRepository: TeamRepository
    private lateinit var teamMemberRepository: TeamMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var teamService: TeamService

    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        teamRepository = mockk()
        teamMemberRepository = mockk()
        userRepository = mockk()
        teamService = TeamService(teamRepository, teamMemberRepository, userRepository)
    }

    @Test
    fun `createTeam creates team and adds user as admin`() {
        val team = Team(1L, "Hockey Team", "ABC123", null, now)
        val member = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)

        every { teamRepository.create("Hockey Team") } returns team
        every { teamMemberRepository.create(100L, 1L, TeamRole.ADMIN) } returns member

        val result = teamService.createTeam("Hockey Team", 100L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Hockey Team", result.name)
        assertEquals("ABC123", result.inviteCode)
        assertEquals("ADMIN", result.role)
        assertEquals(1, result.memberCount)

        verify { teamRepository.create("Hockey Team") }
        verify { teamMemberRepository.create(100L, 1L, TeamRole.ADMIN) }
    }

    @Test
    fun `getTeamById returns team for member`() {
        val team = Team(1L, "Hockey Team", "ABC123", null, now)
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamRepository.findById(1L) } returns team
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { teamMemberRepository.findAllByTeamId(1L) } returns listOf(member)

        val result = teamService.getTeamById(1L, 100L)

        assertEquals(1L, result.id)
        assertEquals("PLAYER", result.role)
    }

    @Test
    fun `getTeamById throws ForbiddenException for non-member`() {
        val team = Team(1L, "Hockey Team", "ABC123", null, now)

        every { teamRepository.findById(1L) } returns team
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns null

        assertThrows<ForbiddenException> {
            teamService.getTeamById(1L, 100L)
        }
    }

    @Test
    fun `getTeamById throws TeamNotFoundException for unknown team`() {
        every { teamRepository.findById(999L) } returns null

        assertThrows<TeamNotFoundException> {
            teamService.getTeamById(999L, 100L)
        }
    }

    @Test
    fun `joinTeam adds user as player`() {
        val team = Team(1L, "Hockey Team", "ABC123", null, now)
        val member = TeamMember(2L, 100L, 1L, TeamRole.PLAYER, now)
        val existingMember = TeamMember(1L, 50L, 1L, TeamRole.ADMIN, now)

        every { teamRepository.findByInviteCode("ABC123") } returns team
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns null
        every { teamMemberRepository.create(100L, 1L, TeamRole.PLAYER) } returns member
        every { teamMemberRepository.findAllByTeamId(1L) } returns listOf(existingMember, member)

        val result = teamService.joinTeam("ABC123", 100L)

        assertEquals(1L, result.id)
        assertEquals("PLAYER", result.role)
        assertEquals(2, result.memberCount)
    }

    @Test
    fun `joinTeam throws InvalidInviteCodeException for unknown code`() {
        every { teamRepository.findByInviteCode("INVALID") } returns null

        assertThrows<InvalidInviteCodeException> {
            teamService.joinTeam("INVALID", 100L)
        }
    }

    @Test
    fun `joinTeam throws AlreadyTeamMemberException for existing member`() {
        val team = Team(1L, "Hockey Team", "ABC123", null, now)
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamRepository.findByInviteCode("ABC123") } returns team
        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<AlreadyTeamMemberException> {
            teamService.joinTeam("ABC123", 100L)
        }
    }

    @Test
    fun `regenerateInviteCode works for admin`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)
        val updatedTeam = Team(1L, "Hockey Team", "NEW123", null, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { teamRepository.updateInviteCode(1L) } returns updatedTeam
        every { teamMemberRepository.findAllByTeamId(1L) } returns listOf(member)

        val result = teamService.regenerateInviteCode(1L, 100L)

        assertEquals("NEW123", result.inviteCode)
    }

    @Test
    fun `regenerateInviteCode throws ForbiddenException for non-admin`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member

        assertThrows<ForbiddenException> {
            teamService.regenerateInviteCode(1L, 100L)
        }
    }

    @Test
    fun `updateMemberRole throws CannotRemoveLastAdminException when demoting last admin`() {
        val adminMember = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)
        val targetMember = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns adminMember
        every { teamMemberRepository.findByUserAndTeam(200L, 1L) } returns targetMember
        every { teamMemberRepository.findAllByTeamId(1L) } returns listOf(targetMember)

        assertThrows<CannotRemoveLastAdminException> {
            teamService.updateMemberRole(1L, 200L, 100L, "PLAYER")
        }
    }

    @Test
    fun `leaveTeam throws CannotRemoveLastAdminException for last admin`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.ADMIN, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        every { teamMemberRepository.findAllByTeamId(1L) } returns listOf(member)

        assertThrows<CannotRemoveLastAdminException> {
            teamService.leaveTeam(1L, 100L)
        }
    }

    @Test
    fun `leaveTeam works for player`() {
        val member = TeamMember(1L, 100L, 1L, TeamRole.PLAYER, now)

        every { teamMemberRepository.findByUserAndTeam(100L, 1L) } returns member
        justRun { teamMemberRepository.delete(1L) }

        teamService.leaveTeam(1L, 100L)

        verify { teamMemberRepository.delete(1L) }
    }
}

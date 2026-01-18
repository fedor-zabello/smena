package com.smena.integration.routes

import com.smena.integration.BaseIntegrationTest
import com.smena.integration.TestDatabaseContainer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LineupRoutesTest : BaseIntegrationTest() {

    private fun setEventStatusToOpen(eventId: Long) {
        TestDatabaseContainer.getConnection().use { conn ->
            conn.prepareStatement("UPDATE events SET status = 'OPEN' WHERE id = ?").use { stmt ->
                stmt.setLong(1, eventId)
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    private fun setUserRoleToCoach(userId: Long, teamId: Long) {
        TestDatabaseContainer.getConnection().use { conn ->
            conn.prepareStatement("UPDATE team_members SET role = 'COACH' WHERE user_id = ? AND team_id = ?").use { stmt ->
                stmt.setLong(1, userId)
                stmt.setLong(2, teamId)
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    @Test
    fun `GET api events lineup returns empty lineup`() = testApp { client ->
        // Create coach user and team
        val coachInitData = generateInitData(telegramId = 1000L, firstName = "Coach")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"name": "Lineup Test Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        // Create event
        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Get lineup
        val response = client.get("/api/events/$eventId/lineup") {
            header("Authorization", "tma $coachInitData")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val entries = body["data"]?.jsonObject?.get("entries")?.jsonArray
        assertNotNull(entries)
        assertEquals(0, entries.size)
    }

    @Test
    fun `PUT api events lineup saves lineup successfully`() = testApp { client ->
        // Create coach user and team
        val coachInitData = generateInitData(telegramId = 1100L, firstName = "Coach")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"name": "Lineup Save Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Create event
        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        setEventStatusToOpen(eventId)

        // Register coach for event
        client.post("/api/events/$eventId/registrations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"status": "GOING"}""")
        }

        // Get coach userId
        val authResponse = client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }
        val coachUserId = Json.parseToJsonElement(authResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("user")?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Change user role to COACH (creator is ADMIN by default)
        setUserRoleToCoach(coachUserId, teamId)

        // Save lineup
        val response = client.put("/api/events/$eventId/lineup") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""
                {
                    "entries": [
                        {
                            "userId": $coachUserId,
                            "lineGroup": "LINE_1",
                            "position": "C"
                        }
                    ]
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val entries = body["data"]?.jsonObject?.get("entries")?.jsonArray
        assertNotNull(entries)
        assertEquals(1, entries.size)
        assertEquals("LINE_1", entries[0].jsonObject["lineGroup"]?.jsonPrimitive?.content)
        assertEquals("C", entries[0].jsonObject["position"]?.jsonPrimitive?.content)
    }

    @Test
    fun `PUT api events lineup fails if not coach`() = testApp { client ->
        // Create coach and team
        val coachInitData = generateInitData(telegramId = 1200L, firstName = "Coach")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"name": "No Permission Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Create event
        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Add player to team
        val playerInitData = generateInitData(telegramId = 1201L, firstName = "Player")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$playerInitData"}""")
        }

        val inviteCode = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("inviteCode")?.jsonPrimitive?.content!!

        client.post("/api/teams/join") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $playerInitData")
            setBody("""{"inviteCode": "$inviteCode"}""")
        }

        // Try to save lineup as player
        val response = client.put("/api/events/$eventId/lineup") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $playerInitData")
            setBody("""{"entries": []}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT api events lineup fails if player not registered with GOING`() = testApp { client ->
        // Create coach user and team
        val coachInitData = generateInitData(telegramId = 1300L, firstName = "Coach")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"name": "Validation Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Create event
        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Get coach userId
        val authResponse = client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$coachInitData"}""")
        }
        val coachUserId = Json.parseToJsonElement(authResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("user")?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Change user role to COACH (creator is ADMIN by default)
        setUserRoleToCoach(coachUserId, teamId)

        // Try to save lineup without registration
        val response = client.put("/api/events/$eventId/lineup") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $coachInitData")
            setBody("""
                {
                    "entries": [
                        {
                            "userId": $coachUserId,
                            "lineGroup": "LINE_1",
                            "position": "C"
                        }
                    ]
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}

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

class RegistrationRoutesTest : BaseIntegrationTest() {

    private fun setEventStatusToOpen(eventId: Long) {
        // Direct DB update to set event status to OPEN for testing
        TestDatabaseContainer.getConnection().use { conn ->
            conn.prepareStatement("UPDATE events SET status = 'OPEN' WHERE id = ?").use { stmt ->
                stmt.setLong(1, eventId)
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    @Test
    fun `POST api events registrations creates registration`() = testApp { client ->
        // Create user and team
        val initData = generateInitData(telegramId = 700L, firstName = "Player")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "Registration Test Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        // Create event
        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        // Set event status to OPEN
        setEventStatusToOpen(eventId)

        // Register for event
        val response = client.post("/api/events/$eventId/registrations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"status": "GOING"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val data = body["data"]?.jsonObject
        assertEquals("GOING", data?.get("status")?.jsonPrimitive?.content)
        assertEquals("Player", data?.get("firstName")?.jsonPrimitive?.content)
    }

    @Test
    fun `GET api events registrations returns list`() = testApp { client ->
        // Setup: create user, team, event
        val initData = generateInitData(telegramId = 800L, firstName = "Viewer")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "List Test Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"type": "TRAINING", "eventDate": "$tomorrow", "eventTime": "20:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        setEventStatusToOpen(eventId)

        // Register
        client.post("/api/events/$eventId/registrations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"status": "GOING"}""")
        }

        // Get registrations
        val response = client.get("/api/events/$eventId/registrations") {
            header("Authorization", "tma $initData")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val registrations = body["data"]?.jsonObject?.get("registrations")?.jsonArray
        assertNotNull(registrations)
        assertEquals(1, registrations.size)
    }

    @Test
    fun `POST api events registrations returns 400 when event not OPEN`() = testApp { client ->
        // Setup
        val initData = generateInitData(telegramId = 900L, firstName = "Early")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "Not Open Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        // Try to register without setting status to OPEN
        val response = client.post("/api/events/$eventId/registrations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"status": "GOING"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val error = body["error"]?.jsonObject
        assertEquals("REGISTRATION_NOT_OPEN", error?.get("code")?.jsonPrimitive?.content)
    }

    @Test
    fun `DELETE api events registrations removes registration`() = testApp { client ->
        // Setup
        val initData = generateInitData(telegramId = 1000L, firstName = "Deleter")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        val teamResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "Delete Test Team"}""")
        }
        val teamId = Json.parseToJsonElement(teamResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        val tomorrow = LocalDate.now().plusDays(1)
        val eventResponse = client.post("/api/teams/$teamId/events") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"type": "GAME", "eventDate": "$tomorrow", "eventTime": "19:00"}""")
        }
        val eventId = Json.parseToJsonElement(eventResponse.bodyAsText())
            .jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long!!

        setEventStatusToOpen(eventId)

        // Register
        client.post("/api/events/$eventId/registrations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"status": "GOING"}""")
        }

        // Delete registration
        val response = client.delete("/api/events/$eventId/registrations") {
            header("Authorization", "tma $initData")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        // Verify empty list
        val listResponse = client.get("/api/events/$eventId/registrations") {
            header("Authorization", "tma $initData")
        }
        val body = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val registrations = body["data"]?.jsonObject?.get("registrations")?.jsonArray
        assertEquals(0, registrations?.size)
    }
}

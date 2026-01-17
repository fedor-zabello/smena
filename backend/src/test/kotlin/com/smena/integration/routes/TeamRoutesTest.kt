package com.smena.integration.routes

import com.smena.integration.BaseIntegrationTest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TeamRoutesTest : BaseIntegrationTest() {

    @Test
    fun `POST api teams creates team`() = testApp { client ->
        // First authenticate
        val initData = generateInitData(telegramId = 100L, firstName = "Admin")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        // Create team
        val response = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "Hockey Stars"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val data = body["data"]?.jsonObject
        assertNotNull(data)
        assertEquals("Hockey Stars", data["name"]?.jsonPrimitive?.content)
        assertEquals("ADMIN", data["role"]?.jsonPrimitive?.content)
        assertEquals(1, data["memberCount"]?.jsonPrimitive?.int)
        assertNotNull(data["inviteCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET api teams returns user teams`() = testApp { client ->
        val initData = generateInitData(telegramId = 200L, firstName = "Player")

        // Authenticate
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        // Create team
        client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $initData")
            setBody("""{"name": "My Team"}""")
        }

        // Get teams
        val response = client.get("/api/teams") {
            header("Authorization", "tma $initData")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val teams = body["data"]?.jsonArray
        assertNotNull(teams)
        assertEquals(1, teams.size)
        assertEquals("My Team", teams[0].jsonObject["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST api teams join adds user to team`() = testApp { client ->
        // Admin creates team
        val adminInitData = generateInitData(telegramId = 300L, firstName = "Admin")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$adminInitData"}""")
        }

        val createResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $adminInitData")
            setBody("""{"name": "Join Test Team"}""")
        }

        val createBody = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val inviteCode = createBody["data"]?.jsonObject?.get("inviteCode")?.jsonPrimitive?.content

        // Player joins team
        val playerInitData = generateInitData(telegramId = 400L, firstName = "Player")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$playerInitData"}""")
        }

        val joinResponse = client.post("/api/teams/join") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $playerInitData")
            setBody("""{"inviteCode": "$inviteCode"}""")
        }

        assertEquals(HttpStatusCode.OK, joinResponse.status)

        val joinBody = Json.parseToJsonElement(joinResponse.bodyAsText()).jsonObject
        val data = joinBody["data"]?.jsonObject
        assertEquals("PLAYER", data?.get("role")?.jsonPrimitive?.content)
        assertEquals(2, data?.get("memberCount")?.jsonPrimitive?.int)
    }

    @Test
    fun `GET api teams id returns 403 for non-member`() = testApp { client ->
        // Admin creates team
        val adminInitData = generateInitData(telegramId = 500L, firstName = "Admin")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$adminInitData"}""")
        }

        val createResponse = client.post("/api/teams") {
            contentType(ContentType.Application.Json)
            header("Authorization", "tma $adminInitData")
            setBody("""{"name": "Private Team"}""")
        }

        val createBody = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val teamId = createBody["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

        // Non-member tries to access
        val otherInitData = generateInitData(telegramId = 600L, firstName = "Other")
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$otherInitData"}""")
        }

        val response = client.get("/api/teams/$teamId") {
            header("Authorization", "tma $otherInitData")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `requests without auth return 401`() = testApp { client ->
        val response = client.get("/api/teams")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

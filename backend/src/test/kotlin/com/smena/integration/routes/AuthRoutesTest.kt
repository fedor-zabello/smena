package com.smena.integration.routes

import com.smena.integration.BaseIntegrationTest
import com.smena.utils.TestInitDataGenerator
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRoutesTest : BaseIntegrationTest() {

    @Test
    fun `POST api auth init creates new user`() = testApp { client ->
        val initData = generateInitData(
            telegramId = 111222L,
            firstName = "NewUser",
            lastName = "Test",
            username = "newuser"
        )

        val response = client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val data = body["data"]?.jsonObject
        assertNotNull(data)

        val user = data["user"]?.jsonObject
        assertNotNull(user)
        assertEquals(111222L, user["telegramId"]?.jsonPrimitive?.long)
        assertEquals("NewUser", user["firstName"]?.jsonPrimitive?.content)
        assertEquals("Test", user["lastName"]?.jsonPrimitive?.content)
        assertEquals("newuser", user["username"]?.jsonPrimitive?.content)

        val teams = data["teams"]?.jsonArray
        assertNotNull(teams)
        assertEquals(0, teams.size)
    }

    @Test
    fun `POST api auth init returns existing user`() = testApp { client ->
        val initData = generateInitData(
            telegramId = 333444L,
            firstName = "Existing"
        )

        // First call creates user
        client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        // Second call returns same user
        val response = client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "$initData"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val user = body["data"]?.jsonObject?.get("user")?.jsonObject
        assertEquals(333444L, user?.get("telegramId")?.jsonPrimitive?.long)
    }

    @Test
    fun `POST api auth init returns 401 for invalid initData`() = testApp { client ->
        val response = client.post("/api/auth/init") {
            contentType(ContentType.Application.Json)
            setBody("""{"initData": "invalid_data"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val error = body["error"]?.jsonObject
        assertEquals("INVALID_INIT_DATA", error?.get("code")?.jsonPrimitive?.content)
    }
}

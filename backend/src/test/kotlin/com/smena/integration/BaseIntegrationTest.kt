package com.smena.integration

import com.smena.plugins.*
import com.smena.routes.authRoutes
import com.smena.routes.eventRoutes
import com.smena.routes.lineupRoutes
import com.smena.routes.registrationRoutes
import com.smena.routes.teamRoutes
import com.smena.services.AuthService
import com.smena.services.EventService
import com.smena.services.LineupService
import com.smena.services.RegistrationService
import com.smena.services.TeamService
import com.smena.utils.TestInitDataGenerator
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class BaseIntegrationTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun startDatabase() {
            TestDatabaseContainer.start()
        }
    }

    @BeforeEach
    fun cleanUp() {
        TestDatabaseContainer.cleanDatabase()
    }

    fun testApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application {
            configureContentNegotiation()
            configureCORS()
            configureStatusPages()
            configureAuthentication(TestInitDataGenerator.TEST_BOT_TOKEN)

            val authService = AuthService(TestInitDataGenerator.TEST_BOT_TOKEN)
            val teamService = TeamService()
            val eventService = EventService()
            val registrationService = RegistrationService()
            val lineupService = LineupService()

            routing {
                route("/api") {
                    authRoutes(authService)

                    authenticate("tma") {
                        teamRoutes(teamService)
                        eventRoutes(eventService)
                        registrationRoutes(registrationService)
                        lineupRoutes(lineupService)
                    }
                }
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        block(client)
    }

    protected fun HttpRequestBuilder.withAuth(
        telegramId: Long = 123456L,
        firstName: String = "Test",
        lastName: String? = "User",
        username: String? = "testuser"
    ) {
        val initData = TestInitDataGenerator.generate(
            telegramId = telegramId,
            firstName = firstName,
            lastName = lastName,
            username = username
        )
        header("Authorization", "tma $initData")
    }

    protected fun generateInitData(
        telegramId: Long = 123456L,
        firstName: String = "Test",
        lastName: String? = "User",
        username: String? = "testuser"
    ): String = TestInitDataGenerator.generate(
        telegramId = telegramId,
        firstName = firstName,
        lastName = lastName,
        username = username
    )
}

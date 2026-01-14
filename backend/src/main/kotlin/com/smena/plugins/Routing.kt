package com.smena.plugins

import com.smena.dto.SuccessResponse
import com.smena.routes.authRoutes
import com.smena.routes.teamRoutes
import com.smena.services.AuthService
import com.smena.services.TeamService
import io.ktor.server.auth.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    val botToken = environment.config.property("telegram.botToken").getString()
    val authService = AuthService(botToken)
    val teamService = TeamService()

    routing {
        get("/") {
            call.respond(
                SuccessResponse(
                    data = HealthResponse(
                        status = "ok",
                        message = "Smena Hockey Team API"
                    )
                )
            )
        }

        route("/api") {
            authRoutes(authService)

            authenticate("tma") {
                teamRoutes(teamService)
            }
        }
    }
}

@Serializable
private data class HealthResponse(
    val status: String,
    val message: String
)

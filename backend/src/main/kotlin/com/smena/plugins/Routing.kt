package com.smena.plugins

import com.smena.dto.SuccessResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
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
    }
}

@Serializable
private data class HealthResponse(
    val status: String,
    val message: String
)

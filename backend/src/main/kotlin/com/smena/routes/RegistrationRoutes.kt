package com.smena.routes

import com.smena.dto.CreateRegistrationRequest
import com.smena.dto.SuccessResponse
import com.smena.plugins.currentUser
import com.smena.services.RegistrationService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.registrationRoutes(registrationService: RegistrationService) {
    route("/events/{eventId}/registrations") {
        get {
            val user = currentUser
            val eventId = call.parameters["eventId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid event ID")

            val response = registrationService.getEventRegistrations(eventId, user.id)
            call.respond(SuccessResponse(data = response))
        }

        post {
            val user = currentUser
            val eventId = call.parameters["eventId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
            val request = call.receive<CreateRegistrationRequest>()

            val response = registrationService.register(eventId, user.id, request.status)
            call.respond(HttpStatusCode.Created, SuccessResponse(data = response))
        }

        delete {
            val user = currentUser
            val eventId = call.parameters["eventId"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid event ID")

            registrationService.unregister(eventId, user.id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

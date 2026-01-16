package com.smena.routes

import com.smena.dto.CreateEventRequest
import com.smena.dto.SuccessResponse
import com.smena.dto.UpdateEventRequest
import com.smena.plugins.currentUser
import com.smena.services.EventService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.eventRoutes(eventService: EventService) {
    route("/teams/{teamId}/events") {
        post {
            val user = currentUser
            val teamId = call.parameters["teamId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val request = call.receive<CreateEventRequest>()

            val response = eventService.createEvent(
                teamId = teamId,
                userId = user.id,
                type = request.type,
                title = request.title,
                description = request.description,
                eventDate = request.eventDate,
                eventTime = request.eventTime,
                location = request.location,
                maxPlayers = request.maxPlayers
            )

            call.respond(HttpStatusCode.Created, SuccessResponse(data = response))
        }

        get {
            val user = currentUser
            val teamId = call.parameters["teamId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid team ID")

            val response = eventService.getTeamEvents(teamId, user.id)
            call.respond(SuccessResponse(data = response))
        }

        get("/{id}") {
            val user = currentUser
            val teamId = call.parameters["teamId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val eventId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid event ID")

            val response = eventService.getEventById(teamId, eventId, user.id)
            call.respond(SuccessResponse(data = response))
        }

        patch("/{id}") {
            val user = currentUser
            val teamId = call.parameters["teamId"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val eventId = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
            val request = call.receive<UpdateEventRequest>()

            val response = eventService.updateEvent(
                teamId = teamId,
                eventId = eventId,
                userId = user.id,
                title = request.title,
                description = request.description,
                eventDate = request.eventDate,
                eventTime = request.eventTime,
                location = request.location,
                maxPlayers = request.maxPlayers
            )

            call.respond(SuccessResponse(data = response))
        }

        delete("/{id}") {
            val user = currentUser
            val teamId = call.parameters["teamId"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val eventId = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid event ID")

            val response = eventService.cancelEvent(teamId, eventId, user.id)
            call.respond(SuccessResponse(data = response))
        }
    }
}

package com.smena.routes

import com.smena.dto.LineupRequest
import com.smena.dto.SuccessResponse
import com.smena.plugins.currentUser
import com.smena.services.LineupService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.lineupRoutes(lineupService: LineupService) {
    route("/events/{eventId}/lineup") {
        get {
            val user = currentUser
            val eventId = call.parameters["eventId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid event ID")

            val response = lineupService.getLineup(eventId, user.id)
            call.respond(SuccessResponse(data = response))
        }

        put {
            val user = currentUser
            val eventId = call.parameters["eventId"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
            val request = call.receive<LineupRequest>()

            val response = lineupService.saveLineup(eventId, user.id, request.entries)
            call.respond(SuccessResponse(data = response))
        }
    }
}

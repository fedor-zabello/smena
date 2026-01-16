package com.smena.routes

import com.smena.dto.CreateTeamRequest
import com.smena.dto.JoinTeamRequest
import com.smena.dto.SuccessResponse
import com.smena.dto.UpdateMemberRequest
import com.smena.plugins.currentUser
import com.smena.services.TeamService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.teamRoutes(teamService: TeamService) {
    route("/teams") {
        post {
            val request = call.receive<CreateTeamRequest>()
            val user = currentUser
            val response = teamService.createTeam(request.name, user.id)
            call.respond(HttpStatusCode.Created, SuccessResponse(data = response))
        }

        get {
            val user = currentUser
            val teams = teamService.getUserTeams(user.id)
            call.respond(SuccessResponse(data = teams))
        }

        post("/join") {
            val request = call.receive<JoinTeamRequest>()
            val user = currentUser
            val response = teamService.joinTeam(request.inviteCode, user.id)
            call.respond(SuccessResponse(data = response))
        }

        get("/{id}") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val team = teamService.getTeamById(teamId, user.id)
            call.respond(SuccessResponse(data = team))
        }

        post("/{id}/regenerate-code") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val response = teamService.regenerateInviteCode(teamId, user.id)
            call.respond(SuccessResponse(data = response))
        }

        get("/{id}/members") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val members = teamService.getTeamMembers(teamId, user.id)
            call.respond(SuccessResponse(data = members))
        }

        patch("/{id}/members/{userId}") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val targetUserId = call.parameters["userId"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
            val request = call.receive<UpdateMemberRequest>()
            val response = teamService.updateMemberRole(teamId, targetUserId, user.id, request.role)
            call.respond(SuccessResponse(data = response))
        }

        delete("/{id}/members/{userId}") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            val targetUserId = call.parameters["userId"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
            teamService.removeMember(teamId, targetUserId, user.id)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}/leave") {
            val user = currentUser
            val teamId = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
            teamService.leaveTeam(teamId, user.id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

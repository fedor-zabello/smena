package com.smena.plugins

import com.smena.dto.ErrorDetail
import com.smena.dto.ErrorResponse
import com.smena.exceptions.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<InvalidInitDataException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_INIT_DATA",
                        message = cause.message ?: "Invalid Telegram initData"
                    )
                )
            )
        }

        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "UNAUTHORIZED",
                        message = cause.message ?: "Unauthorized"
                    )
                )
            )
        }

        exception<TeamNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "TEAM_NOT_FOUND",
                        message = cause.message ?: "Team not found"
                    )
                )
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "FORBIDDEN",
                        message = cause.message ?: "Access denied"
                    )
                )
            )
        }

        exception<InvalidInviteCodeException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_INVITE_CODE",
                        message = cause.message ?: "Invalid invite code"
                    )
                )
            )
        }

        exception<AlreadyTeamMemberException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "ALREADY_TEAM_MEMBER",
                        message = cause.message ?: "Already a member of this team"
                    )
                )
            )
        }

        exception<MemberNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "MEMBER_NOT_FOUND",
                        message = cause.message ?: "Member not found"
                    )
                )
            )
        }

        exception<CannotRemoveLastAdminException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "CANNOT_REMOVE_LAST_ADMIN",
                        message = cause.message ?: "Cannot remove the last admin from the team"
                    )
                )
            )
        }

        exception<InvalidRoleException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_ROLE",
                        message = cause.message ?: "Invalid role"
                    )
                )
            )
        }

        exception<EventNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "EVENT_NOT_FOUND",
                        message = cause.message ?: "Event not found"
                    )
                )
            )
        }

        exception<InvalidEventTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_EVENT_TYPE",
                        message = cause.message ?: "Invalid event type"
                    )
                )
            )
        }

        exception<InvalidEventDateException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INVALID_EVENT_DATE",
                        message = cause.message ?: "Invalid event date or time"
                    )
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INTERNAL_SERVER_ERROR",
                        message = cause.message ?: "An unexpected error occurred"
                    )
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "NOT_FOUND",
                        message = "Resource not found"
                    )
                )
            )
        }
    }
}

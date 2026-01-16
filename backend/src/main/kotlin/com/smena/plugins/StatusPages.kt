package com.smena.plugins

import com.smena.dto.ErrorDetail
import com.smena.dto.ErrorResponse
import com.smena.exceptions.AlreadyTeamMemberException
import com.smena.exceptions.ForbiddenException
import com.smena.exceptions.InvalidInitDataException
import com.smena.exceptions.InvalidInviteCodeException
import com.smena.exceptions.TeamNotFoundException
import com.smena.exceptions.UnauthorizedException
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

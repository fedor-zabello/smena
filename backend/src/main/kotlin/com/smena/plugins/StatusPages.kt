package com.smena.plugins

import com.smena.dto.ErrorDetail
import com.smena.dto.ErrorResponse
import com.smena.exceptions.InvalidInitDataException
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

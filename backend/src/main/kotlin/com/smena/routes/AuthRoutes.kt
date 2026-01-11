package com.smena.routes

import com.smena.dto.AuthRequest
import com.smena.dto.SuccessResponse
import com.smena.services.AuthService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/init") {
            val request = call.receive<AuthRequest>()
            val response = authService.authenticate(request.initData)
            call.respond(SuccessResponse(data = response))
        }
    }
}

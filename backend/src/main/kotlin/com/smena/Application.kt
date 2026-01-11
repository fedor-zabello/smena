package com.smena

import com.smena.db.DatabaseFactory
import com.smena.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)

    configureContentNegotiation()
    configureCORS()
    configureAuthentication()
    configureStatusPages()
    configureRouting()
}

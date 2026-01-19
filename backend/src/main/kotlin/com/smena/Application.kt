package com.smena

import com.smena.db.DatabaseFactory
import com.smena.plugins.*
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository
import com.smena.services.RegistrationOpenerService
import com.smena.telegram.TelegramBot
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

    val registrationOpenerService = RegistrationOpenerService()
    registrationOpenerService.start(this)

    val botToken = environment.config.property("telegram.botToken").getString()
    val miniAppUrl = environment.config.property("telegram.miniAppUrl").getString()

    val userRepository = UserRepository()
    val teamRepository = TeamRepository()
    val teamMemberRepository = TeamMemberRepository()

    val telegramBot = TelegramBot(
        botToken = botToken,
        miniAppUrl = miniAppUrl,
        userRepository = userRepository,
        teamRepository = teamRepository,
        teamMemberRepository = teamMemberRepository
    )
    telegramBot.start(this)

    monitor.subscribe(ApplicationStopped) {
        registrationOpenerService.stop()
        telegramBot.stop()
    }
}

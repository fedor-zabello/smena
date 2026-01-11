package com.smena.plugins

import com.smena.exceptions.UnauthorizedException
import com.smena.models.User
import com.smena.repositories.UserRepository
import com.smena.telegram.InitDataValidator
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

/**
 * Principal, содержащий аутентифицированного пользователя.
 */
data class UserPrincipal(val user: User)

/**
 * Конфигурирует аутентификацию Telegram Mini App.
 *
 * Защищённые эндпоинты должны использовать:
 * ```
 * authenticate("tma") {
 *     get("/protected") {
 *         val user = call.currentUser
 *         // ...
 *     }
 * }
 * ```
 */
fun Application.configureAuthentication() {
    val botToken = environment.config.property("telegram.botToken").getString()
    val userRepository = UserRepository()

    install(Authentication) {
        tma("tma", botToken, userRepository)
    }
}

/**
 * Регистрирует custom authentication provider для схемы "tma" (Telegram Mini App).
 *
 * Ожидает заголовок: `Authorization: tma <initData>`
 */
fun AuthenticationConfig.tma(
    name: String,
    botToken: String,
    userRepository: UserRepository
) {
    provider(name) {
        authenticate { context ->
            val authHeader = context.call.request.headers["Authorization"]

            if (authHeader == null || !authHeader.startsWith("tma ")) {
                context.challenge("tma", AuthenticationFailedCause.NoCredentials) { challenge, call ->
                    throw UnauthorizedException("Missing or invalid Authorization header")
                }
                return@authenticate
            }

            val initData = authHeader.removePrefix("tma ")

            val telegramUser = InitDataValidator.validate(initData, botToken)
            if (telegramUser == null) {
                context.challenge("tma", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                    throw UnauthorizedException("Invalid initData signature")
                }
                return@authenticate
            }

            val user = userRepository.findByTelegramId(telegramUser.id)
            if (user == null) {
                context.challenge("tma", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                    throw UnauthorizedException("User not found. Please call /api/auth/init first")
                }
                return@authenticate
            }

            context.principal(UserPrincipal(user))
        }
    }
}

/**
 * Получает текущего аутентифицированного пользователя из контекста запроса.
 *
 * @throws UnauthorizedException если пользователь не аутентифицирован
 */
val ApplicationCall.currentUser: User
    get() = principal<UserPrincipal>()?.user
        ?: throw UnauthorizedException("User not authenticated")

/**
 * Получает текущего аутентифицированного пользователя или null.
 */
val ApplicationCall.currentUserOrNull: User?
    get() = principal<UserPrincipal>()?.user

/**
 * Получает текущего пользователя внутри роута.
 */
val RoutingContext.currentUser: User
    get() = call.currentUser

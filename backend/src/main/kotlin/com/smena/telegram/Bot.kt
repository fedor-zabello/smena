package com.smena.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.WebAppInfo
import com.smena.models.TeamRole
import com.smena.repositories.TeamMemberRepository
import com.smena.repositories.TeamRepository
import com.smena.repositories.UserRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Telegram бот для Mini App.
 *
 * Функции:
 * - /start — приветствие с кнопкой открытия Mini App
 * - /start {inviteCode} — deep link для вступления в команду
 * - /connect {inviteCode} — привязка группового чата к команде (только для ADMIN)
 */
class TelegramBot(
    private val botToken: String,
    private val miniAppUrl: String,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository
) {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)
    private var job: Job? = null

    private val bot = bot {
        token = botToken

        dispatch {
            command("start") {
                logger.info("Received /start command from chat ${message.chat.id}, user ${message.from?.id}")
                val inviteCode = args.firstOrNull()
                val chatId = ChatId.fromId(message.chat.id)

                if (inviteCode != null) {
                    handleStartWithInviteCode(chatId, inviteCode)
                } else {
                    handleStart(chatId)
                }
            }

            command("connect") {
                logger.info("Received /connect command from chat ${message.chat.id}, user ${message.from?.id}")
                val inviteCode = args.firstOrNull()
                val chatId = ChatId.fromId(message.chat.id)
                val senderTelegramId = message.from?.id

                handleConnect(chatId, message.chat.id, senderTelegramId, inviteCode)
            }

            telegramError {
                logger.error("Telegram API error: ${error.getErrorMessage()}", error.exception)
            }
        }
    }

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            try {
                logger.info("TelegramBot starting with long polling...")
                bot.startPolling()
                logger.warn("TelegramBot polling loop finished unexpectedly")
            } catch (e: Exception) {
                logger.error("Failed to start or run TelegramBot polling", e)
            }
        }
    }

    fun stop() {
        bot.stopPolling()
        job?.cancel()
        logger.info("TelegramBot stopped")
    }

    private fun handleStart(chatId: ChatId) {
        val text = """
            Привет! Я бот для управления хоккейной командой.

            Открой приложение, чтобы:
            • Записаться на игры и тренировки
            • Посмотреть расписание
            • Узнать состав на игру
        """.trimIndent()

        val keyboard = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.WebApp(
                    text = "Открыть приложение",
                    webApp = WebAppInfo(url = miniAppUrl)
                )
            )
        )

        val result = bot.sendMessage(
            chatId = chatId,
            text = text,
            replyMarkup = keyboard
        )

        result.fold(
            ifSuccess = { /* success, no need to log */ },
            ifError = { error ->
                logger.error("Failed to send /start message to chat $chatId: ${error.errorBody}")
            }
        )
    }

    private fun handleStartWithInviteCode(chatId: ChatId, inviteCode: String) {
        val text = """
            Привет! Тебя пригласили в команду.

            Нажми кнопку ниже, чтобы присоединиться.
        """.trimIndent()

        val urlWithCode = "$miniAppUrl?startapp=$inviteCode"

        val keyboard = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.WebApp(
                    text = "Присоединиться к команде",
                    webApp = WebAppInfo(url = urlWithCode)
                )
            )
        )

        val result = bot.sendMessage(
            chatId = chatId,
            text = text,
            replyMarkup = keyboard
        )

        result.fold(
            ifSuccess = { /* success, no need to log */ },
            ifError = { error ->
                logger.error("Failed to send /start invite message to chat $chatId: ${error.errorBody}")
            }
        )
    }

    private fun handleConnect(chatId: ChatId, rawChatId: Long, senderTelegramId: Long?, inviteCode: String?) {
        // Проверка наличия invite code
        if (inviteCode.isNullOrBlank()) {
            bot.sendMessage(
                chatId = chatId,
                text = "Использование: /connect INVITE_CODE"
            )
            logger.debug("Connect command called without invite code in chat $rawChatId")
            return
        }

        // Проверка что это групповой чат (в Telegram групповые чаты имеют отрицательный ID)
        if (rawChatId >= 0) {
            bot.sendMessage(
                chatId = chatId,
                text = "Эта команда работает только в групповых чатах"
            )
            logger.debug("Connect command called in private chat by telegram user $senderTelegramId")
            return
        }

        // Проверка что отправитель существует
        if (senderTelegramId == null) {
            bot.sendMessage(
                chatId = chatId,
                text = "Не удалось определить отправителя команды"
            )
            logger.warn("Connect command sender is null in chat $rawChatId")
            return
        }

        // Поиск команды по invite code
        val team = teamRepository.findByInviteCode(inviteCode)
        if (team == null) {
            bot.sendMessage(
                chatId = chatId,
                text = "Команда с кодом $inviteCode не найдена"
            )
            logger.debug("Connect command: team not found for code $inviteCode")
            return
        }

        // Поиск пользователя по telegram ID
        val user = userRepository.findByTelegramId(senderTelegramId)
        if (user == null) {
            bot.sendMessage(
                chatId = chatId,
                text = "Сначала откройте Mini App через /start"
            )
            logger.debug("Connect command: user not found for telegram id $senderTelegramId")
            return
        }

        // Проверка что пользователь является ADMIN команды
        val membership = teamMemberRepository.findByUserAndTeam(user.id, team.id)
        if (membership == null || membership.role != TeamRole.ADMIN) {
            bot.sendMessage(
                chatId = chatId,
                text = "Только администратор команды может привязать чат"
            )
            logger.debug("Connect command: user ${user.id} is not admin of team ${team.id}")
            return
        }

        // Сохранение chat_id в команду
        teamRepository.updateChatId(team.id, rawChatId)

        bot.sendMessage(
            chatId = chatId,
            text = "Чат привязан к команде ${team.name}"
        )

        logger.info("Chat $rawChatId connected to team ${team.id} (${team.name}) by user ${user.id}")
    }
}

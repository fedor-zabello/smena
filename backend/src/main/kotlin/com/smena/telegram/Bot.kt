package com.smena.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.WebAppInfo
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Telegram бот для Mini App.
 *
 * Функции:
 * - /start — приветствие с кнопкой открытия Mini App
 * - /start {inviteCode} — deep link для вступления в команду
 */
class TelegramBot(
    private val botToken: String,
    private val miniAppUrl: String
) {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)
    private var job: Job? = null

    private val bot = bot {
        token = botToken

        dispatch {
            command("start") {
                val inviteCode = args.firstOrNull()
                val chatId = ChatId.fromId(message.chat.id)

                if (inviteCode != null) {
                    handleStartWithInviteCode(chatId, inviteCode)
                } else {
                    handleStart(chatId)
                }
            }
        }
    }

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            logger.info("TelegramBot starting with long polling...")
            bot.startPolling()
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

        bot.sendMessage(
            chatId = chatId,
            text = text,
            replyMarkup = keyboard
        )

        logger.debug("Sent welcome message to chat $chatId")
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

        bot.sendMessage(
            chatId = chatId,
            text = text,
            replyMarkup = keyboard
        )

        logger.debug("Sent invite message to chat $chatId with code $inviteCode")
    }
}

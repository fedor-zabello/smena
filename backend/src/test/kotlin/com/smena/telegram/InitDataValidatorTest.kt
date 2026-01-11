package com.smena.telegram

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InitDataValidatorTest {

    private val testBotToken = "1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"

    @Test
    fun `validate returns TelegramUser for valid initData`() {
        val initData = createValidInitData(
            telegramId = 123456789,
            firstName = "Ivan",
            lastName = "Petrov",
            username = "ivanpetrov",
            botToken = testBotToken
        )

        val result = InitDataValidator.validate(initData, testBotToken)

        assertNotNull(result)
        assertEquals(123456789, result.id)
        assertEquals("Ivan", result.firstName)
        assertEquals("Petrov", result.lastName)
        assertEquals("ivanpetrov", result.username)
    }

    @Test
    fun `validate returns TelegramUser without optional fields`() {
        val initData = createValidInitData(
            telegramId = 987654321,
            firstName = "Anna",
            lastName = null,
            username = null,
            botToken = testBotToken
        )

        val result = InitDataValidator.validate(initData, testBotToken)

        assertNotNull(result)
        assertEquals(987654321, result.id)
        assertEquals("Anna", result.firstName)
        assertNull(result.lastName)
        assertNull(result.username)
    }

    @Test
    fun `validate returns null for invalid hash`() {
        val initData = createValidInitData(
            telegramId = 123456789,
            firstName = "Ivan",
            lastName = null,
            username = null,
            botToken = testBotToken
        )
        // Подменяем hash на невалидный
        val tamperedInitData = initData.replace(Regex("hash=[^&]+"), "hash=invalidhash123")

        val result = InitDataValidator.validate(tamperedInitData, testBotToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for wrong bot token`() {
        val initData = createValidInitData(
            telegramId = 123456789,
            firstName = "Ivan",
            lastName = null,
            username = null,
            botToken = testBotToken
        )
        val wrongToken = "9999999999:WRONGtokenHERE"

        val result = InitDataValidator.validate(initData, wrongToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for tampered user data`() {
        val initData = createValidInitData(
            telegramId = 123456789,
            firstName = "Ivan",
            lastName = null,
            username = null,
            botToken = testBotToken
        )
        // Подменяем telegram_id в user JSON
        val tamperedInitData = initData.replace("123456789", "999999999")

        val result = InitDataValidator.validate(tamperedInitData, testBotToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for missing hash`() {
        val userJson = """{"id":123,"first_name":"Test"}"""
        val authDate = System.currentTimeMillis() / 1000
        val initData = "user=${urlEncode(userJson)}&auth_date=$authDate"

        val result = InitDataValidator.validate(initData, testBotToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for missing user`() {
        val authDate = System.currentTimeMillis() / 1000
        val initData = "auth_date=$authDate&hash=somehash"

        val result = InitDataValidator.validate(initData, testBotToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for malformed initData`() {
        val result = InitDataValidator.validate("not_valid_data", testBotToken)

        assertNull(result)
    }

    @Test
    fun `validate returns null for empty initData`() {
        val result = InitDataValidator.validate("", testBotToken)

        assertNull(result)
    }

    /**
     * Создаёт валидную initData строку с правильной подписью.
     * Используется для тестирования.
     */
    private fun createValidInitData(
        telegramId: Long,
        firstName: String,
        lastName: String?,
        username: String?,
        botToken: String
    ): String {
        val userJson = buildString {
            append("""{"id":$telegramId,"first_name":"$firstName"""")
            lastName?.let { append(""","last_name":"$it"""") }
            username?.let { append(""","username":"$it"""") }
            append("}")
        }

        val authDate = System.currentTimeMillis() / 1000

        val params = mapOf(
            "user" to userJson,
            "auth_date" to authDate.toString()
        )

        // Формируем data_check_string (отсортированные параметры)
        val dataCheckString = params
            .toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString("\n")

        // Вычисляем hash
        val secretKey = hmacSha256("WebAppData".toByteArray(), botToken.toByteArray())
        val hash = hmacSha256(secretKey, dataCheckString.toByteArray()).toHexString()

        // Формируем итоговую строку
        return params
            .map { (key, value) -> "$key=${urlEncode(value)}" }
            .joinToString("&") + "&hash=$hash"
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}

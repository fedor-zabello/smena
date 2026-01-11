package com.smena.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Данные пользователя Telegram, извлечённые из initData.
 */
@Serializable
data class TelegramUser(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String? = null,
    val username: String? = null
)

/**
 * Валидатор initData от Telegram Mini App.
 *
 * Telegram подписывает initData с помощью HMAC-SHA256.
 * Мы проверяем подпись, используя токен бота.
 *
 * @see <a href="https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app">Telegram Documentation</a>
 */
object InitDataValidator {

    private val json = Json { ignoreUnknownKeys = true }

    /** Максимальный возраст initData: 5 минут */
    private const val MAX_AGE_SECONDS = 300L

    /**
     * Валидирует initData и возвращает данные пользователя.
     *
     * @param initData URL-encoded строка от Telegram (например: "user=%7B...%7D&auth_date=123&hash=abc")
     * @param botToken Токен Telegram бота
     * @return TelegramUser если подпись валидна, null если невалидна или данные устарели
     */
    fun validate(initData: String, botToken: String): TelegramUser? {
        return try {
            val params = parseInitData(initData)

            val hash = params["hash"] ?: return null
            val userJson = params["user"] ?: return null
            val authDate = params["auth_date"]?.toLongOrNull() ?: return null

            // Проверяем, что initData не устарела (защита от replay attacks)
            val currentTime = System.currentTimeMillis() / 1000
            if (currentTime - authDate > MAX_AGE_SECONDS) {
                return null
            }

            // Формируем строку для проверки (все параметры кроме hash, отсортированные по ключу)
            val dataCheckString = params
                .filterKeys { it != "hash" }
                .toSortedMap()
                .map { (key, value) -> "$key=$value" }
                .joinToString("\n")

            // Вычисляем HMAC
            val secretKey = hmacSha256("WebAppData".toByteArray(), botToken.toByteArray())
            val calculatedHash = hmacSha256(secretKey, dataCheckString.toByteArray())
            val calculatedHashHex = calculatedHash.toHexString()

            // Сравниваем хеши (constant-time comparison для защиты от timing attacks)
            if (!constantTimeEquals(hash, calculatedHashHex)) {
                return null
            }

            // Парсим данные пользователя
            json.decodeFromString<TelegramUser>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Парсит URL-encoded initData строку в Map.
     */
    private fun parseInitData(initData: String): Map<String, String> {
        return initData
            .split("&")
            .filter { it.isNotEmpty() }
            .associate { param ->
                val (key, value) = param.split("=", limit = 2).let {
                    if (it.size == 2) it[0] to it[1] else it[0] to ""
                }
                key to URLDecoder.decode(value, StandardCharsets.UTF_8)
            }
    }

    /**
     * Вычисляет HMAC-SHA256.
     */
    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /**
     * Конвертирует ByteArray в hex-строку.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    /**
     * Сравнение строк за константное время (защита от timing attacks).
     *
     * Обычное сравнение (==) прерывается на первом несовпадении, и по времени ответа
     * теоретически можно угадать, сколько символов совпало. Здесь мы проверяем ВСЕ
     * символы через XOR и накапливаем результат — время не зависит от позиции различия.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

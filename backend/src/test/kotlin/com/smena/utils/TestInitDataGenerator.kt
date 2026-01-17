package com.smena.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TestInitDataGenerator {

    const val TEST_BOT_TOKEN = "test_token_for_local_dev"

    fun generate(
        telegramId: Long,
        firstName: String,
        lastName: String? = null,
        username: String? = null,
        botToken: String = TEST_BOT_TOKEN,
        authDate: Long = System.currentTimeMillis() / 1000
    ): String {
        val userJson = buildString {
            append("""{"id":$telegramId,"first_name":"$firstName"""")
            lastName?.let { append(""","last_name":"$it"""") }
            username?.let { append(""","username":"$it"""") }
            append("}")
        }

        val params = mapOf(
            "user" to userJson,
            "auth_date" to authDate.toString()
        )

        val dataCheckString = params
            .toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString("\n")

        val secretKey = hmacSha256("WebAppData".toByteArray(), botToken.toByteArray())
        val hash = hmacSha256(secretKey, dataCheckString.toByteArray()).toHexString()

        return params
            .map { (key, value) -> "$key=${urlEncode(value)}" }
            .joinToString("&") + "&hash=$hash"
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}

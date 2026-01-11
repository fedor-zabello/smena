package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val initData: String
)

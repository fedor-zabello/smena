package com.smena.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String
)

@Serializable
data class SuccessResponse<T>(
    val data: T
)

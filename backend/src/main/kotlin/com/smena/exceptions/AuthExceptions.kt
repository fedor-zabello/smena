package com.smena.exceptions

class InvalidInitDataException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String = "Unauthorized") : RuntimeException(message)

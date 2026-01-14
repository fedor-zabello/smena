package com.smena.exceptions

class TeamNotFoundException(message: String = "Team not found") : RuntimeException(message)

class ForbiddenException(message: String = "Access denied") : RuntimeException(message)

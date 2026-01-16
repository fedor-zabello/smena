package com.smena.exceptions

class TeamNotFoundException(message: String = "Team not found") : RuntimeException(message)

class ForbiddenException(message: String = "Access denied") : RuntimeException(message)

class InvalidInviteCodeException(message: String = "Invalid invite code") : RuntimeException(message)

class AlreadyTeamMemberException(message: String = "Already a member of this team") : RuntimeException(message)

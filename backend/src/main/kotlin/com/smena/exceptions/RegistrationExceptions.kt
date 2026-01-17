package com.smena.exceptions

class RegistrationNotOpenException(message: String = "Registration is not open for this event") : RuntimeException(message)

class RegistrationNotFoundException(message: String = "Registration not found") : RuntimeException(message)

class InvalidRegistrationStatusException(message: String = "Invalid registration status") : RuntimeException(message)

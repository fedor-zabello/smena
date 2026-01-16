package com.smena.exceptions

class EventNotFoundException(message: String = "Event not found") : RuntimeException(message)

class InvalidEventTypeException(message: String = "Invalid event type") : RuntimeException(message)

class InvalidEventDateException(message: String = "Invalid event date or time") : RuntimeException(message)

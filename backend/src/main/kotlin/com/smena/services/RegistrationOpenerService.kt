package com.smena.services

import com.smena.models.EventStatus
import com.smena.repositories.EventRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

class RegistrationOpenerService(
    private val eventRepository: EventRepository = EventRepository()
) {
    private val logger = LoggerFactory.getLogger(RegistrationOpenerService::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            logger.info("RegistrationOpenerService started")
            while (isActive) {
                try {
                    checkAndOpenRegistrations()
                } catch (e: Exception) {
                    logger.error("Error checking registrations", e)
                }
                delay(5.minutes)
            }
        }
    }

    fun stop() {
        job?.cancel()
        logger.info("RegistrationOpenerService stopped")
    }

    private fun checkAndOpenRegistrations() {
        val eventsToOpen = eventRepository.findScheduledReadyToOpen()

        if (eventsToOpen.isEmpty()) {
            logger.debug("No events ready to open registration")
            return
        }

        for (event in eventsToOpen) {
            eventRepository.updateStatus(event.id, EventStatus.OPEN)
            logger.info("Opened registration for event: id=${event.id}, title=${event.title}, date=${event.eventDate}")
        }

        logger.info("Opened registration for ${eventsToOpen.size} event(s)")
    }
}

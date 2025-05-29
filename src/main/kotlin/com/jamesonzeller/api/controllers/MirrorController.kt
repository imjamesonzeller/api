package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.mirror.calendarevents.models.Event
import com.jamesonzeller.api.mirror.calendarevents.services.UpcomingEventsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mirror")
class MirrorController(
    private val upcomingEventsService: UpcomingEventsService
) {

    @GetMapping("/get_upcoming_events")
    fun getUpcomingEvents(): ResponseEntity<List<Event>> {
        val calEvents: List<Event> = upcomingEventsService.getUpcomingEvents()
        return ResponseEntity.ok(calEvents)
    }
}
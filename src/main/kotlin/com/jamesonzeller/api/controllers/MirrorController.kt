package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.mirror.calendarevents.models.Event
import com.jamesonzeller.api.mirror.calendarevents.services.UpcomingEventsService
import com.jamesonzeller.api.mirror.notiontasks.models.Task
import com.jamesonzeller.api.mirror.notiontasks.services.NotionTasksService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mirror")
class MirrorController(
    private val upcomingEventsService: UpcomingEventsService,
    private val notionTasksService: NotionTasksService
) {

    @GetMapping("/get_upcoming_events")
    fun getUpcomingEvents(): List<Event> {
        val calEvents: List<Event> = upcomingEventsService.getUpcomingEvents()
        return calEvents
    }

    @GetMapping("/get_notion_tasks")
    fun getNotionTasks(): List<Task> {
        val notionTasks: List<Task> = notionTasksService.getTasks()
        return notionTasks
    }
}
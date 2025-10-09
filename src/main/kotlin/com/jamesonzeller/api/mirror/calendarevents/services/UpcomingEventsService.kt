package com.jamesonzeller.api.mirror.calendarevents.services

import com.jamesonzeller.api.mirror.calendarevents.models.Event
import com.google.api.services.calendar.Calendar
import org.apache.http.HttpException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@Service
class UpcomingEventsService(
    private val calendarService: Calendar
) {
    private val zone = java.time.ZoneId.of("America/Chicago")
    private data class RequestInformation(
        val calendarId: String,
        val now: String,
        val tomorrow: String,
    )

    fun getUpcomingEvents(): List<Event> {
        val events: MutableList<Event> = emptyList<Event>().toMutableList()

        try {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val isoNow = now.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val midnightTomorrow = now.plusDays(1)
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(0)

            val isoMidnightTomorrow = midnightTomorrow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            println("Getting the upcoming 10 events")

            val calendarIDs = listOf(
                "imjamesonzeller@gmail.com",
                "58da859ee90a0c2d692d6027060e94268b65e3643267614f71b5772b1b2178d1@group.calendar.google.com",
                "918757d470383d7acb40e0b1cb103a3a12ba5175b048c085ccf91c026cb1a46f@group.calendar.google.com"
            )

            for (calendarID in calendarIDs) {
                requestAPI(
                    calendarService,
                    RequestInformation(calendarID, isoNow, isoMidnightTomorrow),
                    events
                )
            }
            events.sortAndTruncateInPlace()
            return events.removeTomorrowEvents()
        } catch (e: HttpException) {
            println("An error occured: ${e.message}")
            return listOf(Event("An error occured: ${e.message}", "ERROR", "ERROR"))
        }
    }

    private fun requestAPI(
        service: Calendar,
        info: RequestInformation,
        events: MutableList<Event>
    ): Unit {
        val timeMin = com.google.api.client.util.DateTime(info.now)
        val timeMax = com.google.api.client.util.DateTime(info.tomorrow)

        val eventsResults = service.events().list(info.calendarId)
            .setTimeMin(timeMin)
            .setTimeMax(timeMax)
            .setMaxResults(10)
            .setSingleEvents(true)
            .setOrderBy("startTime")
            .execute()

        val items = eventsResults.items ?: emptyList()

        if (items.isEmpty()) {
            println("No upcoming events")
        }

        for (event in items) {
            val name = event.summary ?: "No Title"
            val start = event.start?.dateTime?.toString() ?: event.start?.date?.toString() ?: "Unknown Start"
            val end = event.end?.dateTime?.toString() ?: event.end?.date?.toString() ?: "Unknown End"
            val date = formatDate(start, end)

            events.add(Event(name, date, start))
        }
    }

    fun MutableList<Event>.sortAndTruncateInPlace() {
        this.sortBy { it.sortKey }
        if (this.size > 10) this.subList(10, this.size).clear()
    }

    fun List<Event>.removeTomorrowEvents(): List<Event> {
        val tomorrow = LocalDate.now(zone).plusDays(1)
        val filtered = mutableListOf<Event>()

        for (event in this) {
            filtered.add(event)
            val eventDate = parseFlexibleZonedDateTime(event.trueStart).withZoneSameInstant(zone).toLocalDate()

            if (eventDate == tomorrow) {
                break
            }
        }

        return filtered
    }

    fun parseFlexibleZonedDateTime(input: String): ZonedDateTime {
        return try {
            ZonedDateTime.parse(input)
        } catch (e: DateTimeParseException) {
            LocalDate.parse(input).atStartOfDay(zone)
        }
    }

    fun formatDate(start: String, end: String): String {
        val startDateTime = parseFlexibleZonedDateTime(start)
        val endDateTime = parseFlexibleZonedDateTime(end)

        val dateFormatter = DateTimeFormatter.ofPattern("E, MMM dd")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        return if (startDateTime.toLocalDate() == endDateTime.toLocalDate()) {
            if (startDateTime.toLocalDate() == ZonedDateTime.now().toLocalDate()) {
                // Same-day event today
                "${startDateTime.toLocalTime().format(timeFormatter)} - ${endDateTime.toLocalTime().format(timeFormatter)}"
            } else {
                // Same-day event in the future
                "${startDateTime.toLocalDate().format(dateFormatter)} | ${startDateTime.toLocalTime().format(timeFormatter)} - ${endDateTime.toLocalTime().format(timeFormatter)}"
            }
        } else {
            // Multi-day or all-day event
            val isAllDay = ChronoUnit.DAYS.between(startDateTime, endDateTime) == 1L &&
                    startDateTime.toLocalTime() == endDateTime.toLocalTime()

            if (isAllDay) {
                startDateTime.toLocalDate().format(dateFormatter)
            } else {
                "${startDateTime.toLocalDate().format(dateFormatter)} - ${endDateTime.toLocalDate().format(dateFormatter)}"
            }
        }
    }
}
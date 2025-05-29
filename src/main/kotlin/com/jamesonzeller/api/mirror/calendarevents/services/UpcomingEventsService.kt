package com.jamesonzeller.api.mirror.calendarevents.services

import com.jamesonzeller.api.mirror.calendarevents.models.Event
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.api.services.calendar.Calendar
import org.apache.http.HttpException
import java.io.FileInputStream
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlin.system.exitProcess

@Service
class UpcomingEventsService(
    @Value("\${SERVICE_ACCOUNT_PATH}") private val serviceAccountPath: String
) {
    private data class RequestInformation(
        val calendarId: String,
        val now: String,
        val tomorrow: String,
    )

    private val events: MutableList<Event> = emptyList<Event>().toMutableList()
    private val SCOPES = listOf("https://www.googleapis.com/auth/calendar.readonly")

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private var httpTransport = try {
        GoogleNetHttpTransport.newTrustedTransport();
    } catch (e: Exception) {
        e.printStackTrace();
        exitProcess(1);
    }

    fun getUpcomingEvents(): List<Event> {
        val credentials = ServiceAccountCredentials
            .fromStream(FileInputStream(serviceAccountPath))
            .createScoped(SCOPES)

        try {
            val httpRequestInitializer: HttpRequestInitializer = com.google.api.client.googleapis.auth.oauth2.GoogleCredential
                .fromStream(FileInputStream(serviceAccountPath))
                .createScoped(SCOPES)

            val calendarService = Calendar.Builder(httpTransport, jsonFactory, httpRequestInitializer)
                .setApplicationName("Calendar Events")
                .build()


            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val isoNow = now.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

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
                    RequestInformation(calendarID, isoNow, isoMidnightTomorrow)
                )
            }

            events.sortAndTruncate()
            return events.removeTomorrowEvents()
        } catch (e: HttpException) {
            println("An error occured: ${e.message}")
            return listOf(Event("An error occured: ${e.message}", "ERROR", "ERROR"))
        }
    }

    private fun requestAPI(service: Calendar, info: RequestInformation): Unit {
        val timeMin = DateTime(info.now)
        val timeMax = DateTime(info.tomorrow)

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

    fun List<Event>.sortAndTruncate(): List<Event> = this.sorted().take(10)

    fun List<Event>.removeTomorrowEvents(): List<Event> {
        val tomorrow = LocalDate.now().plusDays(1)
        val filtered = mutableListOf<Event>()

        for (event in this) {
            filtered.add(event)
            val eventDate = ZonedDateTime.parse(event.trueStart).toLocalDate()

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
            LocalDate.parse(input).atStartOfDay(ZoneOffset.UTC)
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
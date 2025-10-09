package com.jamesonzeller.api.mirror.calendarevents.models

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.*

data class Event(
    val name: String,
    val date: String,

    @JsonIgnore
    val trueStart: String
) : Comparable<Event> {
    @get:JsonIgnore
    val sortKey: Instant by lazy { toInstant(trueStart) }

    override fun compareTo(other: Event): Int {
        return sortKey.compareTo(other.sortKey)
    }

    private fun toInstant(s: String): Instant {
        // 1) Offset/Z strings like 2025-10-10T09:00:00-05:00 or 2025-10-10T14:00:00Z
        return try {
            OffsetDateTime.parse(s).toInstant()
        } catch (_: Exception) {
            val zone = ZoneId.of("America/Chicago")

            // 2) Date-only like 2025-10-10 -> start of day in that zone
            try {
                LocalDate.parse(s).atStartOfDay(zone).toInstant()
            } catch (_: Exception) {
                // 3) Naive local datetime like 2025-10-10T09:00:00 (no offset)
                LocalDateTime.parse(s).atZone(zone).toInstant()
            }
        }
    }
}
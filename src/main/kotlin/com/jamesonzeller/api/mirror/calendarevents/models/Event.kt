package com.jamesonzeller.api.mirror.calendarevents.models

import com.fasterxml.jackson.annotation.JsonIgnore

data class Event(
    val name: String,
    val date: String,

    @JsonIgnore
    val trueStart: String
) : Comparable<Event> {
    override fun compareTo(other: Event): Int {
        return this.trueStart.compareTo(other.trueStart)
    }
}
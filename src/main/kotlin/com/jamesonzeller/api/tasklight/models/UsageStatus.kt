package com.jamesonzeller.api.tasklight.models

import com.jamesonzeller.api.controllers.DAILY_LIMIT

data class UsageStatus(
    val allowed: Boolean,
    val used: Long,
    val remaining: Long,
    val limit: Int = DAILY_LIMIT,
    val date: String
)
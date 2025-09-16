package com.jamesonzeller.api.tasklight.models

import com.jamesonzeller.api.controllers.DAILY_LIMIT

data class UsageIncrementResult(
    val used: Long,
    val limit: Int = DAILY_LIMIT,
    val date: String,
)
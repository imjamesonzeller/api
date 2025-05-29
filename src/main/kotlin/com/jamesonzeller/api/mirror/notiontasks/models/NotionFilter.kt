package com.jamesonzeller.api.mirror.notiontasks.models

import kotlinx.serialization.Serializable

@Serializable
data class NotionFilter(
    val filter: PropertyFilter
)

@Serializable
data class PropertyFilter(
    val property: String,
    val checkbox: CheckboxCondition
)

@Serializable
data class CheckboxCondition(
    val equals: Boolean
)
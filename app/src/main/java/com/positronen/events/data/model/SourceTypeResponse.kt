package com.positronen.events.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceTypeResponse(
    val id: Long? = null,
    val name: String? = null
)

package com.positronen.events.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TagResponse(
    val id: String,
    val name: String
)

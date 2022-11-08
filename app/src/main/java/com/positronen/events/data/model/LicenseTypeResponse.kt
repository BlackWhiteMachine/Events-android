package com.positronen.events.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LicenseTypeResponse(
    val id: Int,
    val name: String
)

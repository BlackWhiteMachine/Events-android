package com.positronen.events.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NameResponse(
    @SerialName(value = "fi")
    val fi: String? = null,
    @SerialName(value = "en")
    val en: String? = null,
    @SerialName(value = "sv")
    val sv: String? = null,
    @SerialName(value = "zh")
    val zh:	String? = null
)
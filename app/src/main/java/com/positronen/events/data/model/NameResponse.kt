package com.positronen.events.data.model

import com.google.gson.annotations.SerializedName

data class NameResponse(
    @SerializedName(value = "fi")
    val fi: String? = null,
    @SerializedName(value = "en")
    val en: String? = null,
    @SerializedName(value = "sv")
    val sv: String? = null,
    @SerializedName(value = "zh")
    val zh:	String? = null
)
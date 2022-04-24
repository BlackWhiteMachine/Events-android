package com.positronen.events.data.model

data class DescriptionResponse(
    val intro: String,
    val body: String,
    val images: List<ImageResponse>
)
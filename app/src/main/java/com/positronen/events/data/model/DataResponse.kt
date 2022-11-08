package com.positronen.events.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DataResponse<Type>(
    val data: Type
)

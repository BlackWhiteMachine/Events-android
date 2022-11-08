package com.positronen.events.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaceV2Response(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "name")
    val name: NameResponse,
    @SerialName(value = "source_type")
    val sourceType: SourceTypeResponse? = null,
    @SerialName(value = "info_url")
    val infoUrl: String? = null,
    @SerialName(value = "modified_at")
    val modifiedAt: String? = null,
    @SerialName(value = "location")
    val location: LocationResponse? = null,
    @SerialName(value = "description")
    val description: DescriptionResponse? = null,
    @SerialName(value = "tags")
    val tags: List<TagResponse>? = null,
    @SerialName(value = "extra_searchwords")
    val extraSearchwords: List<String>? = null,
    @SerialName(value = "opening_hours_url")
    val openingHoursUrl: String
)

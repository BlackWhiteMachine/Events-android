package com.positronen.events.data.model

import com.google.gson.annotations.SerializedName

data class EventsV1Response(
    @SerializedName(value = "id")
    val id: String,
    @SerializedName(value = "name")
    val name: NameResponse,
    @SerializedName(value = "source_type")
    val sourceType: SourceTypeResponse? = null,
    @SerializedName(value = "info_url")
    val infoUrl: String? = null,
    @SerializedName(value = "modified_at")
    val modifiedAt: String? = null,
    @SerializedName(value = "location")
    val location: LocationResponse? = null,
    @SerializedName(value = "description")
    val description: DescriptionResponse,
    @SerializedName(value = "tags")
    val tags: List<TagResponse>? = null,
    @SerializedName(value = "extra_searchwords")
    val extraSearchwords: List<String>? = null,
    @SerializedName(value = "opening_hours_url")
    val openingHoursUrl: String
)
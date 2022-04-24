package com.positronen.events.domain.model

data class PointDetailModel(
    val id: String,
    val pointType: PointType,
    val name: String,
    val description: String?,
    val images: List<ImageModel>,
    val location: LocationModel,
    val infoUrl: String?
)
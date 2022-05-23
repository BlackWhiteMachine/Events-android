package com.positronen.events.domain.model

sealed class ChannelEvent {

    data class SetMyLocation(
        val lat: Double,
        val lon: Double
    ) : ChannelEvent()

    data class AddPoint(
        val id: String,
        val name: String,
        val description: String?,
        val lat: Double,
        val lon: Double
    ) : ChannelEvent()

    data class RemovePoint(
        val idsList: List<String>
    ) : ChannelEvent()

    data class ShowBottomSheet(
        val id: String,
        val pointType: PointType
    ) : ChannelEvent()
}

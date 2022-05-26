package com.positronen.events.domain.model

import com.positronen.events.domain.model.quad_tree.BoundingBox

sealed class ChannelEvent {

    data class SetMyLocation(
        val lat: Double,
        val lon: Double
    ) : ChannelEvent()

    data class AddPoint(
        val id: String,
        val type: PointType,
        val name: String,
        val description: String?,
        val showInfoWindow: Boolean,
        val lat: Double,
        val lon: Double
    ) : ChannelEvent()

    data class RemovePoint(
        val idsList: List<String>
    ) : ChannelEvent()

    object ClearMap : ChannelEvent()

    data class MoveCamera(val box: BoundingBox) : ChannelEvent()

    data class ShowBottomSheet(
        val id: String,
        val pointType: PointType
    ) : ChannelEvent()
}

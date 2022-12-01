package com.positronen.events.presentation.main

import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.quad_tree.BoundingBox
import com.positronen.events.presentation.mvi.BaseEvent

sealed class MainEvent: BaseEvent() {

    data class SetMyLocation(
        val lat: Double,
        val lon: Double
    ) : MainEvent()

    data class AddPoint(
        val id: String,
        val type: PointType,
        val name: String,
        val description: String?,
        val showInfoWindow: Boolean,
        val lat: Double,
        val lon: Double
    ) : MainEvent()

    data class RemovePoint(
        val idsList: List<String>
    ) : MainEvent()

    object ClearMap : MainEvent()

    data class MoveCamera(val box: BoundingBox) : MainEvent()

    data class ShowBottomSheet(
        val id: String,
        val pointType: PointType
    ) : MainEvent()
}

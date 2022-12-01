package com.positronen.events.presentation.main

import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.presentation.mvi.BaseIntent

sealed class MainIntent: BaseIntent() {

    object MapClicked: MainIntent()
    object MapReady: MainIntent()
    object LocationPermissionGranted: MainIntent()

    data class MarkerClicked(val id: String, val type: PointType): MainIntent()

    data class CameraMoved(
        val zoomLevel: Int,
        val visibleRegion: MapRegionModel,
        val isMaxZoomLevel: Boolean
        ): MainIntent()

    data class PlaceFilterChanged(val isChecked: Boolean): MainIntent()
    data class EventsFilterChanged(val isChecked: Boolean): MainIntent()
    data class ActivitiesFilterChanged(val isChecked: Boolean): MainIntent()
}
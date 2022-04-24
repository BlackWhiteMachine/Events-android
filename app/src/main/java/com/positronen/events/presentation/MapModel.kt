package com.positronen.events.presentation

import com.positronen.events.domain.model.Source

data class MapModel(
    val placesSource: Source = Source.INIT,
    val eventsSource: Source = Source.INIT,
    val zoom: Int = 1,
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val radius: Float = 0f,
    val isPlaceEnabled: Boolean = false,
    val isEventsEnabled: Boolean = false,
    val isActivitiesEnabled: Boolean = false
)
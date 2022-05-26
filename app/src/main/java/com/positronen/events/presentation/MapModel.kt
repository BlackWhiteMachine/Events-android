package com.positronen.events.presentation

import com.positronen.events.domain.model.Source

data class MapModel(
    val placesSource: Source = Source.INIT,
    val eventsSource: Source = Source.INIT
)
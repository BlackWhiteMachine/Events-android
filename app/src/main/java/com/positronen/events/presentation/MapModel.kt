package com.positronen.events.presentation

import com.positronen.events.domain.model.DataResource
import com.positronen.events.domain.model.PointModel

data class MapModel(
    val placesSource: DataResource<List<PointModel>> = DataResource.Init,
    val eventsSource: DataResource<List<PointModel>> = DataResource.Init,
)
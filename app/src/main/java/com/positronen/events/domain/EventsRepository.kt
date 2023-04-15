package com.positronen.events.domain

import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow

interface EventsRepository {

    fun events(tileRegion: MapTileRegionModel): Flow<List<PointModel>>

    fun event(id: String): Flow<PointDetailModel>
}
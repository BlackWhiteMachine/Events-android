package com.positronen.events.domain

import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow

interface PlacesRepository {

    fun places(tileRegion: MapTileRegionModel): Flow<List<PointModel>>

    fun place(id: String): Flow<PointDetailModel>
}
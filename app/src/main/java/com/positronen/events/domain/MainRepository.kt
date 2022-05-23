package com.positronen.events.domain

import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    suspend fun places(tileRegion: MapTileRegionModel): List<PointModel>

    fun place(id: String): Flow<PointDetailModel>

    suspend fun events(tileRegion: MapTileRegionModel): List<PointModel>

    fun event(id: String): Flow<PointDetailModel>

    fun activities(start: Int, limit: Int): Flow<List<PointModel>>

    fun activity(id: String): Flow<PointDetailModel>
}
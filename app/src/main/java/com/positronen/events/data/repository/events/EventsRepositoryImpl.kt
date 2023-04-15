package com.positronen.events.data.repository.events

import com.positronen.events.data.converter.events.convertResponseEventDetail
import com.positronen.events.data.converter.events.convertResponseEvents
import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.EventsRepository
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.utils.radius
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class EventsRepositoryImpl @Inject constructor(
    private val service: MainService
) : EventsRepository {

    override fun events(tileRegion: MapTileRegionModel): Flow<List<PointModel>> = flow {
        val mapTile = tileRegion.mapRegionModel
        val centerLatitude = (mapTile.topLeftLatitude + mapTile.bottomRightLatitude) / 2
        val centerLongitude = (mapTile.topLeftLongitude + mapTile.bottomRightLatitude) / 2
        val distance = radius(
            firstLatitude = mapTile.topLeftLatitude,
            firstLongitude = mapTile.topLeftLongitude,
            secondLatitude = centerLatitude,
            secondLongitude = centerLongitude
        )

        val distanceFilter = DISTANCE_FILTER.format(centerLatitude, centerLongitude, distance)
        val resultList = mutableListOf<EventsV1Response>()
        var hasNext = true
        var start = 0
        while (hasNext) {
            val response = service.events(distanceFilter, start, PAGE_SIZE).data
            resultList.addAll(response)
            start += PAGE_SIZE
            hasNext = response.size == PAGE_SIZE
        }

        val events =  convertResponseEvents(resultList).filter { point ->
            tileRegion.mapRegionModel.isContains(
                point.location.latitude,
                point.location.longitude,
            )
        }

        emit(events)
    }

    override fun event(id: String): Flow<PointDetailModel> = flow {
        emit(service.event(id))
    }.mapNotNull(::convertResponseEventDetail)

    private companion object {
        const val DISTANCE_FILTER: String = "%.7f,%.7f,%.4f"
        const val PAGE_SIZE: Int = 50
    }
}
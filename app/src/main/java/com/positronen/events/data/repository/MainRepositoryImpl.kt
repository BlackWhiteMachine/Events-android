package com.positronen.events.data.repository

import android.location.Location
import com.positronen.events.data.converter.MainConverter
import com.positronen.events.data.converter.MainConverterImpl
import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.MainRepository
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val service: MainService
) : MainRepository {

    private val mainConverter: MainConverter = MainConverterImpl()

    override suspend fun places(tileRegion: MapTileRegionModel): List<PointModel> {
        val mapRegionModel = tileRegion.mapRegionModel
        val centerLatitude = (mapRegionModel.topLeftLatitude + mapRegionModel.bottomRightLatitude) / 2
        val centerLongitude = (mapRegionModel.topLeftLongitude + mapRegionModel.bottomRightLongitude) / 2
        val distance = radius(
            firstLatitude = mapRegionModel.topLeftLatitude,
            firstLongitude = mapRegionModel.topLeftLongitude,
            secondLatitude = centerLatitude,
            secondLongitude = centerLongitude
        )

        val distanceFilter = DISTANCE_FILTER.format(centerLatitude, centerLongitude, distance)
        val resultList = mutableListOf<PlaceV2Response>()
        var hasNext = true
        var start = 0

        while (hasNext) {
            val response = service.places(distanceFilter, start, PAGE_SIZE).data
            resultList.addAll(response)
            start += PAGE_SIZE
            hasNext = response.size == PAGE_SIZE
        }

        return mainConverter.convertResponsePlaces(resultList).filter {
            tileRegion.mapRegionModel.isContains(
                it.location.latitude,
                it.location.longitude,
            )
        }
    }

    override fun place(id: String): Flow<PointDetailModel> = flow {
        emit(service.place(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    override suspend fun events(tileRegion: MapTileRegionModel): List<PointModel>{
        val mapRegionModel = tileRegion.mapRegionModel
        val centerLatitude = (mapRegionModel.topLeftLatitude + mapRegionModel.bottomRightLatitude) / 2
        val centerLongitude = (mapRegionModel.topLeftLongitude + mapRegionModel.bottomRightLatitude) / 2
        val distance = radius(
            firstLatitude = mapRegionModel.topLeftLatitude,
            firstLongitude = mapRegionModel.topLeftLongitude,
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

        return mainConverter.convertResponseEvents(resultList).filter {
            tileRegion.mapRegionModel.isContains(
                it.location.latitude,
                it.location.longitude,
            )
        }
    }

    override fun event(id: String): Flow<PointDetailModel> = flow {
        emit(service.event(id))
    }.mapNotNull(mainConverter::convertResponseEventDetail)

    override fun activities(start: Int, limit: Int): Flow<List<PointModel>> = flow {
        emit(service.activities(start, limit).data)
    }.map(mainConverter::convertResponsePlaces)

    override fun activity(id: String): Flow<PointDetailModel> = flow {
        emit(service.activity(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    private fun radius(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            firstLatitude,
            firstLongitude,
            secondLatitude,
            secondLongitude,
            result
        )

        return result[0]/1000
    }

    private companion object {
        const val DISTANCE_FILTER: String = "%.7f,%.7f,%.4f"
        const val PAGE_SIZE: Int = 50
    }
}
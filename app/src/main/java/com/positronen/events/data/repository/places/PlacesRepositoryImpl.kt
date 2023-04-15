package com.positronen.events.data.repository.places

import com.positronen.events.data.converter.MainConverter
import com.positronen.events.data.converter.MainConverterImpl
import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.PlacesRepository
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.utils.radius
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor(
    private val service: MainService
) : PlacesRepository {

    private val mainConverter: MainConverter = MainConverterImpl()

    override fun places(tileRegion: MapTileRegionModel): Flow<List<PointModel>> = flow {
        val mapTile = tileRegion.mapRegionModel
        val centerLatitude = (mapTile.topLeftLatitude + mapTile.bottomRightLatitude) / 2
        val centerLongitude = (mapTile.topLeftLongitude + mapTile.bottomRightLongitude) / 2
        val distance = radius(
            firstLatitude = mapTile.topLeftLatitude,
            firstLongitude = mapTile.topLeftLongitude,
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

        val places = mainConverter.convertResponsePlaces(resultList).filter {
            tileRegion.mapRegionModel.isContains(
                it.location.latitude,
                it.location.longitude,
            )
        }

        emit(places)
    }

    override fun place(id: String): Flow<PointDetailModel> = flow {
        emit(service.place(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    private companion object {
        const val DISTANCE_FILTER: String = "%.7f,%.7f,%.4f"
        const val PAGE_SIZE: Int = 50
    }
}
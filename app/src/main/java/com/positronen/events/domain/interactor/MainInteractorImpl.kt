package com.positronen.events.domain.interactor

import com.positronen.events.domain.MainRepository
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.utils.getTopLeft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.RuntimeException
import javax.inject.Inject

class MainInteractorImpl @Inject constructor(
    private val mainRepository: MainRepository
) : MainInteractor {

    // TODO make DB cache
    private val placesCache: MutableMap<String, List<PointModel>> = mutableMapOf()
    private val eventsCache: MutableMap<String, List<PointModel>> = mutableMapOf()

    override val defaultDataZoomLevel: Int
        get() = DEFAULT_ZOOM_LEVEL

    override fun places(visibleTilesList: List<MapTileRegionModel>): Flow<List<PointModel>> = flow {
        visibleTilesList.forEach { mapRegionModel ->
            val places = placesCache[mapRegionModel.getName()]
                ?: getPlacesForTileRegionFromNetwork(mapRegionModel)

            emit(places)
        }
    }

    override fun events(visibleTilesList: List<MapTileRegionModel>): Flow<List<PointModel>> = flow {
        visibleTilesList.forEach { mapRegionModel ->
            val events = eventsCache[mapRegionModel.getName()]
                ?: getEventsForTileRegionFromNetwork(mapRegionModel)


            emit(events)
        }
    }

    override fun point(id: String, pointType: PointType): Flow<PointDetailModel> =
        when (pointType) {
            PointType.PLACE -> mainRepository.place(id)
            PointType.EVENT -> mainRepository.event(id)
            PointType.ACTIVITY -> TODO()
            else -> throw RuntimeException("No detail information about cluster")
        }


    private suspend fun getPlacesForTileRegionFromNetwork(mapTileRegionModel: MapTileRegionModel): List<PointModel> {
        return mainRepository.places(getTileRegion(mapTileRegionModel.xTile, mapTileRegionModel.yTile)).apply {
            placesCache[mapTileRegionModel.getName()] = this
        }
    }

    private suspend fun getEventsForTileRegionFromNetwork(mapTileRegionModel: MapTileRegionModel): List<PointModel> {
        return mainRepository.events(getTileRegion(mapTileRegionModel.xTile, mapTileRegionModel.yTile)).apply {
            eventsCache[mapTileRegionModel.getName()] = this
        }
    }

    private fun getTileRegion(xTile: Int, yTile: Int): MapTileRegionModel {
        val (topLeftTileLat, topLeftTileLon) = getTopLeft(xTile, yTile, DEFAULT_ZOOM_LEVEL)
        val (bottomRightTileLat, bottomRightTileLon) = getTopLeft(xTile + 1, yTile + 1, DEFAULT_ZOOM_LEVEL)

        return MapTileRegionModel(
            xTile = xTile,
            yTile = yTile,
            mapRegionModel = MapRegionModel(
                topLeftLatitude = topLeftTileLat,
                topLeftLongitude = topLeftTileLon,
                bottomRightLatitude = bottomRightTileLat,
                bottomRightLongitude = bottomRightTileLon
            )
        )
    }

    private companion object {
        const val DEFAULT_ZOOM_LEVEL: Int = 17
    }
}
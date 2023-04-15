package com.positronen.events.domain.interactor

import com.positronen.events.domain.EventsRepository
import com.positronen.events.domain.ActivitiesRepository
import com.positronen.events.domain.PlacesRepository
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.utils.getTopLeft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class MainInteractorImpl @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val eventsRepository: EventsRepository,
    private val activitiesRepository: ActivitiesRepository
) : MainInteractor {

    // TODO make DB cache
    private val placesCache: ConcurrentHashMap<String, List<PointModel>> = ConcurrentHashMap()
    private val eventsCache: MutableMap<String, List<PointModel>> = ConcurrentHashMap()

    override val defaultDataZoomLevel: Int
        get() = DEFAULT_ZOOM_LEVEL

    override fun places(visibleTilesList: List<MapTileRegionModel>): Flow<List<PointModel>> =
        visibleTilesList.asFlow()
            .flatMapMerge { mapRegionModel ->
                val places = placesCache[mapRegionModel.getName()]

                if (places != null) {
                    flowOf(places)
                } else {
                    getPlacesForTileRegionFromNetwork(mapRegionModel)
                }
            }.flowOn(Dispatchers.IO)

    override fun events(visibleTilesList: List<MapTileRegionModel>): Flow<List<PointModel>> =
        visibleTilesList.asFlow()
            .flatMapMerge { mapRegionModel ->
                val events = eventsCache[mapRegionModel.getName()]

                if (events != null) {
                    flowOf(events)
                } else {
                    getEventsForTileRegionFromNetwork(mapRegionModel)
                }
            }.flowOn(Dispatchers.IO)

    override fun point(id: String, pointType: PointType): Flow<PointDetailModel> =
        when (pointType) {
            PointType.PLACE -> placesRepository.place(id)
            PointType.EVENT -> eventsRepository.event(id)
            PointType.ACTIVITY -> TODO()
            else -> throw RuntimeException("No detail information about cluster")
        }.flowOn(Dispatchers.IO)


    private fun getPlacesForTileRegionFromNetwork(
        mapTile: MapTileRegionModel
    ): Flow<List<PointModel>> =
        placesRepository.places(getTileRegion(mapTile.xTile, mapTile.yTile)).onEach { pointModel ->
            placesCache[mapTile.getName()] = pointModel
        }

    private fun getEventsForTileRegionFromNetwork(
        mapTile: MapTileRegionModel
    ): Flow<List<PointModel>> =
        eventsRepository.events(getTileRegion(mapTile.xTile, mapTile.yTile)).onEach { pointModel ->
            eventsCache[mapTile.getName()] = pointModel
        }

    private fun getTileRegion(xTile: Int, yTile: Int): MapTileRegionModel {
        val (topLeftTileLat, topLeftTileLon) = getTopLeft(xTile, yTile, DEFAULT_ZOOM_LEVEL)
        val (bottomRightTileLat, bottomRightTileLon) = getTopLeft(
            xTile = xTile + 1,
            yTile = yTile + 1,
            zoom = DEFAULT_ZOOM_LEVEL
        )

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
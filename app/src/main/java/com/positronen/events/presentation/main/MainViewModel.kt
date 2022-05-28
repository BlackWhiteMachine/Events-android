@file:OptIn(ExperimentalCoroutinesApi::class)

package com.positronen.events.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.domain.model.ChannelEvent
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.Source
import com.positronen.events.domain.model.quad_tree.BoundingBox
import com.positronen.events.domain.model.quad_tree.QuadTree
import com.positronen.events.presentation.MainInteractor
import com.positronen.events.presentation.MapModel
import com.positronen.events.utils.Logger
import com.positronen.events.utils.getTileRegion
import com.positronen.events.utils.getTilesList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val mainInteractor: MainInteractor
) : ViewModel() {

    private val mapStateFlow = MutableStateFlow(MapModel())

    private var visibleRegion: MapRegionModel ?= null
    private var isMaxZoomLevel: Boolean = false
    private var lastSelectedPoint: String? = null
    private var visibleTiles: List<MapTileRegionModel>? = null
    private var isPlaceEnabled = false
    private var isEventsEnabled = false
    private var isActivitiesEnabled = false

    private var quadTree: QuadTree<String> = QuadTree(
        topRightX = 1f,
        topRightY = 1f,
        bottomLeftX = 0f,
        bottomLeftY = 0f
    )

    private val points: MutableList<PointModel> = mutableListOf()

    private val eventChannel = Channel<ChannelEvent>()
    val eventFlow: Flow<ChannelEvent>
        get() = eventChannel.receiveAsFlow()

    val showLoading: Flow<Boolean>
        get() = mapStateFlow.map {
            it.placesSource == Source.LOADING || it.eventsSource == Source.LOADING
        }

    fun onMapReady() {

    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            locationDataSource.location().collect { (latitude, longitude) ->
                eventChannel.send(ChannelEvent.SetMyLocation(latitude, longitude))
            }
        }
    }

    fun onMarkerClicked(id: String, type: PointType) {
        if (type == PointType.CLUSTER) {
            val box = clusters.find { it.first == id } ?: return

            viewModelScope.launch {
                eventChannel.send(ChannelEvent.MoveCamera(box.second))
            }
        } else {
            lastSelectedPoint = id
            viewModelScope.launch {
                eventChannel.send(
                    ChannelEvent.ShowBottomSheet(id, type)
                )
            }
        }
    }

    fun onMapClicked() {
        lastSelectedPoint = null
    }

    fun onCameraMoved(visibleRegion: MapRegionModel, isMaxZoomLevel: Boolean) {
        this.visibleRegion = visibleRegion
        this.isMaxZoomLevel = isMaxZoomLevel
        quadTree = QuadTree(
            topRightX = visibleRegion.bottomRightLongitude.toFloat(),
            topRightY = visibleRegion.topLeftLatitude.toFloat(),
            bottomLeftX = visibleRegion.topLeftLongitude.toFloat(),
            bottomLeftY = visibleRegion.bottomRightLatitude.toFloat(),
            levels = QUAD_TREE_LEVELS_NUMBER
        )

        val visibleTiles = getTilesList(
            visibleRegion = visibleRegion,
            zoom = mainInteractor.defaultDataZoomLevel
        )
            .map { (xTile, yTile) ->
                getTileRegion(xTile, yTile, mainInteractor.defaultDataZoomLevel)
            }

        this.visibleTiles = visibleTiles

        if (visibleTiles.isNotEmpty()) {
            if (isPlaceEnabled) obtainPlaces(visibleTiles)
            if (isEventsEnabled) obtainEvents(visibleTiles)
        }

        val removeList = mutableListOf<PointModel>()

        points.forEach {
            val contains = visibleRegion.isContains(it.location.latitude, it.location.longitude).not()

            if (contains) {
                removeList.add(it)
            }
        }

        points.removeAll(removeList)

        clusters.clear()

        viewModelScope.launch {
            eventChannel.send(ChannelEvent.ClearMap)
        }
    }

    fun onPlaceFilterChanged(checked: Boolean) {
        isPlaceEnabled = checked
        if (isPlaceEnabled) {
            visibleTiles?.let { obtainPlaces(it) }
        } else {
            val removeList = mutableListOf<PointModel>()
            points.forEach {
                if (it.pointType == PointType.PLACE) {
                    removeList.add(it)
                }
            }
            points.removeAll(removeList)
            mapStateFlow.value = mapStateFlow.value.copy(placesSource = Source.INIT)

            updatePointsOnMap(points)
        }
    }

    fun onEventsFilterChanged(checked: Boolean) {
        isEventsEnabled = checked
        if (isEventsEnabled) {
            visibleTiles?.let { obtainEvents(it) }
        } else {
            val removeList = mutableListOf<PointModel>()
            points.forEach {
                removeList.add(it)
            }
            points.removeAll(removeList)
            mapStateFlow.value = mapStateFlow.value.copy(eventsSource = Source.INIT)

            updatePointsOnMap(points)
        }
    }

    fun onActivitiesFilterChanged(checked: Boolean) {
        isActivitiesEnabled = checked
    }

    private fun obtainPlaces(visibleTilesList: List<MapTileRegionModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentVisibleRegion = visibleRegion
            mapStateFlow.value = mapStateFlow.value.copy(placesSource = Source.LOADING)

            val result = mutableListOf<PointModel>()

            mainInteractor.places(visibleTilesList)
                .catch { error ->
                    mapStateFlow.value = mapStateFlow.value.copy(
                        placesSource = Source.ERROR
                    )
                    Logger.exception(Exception(error.message))
                }
                .onCompletion {
                    val resultSource = if (isPlaceEnabled) {
                        Source.SUCCESS
                    } else {
                        Source.INIT
                    }
                    handleResult(result)
                    mapStateFlow.value = mapStateFlow.value.copy(
                        placesSource = resultSource
                    )
                }
                .collect { placesList ->
                    if (isPlaceEnabled && currentVisibleRegion == visibleRegion) {
                        result.addAll(placesList)
                    } else {
                        cancel()
                    }
                }
        }
    }

    private fun obtainEvents(visibleTilesList: List<MapTileRegionModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentVisibleRegion = visibleRegion
            mapStateFlow.value = mapStateFlow.value.copy(eventsSource = Source.LOADING)

            val result = mutableListOf<PointModel>()

            mainInteractor.events(visibleTilesList)
                .catch { error ->
                    mapStateFlow.value = mapStateFlow.value.copy(
                        eventsSource = Source.ERROR
                    )
                    Logger.exception(Exception(error.message))
                }
                .onCompletion {
                    val resultSource = if (isEventsEnabled) {
                        Source.SUCCESS
                    } else {
                        Source.INIT
                    }
                    handleResult(result)
                    mapStateFlow.value = mapStateFlow.value.copy(
                        eventsSource = resultSource
                    )
                }
                .collect { eventsList ->
                    if (isEventsEnabled && currentVisibleRegion == visibleRegion) {
                        result.addAll(eventsList)
                    } else {
                        cancel()
                    }
                }
        }
    }

    private fun handleResult(pointsList: List<PointModel>) {
        viewModelScope.launch {
            addPointsToMap(pointsList)
        }
    }

    private val clusters = mutableListOf<Pair<String, BoundingBox>>()

    private fun addPointsToMap(pointsList: List<PointModel>) {
        val visibleRegion = this.visibleRegion ?: return

        pointsList.forEach { pointModel ->
            if (points.find { it.id == pointModel.id } != null) return@forEach

            if (visibleRegion.isContains(
                    pointModel.location.latitude,
                    pointModel.location.longitude
                )
            ) {
                points.add(pointModel)
            }
        }

        updatePointsOnMap(points)
    }

    private fun updatePointsOnMap(pointsList: List<PointModel>) {
        if (clusters.isNotEmpty()) {
            viewModelScope.launch {
                eventChannel.send(ChannelEvent.RemovePoint(clusters.map { it.first }))
            }

            clusters.clear()
        }

        if(pointsList.size > 4.0.pow(QUAD_TREE_LEVELS_NUMBER) && isMaxZoomLevel.not()) {
            pointsList.forEach { pointModel ->
                quadTree.insert(
                    x = pointModel.location.longitude.toFloat(),
                    y = pointModel.location.latitude.toFloat(),
                    data = pointModel.id
                )
            }

            val warmMap = quadTree.warmMap()

            warmMap.forEach { node ->
                val nodePoints = node.getPoints()
                if (nodePoints.size == 1) {
                    val point = points.find { it.id == nodePoints.first().second }

                    point?.let {
                        viewModelScope.launch {
                            eventChannel.send(
                                ChannelEvent.AddPoint(
                                    id = point.id,
                                    type = point.pointType,
                                    name = point.name,
                                    description = point.description,
                                    showInfoWindow = point.id == lastSelectedPoint,
                                    lat = point.location.latitude,
                                    lon = point.location.longitude
                                )
                            )
                        }
                    }
                } else {
                    var resultX = 0f
                    var resultY = 0f
                    nodePoints.forEach {
                        resultX += it.first.x / nodePoints.size
                        resultY += it.first.y / nodePoints.size
                    }

                    clusters.add(node.id to node.boundingBox)

                    viewModelScope.launch {
                        eventChannel.send(
                            ChannelEvent.AddPoint(
                                id = node.id,
                                type = PointType.CLUSTER,
                                name = points.size.toString(),
                                description = null,
                                showInfoWindow = false,
                                lat = resultY.toDouble(),
                                lon = resultX.toDouble()
                            )
                        )
                    }
                }
            }
        } else {
            pointsList.forEach { pointModel ->
                viewModelScope.launch {
                    eventChannel.send(
                        ChannelEvent.AddPoint(
                            id = pointModel.id,
                            type = pointModel.pointType,
                            name = pointModel.name,
                            description = pointModel.description,
                            showInfoWindow = pointModel.id == lastSelectedPoint,
                            lat = pointModel.location.latitude,
                            lon = pointModel.location.longitude
                        )
                    )
                }
            }
        }
    }

    private companion object {
        const val QUAD_TREE_LEVELS_NUMBER: Int = 2
    }
}
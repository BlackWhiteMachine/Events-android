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
import com.positronen.events.domain.interactor.MainInteractor
import com.positronen.events.presentation.MapModel
import com.positronen.events.utils.Logger
import com.positronen.events.utils.getTileRegion
import com.positronen.events.utils.getTilesList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject
import kotlin.math.pow

class MainViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val mainInteractor: MainInteractor
) : ViewModel() {

    private val platesStateFlow = MutableStateFlow(Source.INIT)
    private val eventsStateFlow = MutableStateFlow(Source.INIT)
    private val mapStateFlow = MutableStateFlow(MapModel())

    private var visibleRegion: MapRegionModel ?= null
    private var isMaxZoomLevel: Boolean = false
    private var lastSelectedPoint: String? = null
    private var visibleTiles: List<MapTileRegionModel>? = null
    private var isPlaceEnabled = false
    private var placesJob: Job? = null
    private var isEventsEnabled = false
    private var eventsJob: Job? = null
    private var isActivitiesEnabled = false

    private var quadTree: QuadTree<String> = QuadTree(
        topRightX = 1f,
        topRightY = 1f,
        bottomLeftX = 0f,
        bottomLeftY = 0f
    )

    private val points: MutableList<PointModel> = mutableListOf()

    private val eventChannel = MutableSharedFlow<ChannelEvent>()
    val eventFlow: SharedFlow<ChannelEvent>
        get() = eventChannel

    val showLoading: Flow<Boolean>
        get() = combine(platesStateFlow, eventsStateFlow) { platesState, eventsState ->
        platesState == Source.LOADING || eventsState == Source.LOADING
    }

    fun onMapReady() {

    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            locationDataSource.location().collect { (latitude, longitude) ->
                eventChannel.emit(ChannelEvent.SetMyLocation(latitude, longitude))
            }
        }
    }

    fun onMarkerClicked(id: String, type: PointType) {
        if (type == PointType.CLUSTER) {
            val box = clusters.find { it.first == id } ?: return

            viewModelScope.launch {
                eventChannel.emit(ChannelEvent.MoveCamera(box.second))
            }
        } else {
            lastSelectedPoint = id
            viewModelScope.launch {
                eventChannel.emit(
                    ChannelEvent.ShowBottomSheet(id, type)
                )
            }
        }
    }

    fun onMapClicked() {
        lastSelectedPoint = null
    }

    fun onCameraMoved(zoomLevel: Int, visibleRegion: MapRegionModel, isMaxZoomLevel: Boolean) {
        this.visibleRegion = visibleRegion
        this.isMaxZoomLevel = isMaxZoomLevel

        if (zoomLevel < MIN_ZOOM_LEVEL) return

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


        Logger.debug("MainViewModel: onCameraMoved: visibleTiles: ${visibleTiles.size}")

        this.visibleTiles = visibleTiles

        if (visibleTiles.isNotEmpty()) {
            if (isPlaceEnabled) {
                obtainPlaces(visibleTiles)
            }
            if (isEventsEnabled) {
                obtainEvents(visibleTiles)
            }
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
            eventChannel.emit(ChannelEvent.ClearMap)
        }
    }

    fun onPlaceFilterChanged(checked: Boolean) {
        isPlaceEnabled = checked
        if (isPlaceEnabled) {
            visibleTiles?.let {
                obtainPlaces(it)
            }
        } else {
            placesJob?.cancel()
            placesJob = null

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
            visibleTiles?.let {
                obtainEvents(it)
            }
        } else {
            eventsJob?.cancel()
            eventsJob = null

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

    private fun obtainPlaces(visibleTiles: List<MapTileRegionModel>) {
        placesJob?.cancel()
        placesJob = obtainPoints(mainInteractor.places(visibleTiles), platesStateFlow) { placesList ->
            handleResult(placesList)
        }
    }

    private fun obtainEvents(visibleTiles: List<MapTileRegionModel>) {
        eventsJob?.cancel()
        eventsJob = obtainPoints(mainInteractor.events(visibleTiles), eventsStateFlow) { eventsList ->
            handleResult(eventsList)
        }
    }

    private fun obtainPoints(
        pointsSource: Flow<List<PointModel>>,
        stateFlow: MutableStateFlow<Source>,
        onResult: (List<PointModel>) -> Unit
    ): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            stateFlow.value = Source.LOADING

            val result = mutableListOf<PointModel>()

            pointsSource
                .catch { error ->
                    Logger.exception(Exception(error.message))
                }
                .onCompletion {
                    when (it) {
                        null -> {
                            stateFlow.value = Source.SUCCESS
                            onResult.invoke(result)
                        }
                        is CancellationException -> {
                            stateFlow.value = Source.INIT
                        }
                        else -> {
                            stateFlow.value = Source.ERROR
                        }
                    }
                }
                .collect { placesList ->
                    stateFlow.value = Source.LOADING
                    result.addAll(placesList)
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
                eventChannel.emit(ChannelEvent.RemovePoint(clusters.map { it.first }))
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
                            eventChannel.emit(
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
                        eventChannel.emit(
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
                    eventChannel.emit(
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
        const val MIN_ZOOM_LEVEL: Int = 11
        const val QUAD_TREE_LEVELS_NUMBER: Int = 2
    }
}
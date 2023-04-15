package com.positronen.events.presentation.main

import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.domain.model.quad_tree.BoundingBox
import com.positronen.events.domain.model.quad_tree.QuadTree
import com.positronen.events.domain.interactor.MainInteractor
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.Source
import com.positronen.events.presentation.MapModel
import com.positronen.events.presentation.mvi.BaseMVIViewModel
import com.positronen.events.utils.Logger
import com.positronen.events.utils.getTileRegion
import com.positronen.events.utils.getTilesList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject
import kotlin.math.pow

class MainViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val mainInteractor: MainInteractor
) : BaseMVIViewModel<MainState, MainEvent, MainIntent>(
    MainState.Init
) {

    private val platesStateFlow = MutableStateFlow(Source.INIT)
    private val eventsStateFlow = MutableStateFlow(Source.INIT)
    private val mapStateFlow = MutableStateFlow(MapModel())

    private var visibleRegion: MapRegionModel?= null
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

    override fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.CameraMoved -> onCameraMoved(intent)
            MainIntent.LocationPermissionGranted -> onLocationPermissionGranted()
            MainIntent.MapClicked -> onMapClicked()
            MainIntent.MapReady -> onMapReady()
            is MainIntent.MarkerClicked -> onMarkerClicked(intent)
            is MainIntent.PlaceFilterChanged -> onPlaceFilterChanged(intent)
            is MainIntent.EventsFilterChanged -> onEventsFilterChanged(intent)
            is MainIntent.ActivitiesFilterChanged -> onActivitiesFilterChanged(intent)
        }
    }

    private val showLoading: Flow<Boolean>
        get() = combine(platesStateFlow, eventsStateFlow) { platesState, eventsState ->
        platesState == Source.LOADING || eventsState == Source.LOADING
    }

    private fun onMapReady() {
        baseCoroutineScope.launch {
            showLoading.collect { isShowing ->
                updateState {
                    MainState.Loading(isShowing)
                }
            }
        }
    }

    private fun onLocationPermissionGranted() {
        baseCoroutineScope.launch {
            locationDataSource.location().collect { (latitude, longitude) ->
                sendEvent(MainEvent.SetMyLocation(latitude, longitude))
            }
        }
    }

    private fun onMarkerClicked(markerClicked: MainIntent.MarkerClicked) {
        if (markerClicked.type == PointType.CLUSTER) {
            val box = clusters.find { it.first == markerClicked.id } ?: return

            sendEvent(MainEvent.MoveCamera(box.second))
        } else {
            lastSelectedPoint = markerClicked.id
            sendEvent(MainEvent.ShowBottomSheet(markerClicked.id, markerClicked.type))
        }
    }

    private fun onMapClicked() {
        lastSelectedPoint = null
    }


    private fun onCameraMoved(cameraMoved: MainIntent.CameraMoved) {
        this.visibleRegion = cameraMoved.visibleRegion
        this.isMaxZoomLevel = cameraMoved.isMaxZoomLevel

        if (cameraMoved.zoomLevel < MIN_ZOOM_LEVEL) return

        quadTree = QuadTree(
            topRightX = cameraMoved.visibleRegion.bottomRightLongitude.toFloat(),
            topRightY = cameraMoved.visibleRegion.topLeftLatitude.toFloat(),
            bottomLeftX = cameraMoved.visibleRegion.topLeftLongitude.toFloat(),
            bottomLeftY = cameraMoved.visibleRegion.bottomRightLatitude.toFloat(),
            levels = QUAD_TREE_LEVELS_NUMBER
        )

        val visibleTiles = getTilesList(
            visibleRegion = cameraMoved.visibleRegion,
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
            val contains = cameraMoved.visibleRegion.isContains(it.location.latitude, it.location.longitude).not()

            if (contains) {
                removeList.add(it)
            }
        }

        points.removeAll(removeList)

        clusters.clear()

        sendEvent(MainEvent.ClearMap)
    }

    private fun onPlaceFilterChanged(placeFilterChanged: MainIntent.PlaceFilterChanged) {
        isPlaceEnabled = placeFilterChanged.isChecked
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

    private fun onEventsFilterChanged(eventsFilterChanged: MainIntent.EventsFilterChanged) {
        isEventsEnabled = eventsFilterChanged.isChecked
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

    private fun onActivitiesFilterChanged(activitiesFilterChanged: MainIntent.ActivitiesFilterChanged) {
        isActivitiesEnabled = activitiesFilterChanged.isChecked
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
        return baseCoroutineScope.launch(Dispatchers.IO) {
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
        baseCoroutineScope.launch {
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
            sendEvent(MainEvent.RemovePoint(clusters.map { it.first }))

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
                        sendEvent(
                            MainEvent.AddPoint(
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
                } else {
                    var resultX = 0f
                    var resultY = 0f
                    nodePoints.forEach {
                        resultX += it.first.x / nodePoints.size
                        resultY += it.first.y / nodePoints.size
                    }

                    clusters.add(node.id to node.boundingBox)

                    sendEvent(
                        MainEvent.AddPoint(
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
        } else {
            pointsList.forEach { pointModel ->
                sendEvent(
                    MainEvent.AddPoint(
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

    private companion object {
        const val MIN_ZOOM_LEVEL: Int = 12
        const val QUAD_TREE_LEVELS_NUMBER: Int = 2
    }
}
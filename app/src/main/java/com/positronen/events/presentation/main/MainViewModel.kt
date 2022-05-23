@file:OptIn(ExperimentalCoroutinesApi::class)

package com.positronen.events.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.domain.model.ChannelEvent
import com.positronen.events.domain.model.DataResource
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.domain.model.MapTileRegionModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val mainInteractor: MainInteractor
) : ViewModel() {

    private val mapStateFlow = MutableStateFlow(MapModel())

    private var visibleRegion: MapRegionModel ?= null
    private var visibleTiles: List<MapTileRegionModel>? = null
    private var isPlaceEnabled = false
    private var isEventsEnabled = false
    private var isActivitiesEnabled = false

    private val points: MutableList<PointModel> = mutableListOf()

    private val eventChannel = Channel<ChannelEvent>()
    val eventFlow: Flow<ChannelEvent>
        get() = eventChannel.receiveAsFlow()

    val showLoading: Flow<Boolean>
        get() = mapStateFlow.map {
            it.placesSource is DataResource.Loading || it.eventsSource is DataResource.Loading
        }

    fun onMapReady() {
        val placesSourceFlow = mapStateFlow.mapNotNull {
            (it.placesSource as? DataResource.Success)?.data
        }
        val eventsSourceFlow = mapStateFlow.mapNotNull {
            (it.eventsSource as? DataResource.Success)?.data
        }
        viewModelScope.launch {
            merge(placesSourceFlow, eventsSourceFlow).collect { pointsList ->
                addPointsToMap(pointsList)
            }
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            locationDataSource.location().collect { (latitude, longitude) ->
                eventChannel.send(ChannelEvent.SetMyLocation(latitude, longitude))
            }
        }
    }

    fun onMarkerClicked(id: String) {
        val pointType = points.find { it.id == id }?.pointType ?: return
        viewModelScope.launch {
            eventChannel.send(
                ChannelEvent.ShowBottomSheet(id, pointType)
            )
        }
    }

    fun onCameraMoved(visibleRegion: MapRegionModel) {
        this.visibleRegion = visibleRegion

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

        viewModelScope.launch {
            eventChannel.send(ChannelEvent.RemovePoint(removeList.map { it.id }))
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
            viewModelScope.launch {
                eventChannel.send(ChannelEvent.RemovePoint(removeList.map { it.id }))
            }
            points.removeAll(removeList)
            mapStateFlow.value = mapStateFlow.value.copy(placesSource = DataResource.Init)
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
            viewModelScope.launch {
                eventChannel.send(ChannelEvent.RemovePoint(removeList.map { it.id }))
            }
            points.removeAll(removeList)
            mapStateFlow.value = mapStateFlow.value.copy(eventsSource = DataResource.Init)
        }
    }

    fun onActivitiesFilterChanged(checked: Boolean) {
        isActivitiesEnabled = checked
    }

    private fun obtainPlaces(visibleTilesList: List<MapTileRegionModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            mapStateFlow.value = mapStateFlow.value.copy(placesSource = DataResource.Loading)

            val result = mutableListOf<PointModel>()

            mainInteractor.places(visibleTilesList)
                .catch { error ->
                    mapStateFlow.value = mapStateFlow.value.copy(
                        placesSource = DataResource.Error(error)
                    )
                    Logger.exception(Exception(error.message))
                }
                .onCompletion {
                    val resultDataResource = if (isPlaceEnabled) {
                        DataResource.Success(result)
                    } else {
                        DataResource.Init
                    }
                    mapStateFlow.value = mapStateFlow.value.copy(
                        placesSource = resultDataResource
                    )
                }
                .collect { placesList ->
                    if (isPlaceEnabled) {
                        result.addAll(placesList)
                    } else {
                        cancel()
                    }
                }
        }
    }

    private fun obtainEvents(visibleTilesList: List<MapTileRegionModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            mapStateFlow.value = mapStateFlow.value.copy(eventsSource = DataResource.Loading)

            val result = mutableListOf<PointModel>()

            mainInteractor.events(visibleTilesList)
                .catch { error ->
                    mapStateFlow.value = mapStateFlow.value.copy(
                        eventsSource = DataResource.Error(error)
                    )
                    Logger.exception(Exception(error.message))
                }
                .onCompletion {
                    val resultDataResource = if (isPlaceEnabled) {
                        DataResource.Success(result)
                    } else {
                        DataResource.Init
                    }
                    mapStateFlow.value = mapStateFlow.value.copy(
                        eventsSource = resultDataResource
                    )
                }
                .collect { eventsList ->
                    if (isEventsEnabled) {
                        result.addAll(eventsList)
                    } else {
                        cancel()
                    }
                }
        }
    }

    private fun addPointsToMap(pointsList: List<PointModel>) {
        val visibleRegion = this.visibleRegion ?: return

        pointsList.forEach { pointModel ->
            if (points.find { it.id == pointModel.id } != null) return@forEach

            if (visibleRegion.isContains(pointModel.location.latitude, pointModel.location.longitude)) {
                points.add(pointModel)

                viewModelScope.launch {
                    eventChannel.send(
                        ChannelEvent.AddPoint(
                            id = pointModel.id,
                            name = pointModel.name,
                            description = pointModel.description,
                            lat = pointModel.location.latitude,
                            lon = pointModel.location.longitude
                        )
                    )
                }
            }
        }
    }
}
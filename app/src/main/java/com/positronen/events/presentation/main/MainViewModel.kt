package com.positronen.events.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.positronen.events.data.location.LocationDataSource
import com.positronen.events.domain.model.ChannelEvent
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.Source
import com.positronen.events.presentation.MainInteractor
import com.positronen.events.presentation.MapModel
import com.positronen.events.presentation.map.VisibleRegionWrapper
import com.positronen.events.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val mapState = MutableStateFlow(MapModel())

    private val points: MutableList<PointModel> = mutableListOf()

    private val eventChannel = Channel<ChannelEvent>()
    val eventFlow: Flow<ChannelEvent>
        get() = eventChannel.receiveAsFlow()

    val showLoading: Flow<Boolean>
        get() = mapState.map { it.placesSource == Source.LOADING || it.eventsSource == Source.LOADING }

    fun onMapReady() {
        viewModelScope.launch {
            mapState.distinctUntilChangedBy { it.isPlaceEnabled }
                .collect { mapModel ->
                    if (mapModel.isPlaceEnabled) {
                        obtainPlaces(mapModel)
                    } else {
                        val removeList = mutableListOf<PointModel>()
                        points.forEach {
                            if (it.pointType == PointType.PLACE) {
                                eventChannel.send(ChannelEvent.RemovePoint(it.id))
                                removeList.add(it)
                            }
                        }
                        points.removeAll(removeList)
                        mapState.value = mapState.value.copy(placesSource = Source.INIT)
                    }
            }
        }

        viewModelScope.launch {
            mapState.distinctUntilChangedBy { it.isEventsEnabled }
                .collect { mapModel ->
                    if (mapModel.isEventsEnabled) {
                        obtainEvents(mapModel)
                    } else {
                        val removeList = mutableListOf<PointModel>()
                        points.forEach {
                            eventChannel.send(ChannelEvent.RemovePoint(it.id))
                            removeList.add(it)

                        }
                        points.removeAll(removeList)
                        mapState.value = mapState.value.copy(eventsSource = Source.INIT)
                    }
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

    fun onCameraMoved(visibleRegionWrapper: VisibleRegionWrapper, zoom: Int) {
        val newMapState = mapState.value.copy(
            zoom = zoom,
            centerLatitude = visibleRegionWrapper.centerLatitude(),
            centerLongitude = visibleRegionWrapper.centerLongitude(),
            radius = visibleRegionWrapper.radius()
        )

        if (zoom > mapState.value.zoom) {
            viewModelScope.launch {
                if (newMapState.isPlaceEnabled) obtainPlaces(newMapState)
                if (newMapState.isEventsEnabled) obtainEvents(newMapState)
            }
        } else if (zoom == mapState.value.zoom) {
            val removeList = mutableListOf<PointModel>()

            points.forEach {
                val contains = visibleRegionWrapper.contains(it.location)

                if (contains.not()) {
                    viewModelScope.launch {
                        eventChannel.send(ChannelEvent.RemovePoint(it.id))
                    }
                    removeList.add(it)
                }
            }

            removeList.forEach { points.remove(it) }

            if (newMapState.isPlaceEnabled) obtainPlaces(newMapState)
            if (newMapState.isEventsEnabled) obtainEvents(newMapState)
        }

        mapState.value = newMapState
    }

    fun onPlaceFilterChanged(checked: Boolean) {
        mapState.value = mapState.value.copy(isPlaceEnabled = checked)
    }

    fun onEventsFilterChanged(checked: Boolean) {
        mapState.value = mapState.value.copy(isEventsEnabled = checked)
    }

    fun onActivitiesFilterChanged(checked: Boolean) {
        mapState.value = mapState.value.copy(isActivitiesEnabled = checked)
    }

    private fun obtainPlaces(mapModel: MapModel) {
        mapState.value = mapState.value.copy(placesSource = Source.LOADING)

        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.places(mapModel.centerLatitude, mapModel.centerLongitude, mapModel.radius)
                .catch { error ->
                    Logger.exception(Exception(error.message))
                }
                .collect { placesList ->
                    mapState.value = mapState.value.copy(placesSource = Source.SUCCESS)

                    addPointsToMap(placesList)
                }
        }
    }

    private fun obtainEvents(mapModel: MapModel) {
        mapState.value = mapState.value.copy(eventsSource = Source.LOADING)

        viewModelScope.launch(Dispatchers.IO) {
            mainInteractor.events(mapModel.centerLatitude, mapModel.centerLongitude, mapModel.radius)
                .catch { error ->
                    Logger.exception(Exception(error.message))
                }
                .collect { placesList ->
                    mapState.value = mapState.value.copy(eventsSource = Source.SUCCESS)

                    addPointsToMap(placesList)
                }
        }
    }

    private suspend fun addPointsToMap(pointsList: List<PointModel>) {
        pointsList.forEach { pointModel ->
            if (points.find { it.id == pointModel.id } != null) return@forEach

            points.add(pointModel)

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

    private companion object {
        const val MIN_ZOOM_LEVEL: Int = 13
    }
}
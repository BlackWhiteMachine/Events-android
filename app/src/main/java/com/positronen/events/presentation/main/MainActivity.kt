package com.positronen.events.presentation.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.positronen.events.EventsApplication
import com.positronen.events.R
import com.positronen.events.databinding.ActivityMapsBinding
import com.positronen.events.domain.model.MapRegionModel
import com.positronen.events.presentation.detail.DetailInfoDialogFragment
import com.positronen.events.presentation.mvi.BaseMVIActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainActivity : BaseMVIActivity<MainState, MainEvent, MainIntent, MainViewModel>() {

    private var map: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityBinding = ActivityMapsBinding.inflate(layoutInflater)
        binding = activityBinding
        setContentView(activityBinding.root)

        (application as EventsApplication).component.inject(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(::onMapReady)

        activityBinding.initListeners()
    }

    override fun onDestroy() {
        super.onDestroy()

        map = null
        binding = null
    }

    override fun handleState(state: MainState) {
        when (state) {
            is MainState.Loading -> {
                binding?.let { it.loaderProgressBar.isVisible = state.isShowing }
            }
            MainState.Init -> { }
        }
    }

    override fun handleEvent(state: MainEvent) {
        when (state) {
            is MainEvent.SetMyLocation -> setMyLocation(state)
            is MainEvent.AddPoint -> addPoint(state)
            is MainEvent.RemovePoint -> removePoint(state)
            is MainEvent.ClearMap -> clearMap()
            is MainEvent.MoveCamera -> moveCamera(state)
            is MainEvent.ShowBottomSheet -> showBottomSheet(state)
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        googleMap.initMap()
    }

    private fun GoogleMap.initMap() {
        setOnMarkerClickListener { marker ->
            (marker.tag as? MainEvent.AddPoint)?.let { event ->
                viewModel.sendIntent(MainIntent.MarkerClicked(event.id, event.type))
            }
            false
        }
        setOnMapClickListener {
            viewModel.sendIntent(MainIntent.MapClicked)
        }

        setOnCameraMoveListener(object : GoogleMap.OnCameraMoveListener {

            private val cameraMovedChannel = MutableSharedFlow<Unit>()
            private val cameraMovedFlow: Flow<Unit>
                get() = cameraMovedChannel

            init {
                baseCoroutineScope.launchWhenStarted {
                    cameraMovedChannel.emit(Unit)
                }
                baseCoroutineScope.launchWhenStarted {
                    cameraMovedFlow.debounce(Duration.Companion.milliseconds(DEBOUNCE_MILLIS))
                        .collect {
                            val visibleRegion = projection.visibleRegion

                            viewModel.sendIntent(
                                MainIntent.CameraMoved(
                                    cameraPosition.zoom.toInt(),
                                    MapRegionModel(
                                        topLeftLatitude = visibleRegion.farLeft.latitude,
                                        topLeftLongitude = visibleRegion.farLeft.longitude,
                                        bottomRightLatitude = visibleRegion.nearRight.latitude,
                                        bottomRightLongitude = visibleRegion.nearRight.longitude
                                    ),
                                    isMaxZoomLevel = maxZoomLevel == cameraPosition.zoom,
                                )
                            )
                        }
                }
            }

            override fun onCameraMove() {
                baseCoroutineScope.launch {
                    cameraMovedChannel.emit(Unit)
                }
            }
        })

        uiSettings.isCompassEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isZoomControlsEnabled = true
        moveCamera(CameraUpdateFactory.zoomTo(18F))

        showRequestPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.string.main_activity_request_permission_rationale_message,
            requestLocationPermissionLauncher
        )

        mviViewModel
        viewModel.sendIntent(MainIntent.MapReady)
    }

    private fun ActivityMapsBinding.initListeners() {
        placeCheckedTextView.setOnClickListener {
            val isChecked = placeCheckedTextView.isChecked.not()
            placeCheckedTextView.isChecked = isChecked
            viewModel.sendIntent(MainIntent.PlaceFilterChanged(isChecked))
        }
        eventsCheckedTextView.setOnClickListener {
            val isChecked = eventsCheckedTextView.isChecked.not()
            eventsCheckedTextView.isChecked = isChecked
            viewModel.sendIntent(MainIntent.EventsFilterChanged(isChecked))
        }
        activitiesCheckedTextView.setOnClickListener {
            val isChecked = activitiesCheckedTextView.isChecked.not()
            activitiesCheckedTextView.isChecked = isChecked
            viewModel.sendIntent(MainIntent.ActivitiesFilterChanged(isChecked))
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onLocationPermissionGranted()
            } else {
            }
        }

    private fun showRequestPermission(
        permission: String,
        @StringRes messageId: Int? = null,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
        val showMessage = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        when {
            permissionCheck == PackageManager.PERMISSION_GRANTED -> {
                onLocationPermissionGranted()
            }
            messageId != null && showMessage -> {
                showPermissionRationaleDialog(permission, messageId, requestPermissionLauncher)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showPermissionRationaleDialog(
        permission: String,
        @StringRes messageId: Int,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        AlertDialog.Builder(this)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        map?.isMyLocationEnabled = true

        viewModel.sendIntent(MainIntent.LocationPermissionGranted)
    }

    private fun showBottomSheet(event: MainEvent.ShowBottomSheet) {
        val bottomSheetFragment = DetailInfoDialogFragment()
        val bundle = Bundle()
        bundle.putString(DetailInfoDialogFragment.ID_AGR, event.id)
        bundle.putInt(DetailInfoDialogFragment.POINT_TYPE_AGR, event.pointType.ordinal)
        bottomSheetFragment.arguments = bundle
        bottomSheetFragment.show(supportFragmentManager, "Detail")
    }

    private fun setMyLocation(event: MainEvent.SetMyLocation) {
        val point = LatLng(event.lat, event.lon)

        map?.moveCamera(CameraUpdateFactory.newLatLng(point))
    }

    private val markersMap: MutableMap<String, Marker> = mutableMapOf()

    private fun addPoint(event: MainEvent.AddPoint) {
        val markerOptions = MarkerOptions()
            .position(LatLng(event.lat, event.lon))
            .title(event.name)
        event.description?.let {
            markerOptions.snippet(it)
        }
        val marker = map?.addMarker(markerOptions)
        marker?.let {
            it.tag = event
            if (event.showInfoWindow) {
                it.showInfoWindow()
            }
            markersMap[event.id] = it
        }
    }

    private fun removePoint(channelEvent: MainEvent.RemovePoint) {
        channelEvent.idsList.forEach { id ->
            markersMap.remove(id)?.remove()
        }
    }

    private fun clearMap() {
        markersMap.values.forEach { marker ->
            marker.remove()
        }

        markersMap.clear()
    }

    private fun moveCamera(channelEvent: MainEvent.MoveCamera) {
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds(
                    LatLng(
                        channelEvent.box.bottomLeft.y.toDouble(),
                        channelEvent.box.bottomLeft.x.toDouble()
                    ),
                    LatLng(
                        channelEvent.box.topRight.y.toDouble(),
                        channelEvent.box.topRight.x.toDouble()
                    )
                ),
                10)
        )
    }

    private companion object {
        const val DEBOUNCE_MILLIS: Int = 500
    }
}
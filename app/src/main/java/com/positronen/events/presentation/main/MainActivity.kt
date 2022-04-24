package com.positronen.events.presentation.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.positronen.events.R
import com.positronen.events.databinding.ActivityMapsBinding
import com.positronen.events.domain.model.ChannelEvent
import com.positronen.events.presentation.detail.DetailInfoDialogFragment
import com.positronen.events.presentation.map.VisibleRegionWrapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.initListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMarkerClickListener { marker ->
            (marker.tag as? String)?.let {
                viewModel.onMarkerClicked(it)
            }
            false
        }

        map.setOnCameraMoveListener(object : GoogleMap.OnCameraMoveListener {

            private val cameraMovedChannel = Channel<Unit>()
            private val cameraMovedFlow: Flow<Unit>
                get() = cameraMovedChannel.receiveAsFlow()

            init {
                lifecycleScope.launchWhenStarted {
                    cameraMovedChannel.send(Unit)
                }
                lifecycleScope.launchWhenStarted {
                    cameraMovedFlow.debounce(Duration.Companion.milliseconds(DEBOUNCE_MILLIS))
                        .collect {
                            val visibleRegionWrapper = VisibleRegionWrapper(map.projection.visibleRegion)

                            viewModel.onCameraMoved(
                                visibleRegionWrapper,
                                map.cameraPosition.zoom.roundToInt()
                            )
                        }
                }
            }

            override fun onCameraMove() {
                lifecycleScope.launch {
                    cameraMovedChannel.send(Unit)
                }
            }
        })

        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera(CameraUpdateFactory.zoomTo(18F))

        initObserver()
        showRequestPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.string.main_activity_request_permission_rationale_message,
            requestLocationPermissionLauncher
        )

        viewModel.onMapReady()
    }

    private fun ActivityMapsBinding.initListeners() {
        placeCheckedTextView.setOnClickListener {
            val isChecked = placeCheckedTextView.isChecked.not()
            placeCheckedTextView.isChecked = isChecked
            viewModel.onPlaceFilterChanged(isChecked)
        }
        eventsCheckedTextView.setOnClickListener {
            val isChecked = eventsCheckedTextView.isChecked.not()
            eventsCheckedTextView.isChecked = isChecked
            viewModel.onEventsFilterChanged(isChecked)
        }
        activitiesCheckedTextView.setOnClickListener {
            val isChecked = activitiesCheckedTextView.isChecked.not()
            activitiesCheckedTextView.isChecked = isChecked
            viewModel.onActivitiesFilterChanged(isChecked)
        }
    }

    private fun initObserver() {
        lifecycleScope.launchWhenStarted {
            viewModel.eventFlow.collect { channelEvent ->
                when (channelEvent) {
                    is ChannelEvent.SetMyLocation -> setMyLocation(channelEvent)
                    is ChannelEvent.AddPoint -> addPoint(channelEvent)
                    is ChannelEvent.RemovePoint -> removePoint(channelEvent)
                    is ChannelEvent.ShowBottomSheet -> showBottomSheet(channelEvent)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.showLoading.collect { isShowing ->
                binding.loaderProgressBar.isVisible = isShowing
            }
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
        map.isMyLocationEnabled = true

        viewModel.onLocationPermissionGranted()
    }

    private fun showBottomSheet(event: ChannelEvent.ShowBottomSheet) {
        val bottomSheetFragment = DetailInfoDialogFragment()
        val bundle = Bundle()
        bundle.putString(DetailInfoDialogFragment.ID_AGR, event.id)
        bundle.putInt(DetailInfoDialogFragment.POINT_TYPE_AGR, event.pointType.ordinal)
        bottomSheetFragment.arguments = bundle
        bottomSheetFragment.show(supportFragmentManager, "Detail")
    }

    private fun setMyLocation(event: ChannelEvent.SetMyLocation) {
        val point = LatLng(event.lat, event.lon)

        map.moveCamera(CameraUpdateFactory.newLatLng(point))
    }

    private val markersMap: MutableMap<String, Marker> = mutableMapOf()

    private fun addPoint(event: ChannelEvent.AddPoint) {
        val markerOptions = MarkerOptions()
            .position(LatLng(event.lat, event.lon))
            .title(event.name)
        event.description?.let {
            markerOptions.snippet(it)
        }
        val marker = map.addMarker(markerOptions)
        marker?.let {
            it.tag = event.id
            markersMap[event.id] = it
        }
    }

    private fun removePoint(channelEvent: ChannelEvent.RemovePoint) {
        markersMap.remove(channelEvent.id)?.remove()
    }

    private companion object {
        const val DEBOUNCE_MILLIS: Int = 500
    }
}
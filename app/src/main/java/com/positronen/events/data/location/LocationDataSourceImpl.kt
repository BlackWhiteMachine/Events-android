package com.positronen.events.data.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class LocationDataSourceImpl @Inject constructor(
    @ApplicationContext
    private val context: Context
): LocationDataSource {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback: LocationCallback

    private val locationStateFlow: MutableStateFlow<Pair<Double, Double>?> = MutableStateFlow(null)

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.locations.firstOrNull()

                location?.let {
                    locationStateFlow.value = location.latitude to location.longitude
                }
            }
        }
    }

    override fun location(): Flow<Pair<Double, Double>> {
        val locationRequest = LocationRequest.create()
        locationRequest.fastestInterval = 5000
        locationRequest.interval = 10000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            location?.let {
                locationStateFlow.value = location.latitude to location.longitude
            }
        }

//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )

        return locationStateFlow.mapNotNull { it }
    }
}
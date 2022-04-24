package com.positronen.events.presentation.map

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.positronen.events.domain.model.LocationModel

class VisibleRegionWrapper(
    private val visibleRegion: VisibleRegion
) {

    fun contains(location: LocationModel): Boolean {
        val point = LatLng(location.latitude, location.longitude)
        return visibleRegion.latLngBounds.contains(point)
    }

    fun centerLatitude(): Double = visibleRegion.latLngBounds.center.latitude

    fun centerLongitude(): Double = visibleRegion.latLngBounds.center.longitude

    fun radius(): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            visibleRegion.latLngBounds.center.latitude,
            visibleRegion.latLngBounds.center.longitude,
            visibleRegion.farLeft.latitude,
            visibleRegion.farLeft.longitude,
            result
        )

        return result[0]/1000
    }
}
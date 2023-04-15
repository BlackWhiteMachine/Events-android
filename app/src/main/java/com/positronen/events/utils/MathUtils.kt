package com.positronen.events.utils

import android.location.Location

fun radius(
    firstLatitude: Double,
    firstLongitude: Double,
    secondLatitude: Double,
    secondLongitude: Double
): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        firstLatitude,
        firstLongitude,
        secondLatitude,
        secondLongitude,
        result
    )

    return result[0]/1000
}
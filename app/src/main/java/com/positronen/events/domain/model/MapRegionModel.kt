package com.positronen.events.domain.model

open class MapRegionModel(
    open val topLeftLatitude: Double,
    open val topLeftLongitude: Double,
    open val bottomRightLatitude: Double,
    open val bottomRightLongitude: Double
) {
    fun isContains(latitude: Double, longitude: Double): Boolean {
        return topLeftLatitude >= latitude && bottomRightLatitude < latitude &&
                topLeftLongitude <= longitude && bottomRightLongitude > longitude
    }
}
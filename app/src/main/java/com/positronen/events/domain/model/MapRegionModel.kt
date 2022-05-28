package com.positronen.events.domain.model

data class MapRegionModel(
    val topLeftLatitude: Double,
    val topLeftLongitude: Double,
    val bottomRightLatitude: Double,
    val bottomRightLongitude: Double
) {
    fun isContains(latitude: Double, longitude: Double): Boolean {
        return topLeftLatitude >= latitude && bottomRightLatitude < latitude &&
                topLeftLongitude <= longitude && bottomRightLongitude > longitude
    }
}
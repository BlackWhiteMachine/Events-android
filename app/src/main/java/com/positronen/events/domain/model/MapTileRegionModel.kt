package com.positronen.events.domain.model

data class MapTileRegionModel(
    val xTile: Int,
    val yTile: Int,
    override val topLeftLatitude: Double,
    override val topLeftLongitude: Double,
    override val bottomRightLatitude: Double,
    override val bottomRightLongitude: Double,
) : MapRegionModel(topLeftLatitude, topLeftLongitude, bottomRightLatitude, bottomRightLongitude) {

    fun getName(): String = "$xTile$yTile"
}

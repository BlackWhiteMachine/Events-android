package com.positronen.events.domain.model

data class MapTileRegionModel(
    val xTile: Int,
    val yTile: Int,
    val mapRegionModel: MapRegionModel
) {
    fun getName(): String = "$xTile$yTile"
}

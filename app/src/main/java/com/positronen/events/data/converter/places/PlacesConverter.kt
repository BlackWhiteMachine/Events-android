package com.positronen.events.data.converter.places

import com.positronen.events.data.converter.convertLocation
import com.positronen.events.data.converter.mapImages
import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType

fun convertResponsePlaces(places: List<PlaceV2Response>): List<PointModel> {
    return places.mapNotNull(::convertResponsePlace)
}

fun convertResponsePlaceDetail(place: PlaceV2Response): PointDetailModel? {
    val name = place.name.fi ?: return null
    val location = place.location ?: return null

    return PointDetailModel(
        id = place.id,
        pointType = PointType.PLACE,
        name = name,
        description = place.description?.body?.ifEmpty { place.description.intro },
        images = place.description?.images?.map(::mapImages) ?: emptyList(),
        location = convertLocation(location),
        infoUrl = place.infoUrl,
    )
}

private fun convertResponsePlace(place: PlaceV2Response): PointModel? {
    val name = place.name.fi ?: return null
    val location = place.location ?: return null

    return PointModel(
        id = place.id,
        pointType = PointType.PLACE,
        name = name,
        description = place.description?.intro,
        location = convertLocation(location)
    )
}
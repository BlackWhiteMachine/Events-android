package com.positronen.events.data.converter.events

import com.positronen.events.data.converter.convertLocation
import com.positronen.events.data.converter.mapImages
import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType

fun convertResponseEvents(events: List<EventsV1Response>): List<PointModel> {
    return events.mapNotNull(::convertResponseEvent)
}

fun convertResponseEventDetail(event: EventsV1Response): PointDetailModel? {
    val name = event.name.fi ?: return null
    val location = event.location ?: return null

    return PointDetailModel(
        id = event.id,
        pointType = PointType.EVENT,
        name = name,
        description = event.description?.body?.ifEmpty { event.description.intro },
        images = event.description?.images?.map(::mapImages) ?: emptyList(),
        location = convertLocation(location),
        infoUrl = event.infoUrl,
    )
}

private fun convertResponseEvent(place: EventsV1Response): PointModel? {
    val name = place.name.fi ?: return null
    val location = place.location ?: return null

    return PointModel(
        id = place.id,
        pointType = PointType.EVENT,
        name = name,
        description = place.description?.intro,
        location = convertLocation(location)
    )
}
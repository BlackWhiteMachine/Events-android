package com.positronen.events.data.converter

import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel

interface MainConverter {

    fun convertResponsePlaces(places: List<PlaceV2Response>): List<PointModel>

    fun convertResponsePlaceDetail(place: PlaceV2Response): PointDetailModel?

    fun convertResponseEvents(events: List<EventsV1Response>): List<PointModel>

    fun convertResponseEventDetail(event: EventsV1Response): PointDetailModel?
}
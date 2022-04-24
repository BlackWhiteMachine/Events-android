package com.positronen.events.data.repository

import com.positronen.events.data.converter.MainConverter
import com.positronen.events.data.converter.MainConverterImpl
import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.MainRepository
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val service: MainService
) : MainRepository {

    private val mainConverter: MainConverter = MainConverterImpl()

    override fun places(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>> = flow {
        val distanceFilter = DISTANCE_FILTER.format(latitude, longitude, distance)
        val resultList = mutableListOf<PlaceV2Response>()
        var hasNext = true
        var start = 0

        while (hasNext) {
            val response = service.places(distanceFilter, start, PAGE_SIZE).data
            resultList.addAll(response)
            start += PAGE_SIZE
            hasNext = response.size == PAGE_SIZE
        }

        emit(resultList)
    }.map(mainConverter::convertResponsePlaces)

    override fun place(id: String): Flow<PointDetailModel> = flow {
        emit(service.place(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    override fun events(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>> = flow {
        val distanceFilter = DISTANCE_FILTER.format(latitude, longitude, distance)
        val resultList = mutableListOf<EventsV1Response>()
        var hasNext = true
        var start = 0
        while (hasNext) {
            val response = service.events(distanceFilter, start, PAGE_SIZE).data
            resultList.addAll(response)
            start += PAGE_SIZE
            hasNext = response.size == PAGE_SIZE
        }

        emit(resultList)
    }.map(mainConverter::convertResponseEvents)

    override fun event(id: String): Flow<PointDetailModel> = flow {
        emit(service.event(id))
    }.mapNotNull(mainConverter::convertResponseEventDetail)

    override fun activities(start: Int, limit: Int): Flow<List<PointModel>> = flow {
        emit(service.activities(start, limit).data)
    }.map(mainConverter::convertResponsePlaces)

    override fun activity(id: String): Flow<PointDetailModel> = flow {
        emit(service.activity(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    private companion object {
        const val DISTANCE_FILTER: String = "%.7f,%.7f,%.4f"
        const val PAGE_SIZE: Int = 20
    }
}
package com.positronen.events.data.repository.activities

import com.positronen.events.data.converter.MainConverter
import com.positronen.events.data.converter.MainConverterImpl
import com.positronen.events.data.service.MainService
import com.positronen.events.domain.ActivitiesRepository
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class ActivitiesRepositoryImpl @Inject constructor(
    private val service: MainService
) : ActivitiesRepository {

    private val mainConverter: MainConverter = MainConverterImpl()

    override fun activities(start: Int, limit: Int): Flow<List<PointModel>> = flow {
        emit(service.activities(start, limit).data)
    }.map(mainConverter::convertResponsePlaces)

    override fun activity(id: String): Flow<PointDetailModel> = flow {
        emit(service.activity(id))
    }.mapNotNull(mainConverter::convertResponsePlaceDetail)

    private companion object {
        const val DISTANCE_FILTER: String = "%.7f,%.7f,%.4f"
        const val PAGE_SIZE: Int = 50
    }
}
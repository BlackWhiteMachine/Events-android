package com.positronen.events.domain

import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    fun places(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>>

    fun place(id: String): Flow<PointDetailModel>

    fun events(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>>

    fun event(id: String): Flow<PointDetailModel>

    fun activities(start: Int, limit: Int): Flow<List<PointModel>>

    fun activity(id: String): Flow<PointDetailModel>
}
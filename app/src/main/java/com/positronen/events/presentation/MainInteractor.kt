package com.positronen.events.presentation

import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import kotlinx.coroutines.flow.Flow

interface MainInteractor {

    fun places(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>>

    fun events(latitude: Double, longitude: Double, distance: Float): Flow<List<PointModel>>

    fun point(id: String, pointType: PointType):  Flow<PointDetailModel>
}
package com.positronen.events.domain

import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import kotlinx.coroutines.flow.Flow

interface ActivitiesRepository {

    fun activities(start: Int, limit: Int): Flow<List<PointModel>>

    fun activity(id: String): Flow<PointDetailModel>
}
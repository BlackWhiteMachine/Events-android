package com.positronen.events.domain.interactor

import com.positronen.events.domain.MainRepository
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.presentation.MainInteractor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MainInteractorImpl @Inject constructor(
    private val mainRepository: MainRepository
) : MainInteractor {

    override fun places(
        latitude: Double,
        longitude: Double,
        distance: Float
    ): Flow<List<PointModel>> = mainRepository.places(latitude, longitude, distance)

    override fun events(
        latitude: Double,
        longitude: Double,
        distance: Float
    ): Flow<List<PointModel>> = mainRepository.events(latitude, longitude, distance)

    override fun point(id: String, pointType: PointType): Flow<PointDetailModel> =
        when (pointType) {
            PointType.PLACE -> mainRepository.place(id)
            PointType.EVENT -> mainRepository.event(id)
            PointType.ACTIVITY -> TODO()
        }
}
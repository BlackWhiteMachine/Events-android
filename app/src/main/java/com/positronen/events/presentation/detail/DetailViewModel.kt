package com.positronen.events.presentation.detail

import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.detail.ChannelEventDetail
import com.positronen.events.domain.interactor.MainInteractor
import com.positronen.events.presentation.base.BaseViewModel
import com.positronen.events.utils.Logger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class DetailViewModel @Inject constructor(
    private val interactor: MainInteractor
) : BaseViewModel() {

    private val dataMutableStateFlow = MutableStateFlow<PointDetailModel?>(null)
    val dataFlow: Flow<PointDetailModel>
        get() = dataMutableStateFlow.mapNotNull { it }

    val showProgressBarFlow: Flow<Boolean>
        get() = dataMutableStateFlow.map { it == null }

    private val eventSharedFlow = MutableSharedFlow<ChannelEventDetail>()
    val eventFlow: Flow<ChannelEventDetail>
        get() = eventSharedFlow

    fun onViewInit(id: String, pointType: PointType) {
        baseCoroutineScope.launch {
            interactor.point(id, pointType)
                .catch { error ->
                    Logger.exception(Exception(error.message))
                }
                .collect { place ->
                    dataMutableStateFlow.value = place
                }
        }
    }

    fun onShareAddressClicked() {
        val address: String = dataMutableStateFlow.value?.location?.address ?: return

        baseCoroutineScope.launch {
            eventSharedFlow.emit(ChannelEventDetail.ShareText(address))
        }
    }
}
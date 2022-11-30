package com.positronen.events.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointType
import com.positronen.events.domain.model.detail.ChannelEventDetail
import com.positronen.events.domain.interactor.MainInteractor
import com.positronen.events.utils.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class DetailViewModel @Inject constructor(
    private val interactor: MainInteractor
) : ViewModel() {

    private val dataMutableStateFlow = MutableStateFlow<PointDetailModel?>(null)
    val dataFlow: Flow<PointDetailModel>
        get() = dataMutableStateFlow.mapNotNull { it }

    val showProgressBarFlow: Flow<Boolean>
        get() = dataMutableStateFlow.map { it == null }

    private val eventChannel = Channel<ChannelEventDetail>()
    val eventFlow: Flow<ChannelEventDetail>
        get() = eventChannel.receiveAsFlow()

    fun onViewInit(id: String, pointType: PointType) {
        viewModelScope.launch {
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

        viewModelScope.launch {
            eventChannel.send(ChannelEventDetail.ShareText(address))
        }
    }
}
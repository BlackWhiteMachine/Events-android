package com.positronen.events.presentation.mvi

import com.positronen.events.presentation.base.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseMVIViewModel<SM: BaseState, EM: BaseEvent, IM : BaseIntent>(
    initialState: SM
): BaseViewModel() {

    private val _mutableStateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<SM>
        get() = _mutableStateFlow

    private val _mutableSharedFlow = MutableSharedFlow<EM>()
    val sharedFlow: SharedFlow<EM>
        get() = _mutableSharedFlow

    abstract fun handleIntent(intent: IM)

    protected fun updateState(action: SM.() -> SM) {
        _mutableStateFlow.value = _mutableStateFlow.value.action()
    }

    protected fun sendEvent(event: EM) {
        baseCoroutineScope.launch {
            _mutableSharedFlow.emit(event)
        }
    }

    fun sendIntent(intent: IM) {
        handleIntent(intent)
    }
}
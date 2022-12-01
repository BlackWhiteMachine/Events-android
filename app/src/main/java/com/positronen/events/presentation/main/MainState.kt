package com.positronen.events.presentation.main

import com.positronen.events.presentation.mvi.BaseState

sealed class MainState: BaseState() {

    object Init : MainState()
    data class Loading(val isShowing: Boolean) : MainState()
}
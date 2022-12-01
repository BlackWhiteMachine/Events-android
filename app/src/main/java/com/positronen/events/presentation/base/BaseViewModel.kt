package com.positronen.events.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

abstract class BaseViewModel : ViewModel() {

    protected val baseCoroutineScope: CoroutineScope
        get() = viewModelScope
}
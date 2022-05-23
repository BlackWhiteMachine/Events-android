package com.positronen.events.domain.model

sealed class DataResource<out T: Any> {
    object Init: DataResource<Nothing>()
    data class Success<out T: Any>(val data: T): DataResource<T>()
    data class Error(val exception: Throwable): DataResource<Nothing>()
    object Loading: DataResource<Nothing>()
}
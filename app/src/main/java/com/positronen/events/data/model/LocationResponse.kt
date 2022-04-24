package com.positronen.events.data.model

data class LocationResponse(
    val lat: Double,
    val lon: Double,
    val address: AddressResponse
)

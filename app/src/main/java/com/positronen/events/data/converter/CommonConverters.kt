package com.positronen.events.data.converter

import com.positronen.events.data.model.ImageResponse
import com.positronen.events.data.model.LocationResponse
import com.positronen.events.domain.model.ImageModel
import com.positronen.events.domain.model.LocationModel

fun mapImages(imageResponse: ImageResponse): ImageModel =
    ImageModel(
        url = imageResponse.url,
        copyrightHolder = imageResponse.copyrightHolder,
        license = imageResponse.licenseType.name
    )

fun convertLocation(location: LocationResponse): LocationModel {
    val address = location.address

    val constructedAddress = "${address.postalCode}, " +
            "${address.locality}, " +
            "${address.neighbourhood}, " +
            "${address.streetAddress}, " +
            String.format("%.7f", location.lat) +
            "," +
            String.format("%.7f", location.lon)

    return LocationModel(
        latitude = location.lat,
        longitude = location.lon,
        address = constructedAddress
    )
}
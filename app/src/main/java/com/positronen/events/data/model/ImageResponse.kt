package com.positronen.events.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageResponse(
    val url: String,
    @SerialName(value = "copyright_holder")
    val copyrightHolder: String,
    @SerialName(value = "license_type")
    val licenseType: LicenseTypeResponse,
    @SerialName(value = "media_id")
    val mediaId: String
)
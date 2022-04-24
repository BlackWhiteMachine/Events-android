package com.positronen.events.data.model

import com.google.gson.annotations.SerializedName

data class ImageResponse(
    val url: String,
    @SerializedName(value = "copyright_holder")
    val copyrightHolder: String,
    @SerializedName(value = "license_type")
    val licenseType: LicenseTypeResponse,
    @SerializedName(value = "media_id")
    val mediaId: String
)
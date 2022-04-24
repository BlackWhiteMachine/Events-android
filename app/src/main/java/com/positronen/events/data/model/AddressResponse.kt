package com.positronen.events.data.model

import com.google.gson.annotations.SerializedName

data class AddressResponse(
    @SerializedName(value = "street_address")
    val streetAddress: String? = null,
    @SerializedName(value = "postal_code")
    val postalCode: String? = null,
    val locality: String? = null,
    val neighbourhood: String? = null
)

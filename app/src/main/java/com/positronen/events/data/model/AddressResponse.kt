package com.positronen.events.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressResponse(
    @SerialName(value = "street_address")
    val streetAddress: String? = null,
    @SerialName(value = "postal_code")
    val postalCode: String? = null,
    val locality: String? = null,
    val neighbourhood: String? = null
)

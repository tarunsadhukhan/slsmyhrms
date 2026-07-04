package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AddOccupationRequest(
    @SerializedName("name")
    val name: String
)

data class AddOccupationResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("id")
    val id: Int?,

    @SerializedName("message")
    val message: String?
)


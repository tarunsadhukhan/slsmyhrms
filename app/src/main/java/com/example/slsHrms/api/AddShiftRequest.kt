package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AddShiftRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("start_time")
    val startTime: String,

    @SerializedName("end_time")
    val endTime: String
)

data class AddShiftResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("id")
    val id: Int?,

    @SerializedName("message")
    val message: String?
)


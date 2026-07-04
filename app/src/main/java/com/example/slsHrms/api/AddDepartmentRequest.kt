package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AddDepartmentRequest(
    @SerializedName("name")
    val name: String
)

data class AddDepartmentResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("id")
    val id: Int?,

    @SerializedName("message")
    val message: String?
)


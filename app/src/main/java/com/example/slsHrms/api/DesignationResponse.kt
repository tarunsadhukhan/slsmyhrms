package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Designation(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
) {
    override fun toString(): String = name
}

data class DesignationResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val designations: List<Designation>?,

    @SerializedName("total")
    val total: Int?
)


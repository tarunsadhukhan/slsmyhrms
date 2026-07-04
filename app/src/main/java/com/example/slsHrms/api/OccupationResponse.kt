package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Occupation(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
) {
    override fun toString(): String = name
}

data class OccupationResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val occupations: List<Occupation>?,

    @SerializedName("total")
    val total: Int?
)


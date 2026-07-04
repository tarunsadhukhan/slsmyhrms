package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Department(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
) {
    override fun toString(): String = name
}

data class DepartmentResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val departments: List<Department>?,

    @SerializedName("total")
    val total: Int?
)


package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AddEmployeeRequest(
    @SerializedName("emp_code")
    val empCode: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("department_id")
    val departmentId: Int?,

    @SerializedName("designation_id")
    val designationId: Int?,

    @SerializedName("shift_id")
    val shiftId: Int?,

    @SerializedName("image")
    val faceImage: String?   // base64 encoded image
)

data class AddEmployeeResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("id")
    val id: Int?,

    @SerializedName("message")
    val message: String?
)

package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class OnBoardingEmployeeResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("eb_id") val ebId: Int?,
    @SerializedName("emp_code") val empCode: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("department_name") val departmentName: String?,
    @SerializedName("designation_name") val designationName: String?,
    @SerializedName("branch_id") val branchId: Int?,
    @SerializedName("face_count") val faceCount: Int?,
    @SerializedName("can_register") val canRegister: Boolean?,
    @SerializedName("message") val message: String?
)

data class OnBoardingRegisterRequest(
    @SerializedName("emp_code") val empCode: String,
    @SerializedName("face_image") val faceImage: String,
    @SerializedName("branch_id") val branchId: Int? = null
)

data class OnBoardingRegisterResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("face_count") val faceCount: Int?,
    @SerializedName("can_register") val canRegister: Boolean?
)


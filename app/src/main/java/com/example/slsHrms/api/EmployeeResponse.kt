package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("id")
    val id: Int,

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

    @SerializedName("department_name")
    val departmentName: String?,

    @SerializedName("designation_name")
    val designationName: String?,

    @SerializedName("shift_name")
    val shiftName: String?,

    @SerializedName("photo_base64")
    val photoBase64: String?,

    @SerializedName("is_active")
    val isActive: Int?
)

data class EmployeeResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val employees: List<Employee>?,

    @SerializedName("total")
    val total: Int?
)


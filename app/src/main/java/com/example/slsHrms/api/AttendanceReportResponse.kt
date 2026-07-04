package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AttendanceRecord(
    @SerializedName("id")
    val id: Int,

    @SerializedName("emp_code")
    val empCode: String,

    @SerializedName("eb_id")
    val ebId: Int?,

    @SerializedName("emp_name")
    val empName: String,

    @SerializedName("department_name")
    val departmentName: String?,

    @SerializedName("designation_name")
    val designationName: String?,

    @SerializedName("shift_name")
    val shiftName: String?,

    @SerializedName("attendance_date")
    val attendanceDate: String,

    @SerializedName("attendance_time")
    val attendanceTime: String,

    @SerializedName("exit_time")
    val exitTime: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("att_type")
    val attType: String?,

    @SerializedName("shift_hours")
    val shiftHours: Double?,

    @SerializedName("working_hours")
    val workingHours: Double?,

    @SerializedName("idle_hours")
    val idleHours: Double?,

    @SerializedName("photo_att")
    val photoAtt: String?,

    @SerializedName("machine_nos")
    val machineNos: String?
)

data class AttendanceReportResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val data: List<AttendanceRecord>?,

    @SerializedName("total")
    val total: Int?,

    @SerializedName("message")
    val message: String?
)


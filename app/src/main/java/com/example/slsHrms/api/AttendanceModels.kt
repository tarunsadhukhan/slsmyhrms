package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// AttendanceRecord already exists in AttendanceReportResponse.kt
// AttendanceListResponse = AttendanceReportResponse

// /attendance_leave_status — is the employee on leave (vw_leave_dates) on a date?
data class LeaveStatusResponse(
    @SerializedName("data") val data: LeaveStatus?
)

data class LeaveStatus(
    @SerializedName("on_leave") val onLeave: Boolean?,
    @SerializedName("payable")  val payable: String?   // "Y" = paid leave
)

data class AttendanceDetailRecord(
    @SerializedName("id")
    val id: Int,

    @SerializedName("eb_id")
    val ebId: Int?,

    @SerializedName("emp_code")
    val empCode: String,

    @SerializedName("emp_name")
    val empName: String,

    @SerializedName("attendance_date")
    val attendanceDate: String,

    @SerializedName("att_type")
    val attType: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("department_id")
    val departmentId: Int?,

    @SerializedName("designation_id")
    val designationId: Int?,

    @SerializedName("shift_id")
    val shiftId: Int?,

    @SerializedName("shift_name")
    val shiftName: String?,

    @SerializedName("shift_hours")
    val shiftHours: Double?,

    @SerializedName("working_hours")
    val workingHours: Double?,

    @SerializedName("idle_hours")
    val idleHours: Double?,

    @SerializedName("branch_id")
    val branchId: Int?,

    @SerializedName("photo_html")
    val photoHtml: String?
)

data class AttendanceDetailResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val data: AttendanceDetailRecord?
)

data class UpdateAttendanceRequest(
    @SerializedName("emp_code")
    val empCode: String,

    @SerializedName("attendance_date")
    val attendanceDate: String,

    @SerializedName("att_type")
    val attType: String,

    @SerializedName("department_id")
    val departmentId: Int?,

    @SerializedName("shift_id")
    val shiftId: Int?,

    @SerializedName("designation_id")
    val designationId: Int?,

    @SerializedName("shift_hours")
    val shiftHours: Double?,

    @SerializedName("working_hours")
    val workingHours: Double?,

    @SerializedName("idle_hours")
    val idleHours: Double?,

    @SerializedName("machine_ids")
    val machineIds: List<Int>? = null
)

data class UpdateAttendanceResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("attendance_id")
    val attendanceId: Int?
)


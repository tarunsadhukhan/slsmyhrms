package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class FaceRecognitionRequest(
    @SerializedName("image")
    val image: String,

    @SerializedName("att_type")
    val attType: String? = null, // R=Regular, O=OT, C=Cash (optional, used by /attendance)

    @SerializedName("department_id")
    val departmentId: Int? = null,

    @SerializedName("shift_id")
    val shiftId: Int? = null,

    @SerializedName("designation_id")
    val designationId: Int? = null,

    @SerializedName("attendance_date")
    val attendanceDate: String? = null,

    @SerializedName("shift_hours")
    val shiftHours: Double? = null,

    @SerializedName("working_hours")
    val workingHours: Double? = null,

    @SerializedName("idle_hours")
    val idleHours: Double? = null,

    @SerializedName("machine_ids")
    val machineIds: List<Int>? = null,

    @SerializedName("branch_id")
    val branchId: Int? = null,

    // Device geo-location captured at submit time, "latitude,longitude".
    // Stored in daily_attendance.get_location. Empty string when no fix.
    @SerializedName("get_location")
    val getLocation: String? = null
)

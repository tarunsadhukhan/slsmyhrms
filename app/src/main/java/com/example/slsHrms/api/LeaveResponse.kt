package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Leave Types ──────────────────────────────────────────────────────────────

data class LeaveType(
    @SerializedName("id")              val id: Int?,
    @SerializedName("leave_type_name") val name: String
) {
    override fun toString() = name
}

data class LeaveTypeResponse(
    @SerializedName("status")      val status: String?,
    @SerializedName("leave_types") val leaveTypes: List<LeaveType>?
)

// ── Status Master ─────────────────────────────────────────────────────────────

data class StatusMst(
    @SerializedName("status_id")   val statusId: Int,
    @SerializedName("status_name") val statusName: String
) {
    override fun toString() = statusName
}

data class StatusMstResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("statuses") val statuses: List<StatusMst>?
)

// ── List response ─────────────────────────────────────────────────────────────

data class LeaveListResponse(
    @SerializedName("status")       val status: String?,
    @SerializedName("message")      val message: String?,
    @SerializedName("transactions") val transactions: List<LeaveTransaction>?
)

data class LeaveSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Long?   // leave ids are bigint (6,065,723+)
)

// ── Main transaction header ───────────────────────────────────────────────────

data class LeaveTransaction(
    @SerializedName("id")               val id: Long?,
    @SerializedName("eb_id")            val ebId: Int?,
    @SerializedName("emp_code")         val empCode: String?,
    @SerializedName("emp_name")         val empName: String?,
    @SerializedName("leave_type_id")    val leaveTypeId: Int?,
    @SerializedName("leave_type")       val leaveType: String?,
    @SerializedName("from_date")        val fromDate: String?,
    @SerializedName("to_date")          val toDate: String?,
    @SerializedName("non_working_days") val nonWorkingDays: Int?,
    @SerializedName("remarks")          val remarks: String?,
    @SerializedName("status_id")        val statusId: Int?,
    @SerializedName("status")           val status: String?
)

// ── Save request (server saves it as status 3) ────────────────────────────────

data class LeaveSaveRequest(
    @SerializedName("eb_id")            val ebId: Int,
    @SerializedName("user_id")          val userId: Int,
    @SerializedName("leave_type_id")    val leaveTypeId: Int,
    @SerializedName("from_date")        val fromDate: String,
    @SerializedName("to_date")          val toDate: String,
    @SerializedName("non_working_days") val nonWorkingDays: Int,
    @SerializedName("remarks")          val remarks: String,
    @SerializedName("branch_id")        val branchId: Int
)

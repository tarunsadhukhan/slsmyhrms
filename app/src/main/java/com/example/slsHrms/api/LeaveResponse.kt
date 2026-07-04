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
    @SerializedName("id")      val id: Int?
)

// ── Main transaction header ───────────────────────────────────────────────────

data class LeaveTransaction(
    @SerializedName("id")            val id: Int?,
    @SerializedName("eb_id")         val ebId: Int?,
    @SerializedName("emp_code")      val empCode: String?,
    @SerializedName("emp_name")      val empName: String?,
    @SerializedName("leave_type_id") val leaveTypeId: Int?,
    @SerializedName("leave_type")    val leaveType: String?,
    @SerializedName("from_date")     val fromDate: String?,
    @SerializedName("to_date")       val toDate: String?,
    @SerializedName("no_of_days")    val noOfDays: Double?,
    @SerializedName("reason")        val reason: String?,
    @SerializedName("remarks")       val remarks: String?,
    @SerializedName("status_id")     val statusId: Int?,
    @SerializedName("status")        val status: String?,
    @SerializedName("branch_id")     val branchId: Int?,
    @SerializedName("updated_by")    val updatedBy: Int?,
    @SerializedName("created_at")    val createdAt: String?,
    @SerializedName("details")       val details: List<LeaveTranDetail>?
)

// ── Detail rows ───────────────────────────────────────────────────────────────

data class LeaveTranDetail(
    @SerializedName("id")         val id: Int?,
    @SerializedName("tran_id")    val tranId: Int?,
    @SerializedName("leave_date") val leaveDate: String?
)

// ── Save request ──────────────────────────────────────────────────────────────

data class LeaveSaveRequest(
    @SerializedName("eb_id")          val ebId: Int,
    @SerializedName("user_id")        val userId: Int,
    @SerializedName("leave_type_id")  val leaveTypeId: Int,
    @SerializedName("from_date")      val fromDate: String,
    @SerializedName("to_date")        val toDate: String,
    @SerializedName("no_of_days")     val noOfDays: Double,
    @SerializedName("reason")         val reason: String,
    @SerializedName("remarks")        val remarks: String,
    @SerializedName("status_id")      val statusId: Int,
    @SerializedName("branch_id")      val branchId: Int?,
    @SerializedName("details")        val details: List<LeaveTranDetailRequest>
)

data class LeaveTranDetailRequest(
    @SerializedName("leave_date") val leaveDate: String
)

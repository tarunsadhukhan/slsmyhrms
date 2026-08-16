package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class EmpWiseAttendanceResponse(
    @SerializedName("status")          val status: String?,
    @SerializedName("report_type")     val reportType: String?,
    @SerializedName("from_date")       val fromDate: String?,
    @SerializedName("to_date")         val toDate: String?,
    @SerializedName("columns")         val columns: List<String>?,
    @SerializedName("total_employees") val totalEmployees: Int,
    @SerializedName("employees")       val employees: List<EmpAttRow>?
)

/**
 * One muster line: a distinct (employee, department, shift, designation,
 * attendance type). An employee spanning more than one context gets several
 * rows followed by an [isSubtotal] row summing them.
 *
 * [attendance] values are DAY COUNTS keyed by period label — 1 per day worked
 * (or on leave), "" for nothing. Not hours.
 */
data class EmpAttRow(
    @SerializedName("emp_code")    val empCode: String,
    @SerializedName("emp_name")    val empName: String,
    @SerializedName("dept")        val dept: String?,
    @SerializedName("shift")       val shift: String?,
    @SerializedName("designation") val designation: String?,
    @SerializedName("att_type")    val attType: String?,
    @SerializedName("attendance")  val attendance: Map<String, Any>?,
    @SerializedName("is_subtotal") val isSubtotal: Boolean = false,
    @SerializedName("tot_r")       val totR: Int = 0,
    @SerializedName("tot_o")       val totO: Int = 0,
    @SerializedName("tot_c")       val totC: Int = 0,
    @SerializedName("tot_l")       val totL: Int = 0,
    @SerializedName("tot_h")       val totH: Int = 0,
    @SerializedName("tot_all")     val totAll: Int = 0
)

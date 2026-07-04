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

data class EmpAttRow(
    @SerializedName("emp_code")      val empCode: String,
    @SerializedName("emp_name")      val empName: String,
    @SerializedName("dept")          val dept: String?,
    @SerializedName("designation")   val designation: String?,
    @SerializedName("attendance")    val attendance: Map<String, Any>?,  // value: hours (Double) or "" empty
    @SerializedName("total_hours")   val totalHours: Double,
    @SerializedName("total_present") val totalPresent: Int,
    @SerializedName("total_absent")  val totalAbsent: Int
)




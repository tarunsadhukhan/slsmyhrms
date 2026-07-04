package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class AttendanceDashboardResponse(
    @SerializedName("status")              val status: String?,
    @SerializedName("date")                val date: String?,
    @SerializedName("branch_id")           val branchId: Int?,
    @SerializedName("co_id")               val coId: Int?,
    @SerializedName("today_attendance")    val todayAttendance: TodayAttendance?,
    @SerializedName("wages_last_7_days")   val wagesLast7Days: List<WagesDay>?,
    @SerializedName("last_7_days_present") val last7DaysPresent: List<DayPresent>?,
    @SerializedName("absent_buckets")      val absentBuckets: AbsentBuckets?,
    @SerializedName("man_machine_last_7_days") val manMachineLast7Days: List<ManMachineDay>?,
    @SerializedName("message")             val message: String?
)

data class ManMachineDay(
    @SerializedName("date")         val date: String?,
    @SerializedName("label")        val label: String?,
    @SerializedName("total_hands")  val totalHands: Float = 0f,
    @SerializedName("total_target") val totalTarget: Float = 0f
)

data class TodayAttendance(
    @SerializedName("present")         val present: Int = 0,
    @SerializedName("absent")          val absent: Int = 0,
    @SerializedName("leave")           val leave: Int = 0,
    @SerializedName("total_employees") val totalEmployees: Int = 0
)

data class WagesDay(
    @SerializedName("date")        val date: String?,
    @SerializedName("label")       val label: String?,
    @SerializedName("total_hours") val totalHours: Float = 0f,
    @SerializedName("amount")      val amount: Float = 0f
)

data class DayPresent(
    @SerializedName("date")    val date: String?,
    @SerializedName("label")   val label: String?,
    @SerializedName("present") val present: Int = 0
)

data class AbsentBuckets(
    @SerializedName("range_1_to_7")   val range1to7: Int = 0,
    @SerializedName("range_8_to_15")  val range8to15: Int = 0,
    @SerializedName("range_16_to_30") val range16to30: Int = 0,
    @SerializedName("over_30_days")   val over30Days: Int = 0
)


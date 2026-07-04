package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class DashboardStatsResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("date")
    val date: String?,

    @SerializedName("total_departments")
    val totalDepartments: Int,

    @SerializedName("total_designations")
    val totalDesignations: Int,

    @SerializedName("total_shifts")
    val totalShifts: Int,

    @SerializedName("total_employees")
    val totalEmployees: Int,

    @SerializedName("total_present")
    val totalPresent: Int,

    @SerializedName("present_face")
    val presentFace: Int,

    @SerializedName("present_manual")
    val presentManual: Int,

    @SerializedName("total_absent")
    val totalAbsent: Int,

    @SerializedName("department_wise")
    val departmentWise: List<DeptWiseStat>?,

    @SerializedName("department_present")
    val departmentPresent: List<DeptWiseStat>?,

    @SerializedName("department_master")
    val departmentMaster: List<DeptWiseStat>?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("jute_recv")     val juteRecv: Int = 0,
    @SerializedName("jute_issue")    val juteIssue: Int = 0,
    @SerializedName("jute_stock")    val juteStock: Int = 0,

    @SerializedName("spg_prod")      val spgProd: Int = 0,
    @SerializedName("spg_eff")       val spgEff: Int = 0,
    @SerializedName("spg_run_eff")   val spgRunEff: Int = 0,
    @SerializedName("spg_prd_frame") val spgPrdFrame: Int = 0,

    @SerializedName("wdg_prod")      val wdgProd: Int = 0,
    @SerializedName("wdg_winders")   val wdgWinders: Int = 0,
    @SerializedName("wdg_avg_prod")  val wdgAvgProd: Int = 0,

    @SerializedName("oth_weaving")   val othWeaving: Int = 0,
    @SerializedName("oth_hemming")   val othHemming: Int = 0,
    @SerializedName("oth_heracle")   val othHeracle: Int = 0,
    @SerializedName("oth_hsewer")    val othHsewer: Int = 0,

    @SerializedName("bales_prod")    val balesProd: Int = 0,
    @SerializedName("bales_issue")   val balesIssue: Int = 0,
    @SerializedName("bales_stock")   val balesStock: Int = 0
)

data class DeptWiseStat(
    @SerializedName("department_id")
    val departmentId: Int,

    @SerializedName("department_name")
    val departmentName: String,

    @SerializedName("total_employees")
    val totalEmployees: Int,

    @SerializedName("present")
    val present: Int,

    @SerializedName("absent")
    val absent: Int
)


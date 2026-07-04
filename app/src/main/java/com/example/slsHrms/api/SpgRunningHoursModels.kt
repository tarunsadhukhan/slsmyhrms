package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class SpgRunningEntry(
    @SerializedName("id")                val id: Int?,
    @SerializedName("tran_date")         val tranDate: String?,
    @SerializedName("spell_id")          val spellId: Int?,
    @SerializedName("branch_id")         val branchId: Int?,
    @SerializedName("mc_id")             val mcId: Int?,
    @SerializedName("mc_name")           val mcName: String?,
    @SerializedName("mc_runs_time")      val mcRunsTime: Double?,
    @SerializedName("kw_consumption")    val kwConsumption: Double?,
    @SerializedName("updated_by")        val updatedBy: Int?,
    @SerializedName("updated_date_time") val updatedDateTime: String?
)

data class SpgRunningListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<SpgRunningEntry>?,
    @SerializedName("total")   val total: Int?
)

data class SpgRunningSaveRequest(
    @SerializedName("date")           val date: String,
    @SerializedName("spell_id")       val spellId: Int,
    @SerializedName("branch_id")      val branchId: Int,
    @SerializedName("mc_id")          val mcId: Int,
    @SerializedName("mc_runs_time")   val mcRunsTime: Double,
    @SerializedName("kw_consumption") val kwConsumption: Double?,
    @SerializedName("user_id")        val userId: Int
)

data class SpgRunningUpdateRequest(
    @SerializedName("mc_id")          val mcId: Int?,
    @SerializedName("branch_id")      val branchId: Int?,
    @SerializedName("mc_runs_time")   val mcRunsTime: Double?,
    @SerializedName("kw_consumption") val kwConsumption: Double?,
    @SerializedName("user_id")        val userId: Int?
)

data class SpgRunningSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

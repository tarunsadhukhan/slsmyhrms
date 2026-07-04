package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class WeightTransaction(
    @SerializedName("id")           val id: Int?,
    @SerializedName("tran_date")    val tranDate: String?,
    @SerializedName("branch_id")    val branchId: Int?,
    @SerializedName("spell_id")     val spellId: Int?,
    @SerializedName("spell_name")   val spellName: String?,
    @SerializedName("gross_weight") val grossWeight: Double?,
    @SerializedName("tare_weight")  val tareWeight: Double?,
    @SerializedName("net_weight")   val netWeight: Double?,
    @SerializedName("weight_type")  val weightType: String?,
    @SerializedName("created_at")   val createdAt: String?
)

data class WeightListResponse(
    @SerializedName("status")       val status: String?,
    @SerializedName("message")      val message: String?,
    @SerializedName("transactions") val transactions: List<WeightTransaction>?
)

data class WeightSaveRequest(
    @SerializedName("tran_date")    val tranDate: String,
    @SerializedName("branch_id")    val branchId: Int,
    @SerializedName("spell_id")     val spellId: Int?,
    @SerializedName("gross_weight") val grossWeight: Double,
    @SerializedName("tare_weight")  val tareWeight: Double,
    @SerializedName("net_weight")   val netWeight: Double,
    @SerializedName("user_id")      val userId: Int
)

data class WeightSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class McCode(
    @SerializedName("mc_code_id")          val mcCodeId: Int?,
    @SerializedName("mc_code")             val mcCode: String?,
    @SerializedName("mechine_type_name")   val mechineTypeName: String?,
    @SerializedName("dept_id")             val deptId: Int?,
    @SerializedName("machine_type")        val machineType: Int?
) {
    fun label(): String = (mechineTypeName ?: mcCode ?: "MC$mcCodeId").trim()
}

data class McCodeResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("message")  val message: String?,
    @SerializedName("machines") val machines: List<McCode>?
)

data class RollStockEntry(
    @SerializedName("id")                val id: Int?,
    @SerializedName("stock_date")        val stockDate: String?,
    @SerializedName("spell_id")          val spellId: Int?,
    @SerializedName("mc_code_id")        val mcCodeId: Int?,
    @SerializedName("mc_code")           val mcCode: String?,
    @SerializedName("mc_name")           val mcName: String?,
    @SerializedName("sprd_quality_id")   val sprdQualityId: Int?,
    @SerializedName("quality")           val quality: String?,
    @SerializedName("no_of_rolls")       val noOfRolls: Int?,
    @SerializedName("updated_by")        val updatedBy: Int?,
    @SerializedName("updated_date_time") val updatedDateTime: String?
)

data class RollStockListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<RollStockEntry>?
)

data class RollStockSaveRequest(
    @SerializedName("date")            val date: String,
    @SerializedName("spell_id")        val spellId: Int,
    @SerializedName("mc_code_id")      val mcCodeId: Int,
    @SerializedName("sprd_quality_id") val sprdQualityId: Int,
    @SerializedName("no_of_rolls")     val noOfRolls: Int,
    @SerializedName("user_id")         val userId: Int
)

data class RollStockUpdateRequest(
    @SerializedName("mc_code_id")      val mcCodeId: Int?,
    @SerializedName("sprd_quality_id") val sprdQualityId: Int?,
    @SerializedName("no_of_rolls")     val noOfRolls: Int?,
    @SerializedName("user_id")         val userId: Int?
)

data class RollStockSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)
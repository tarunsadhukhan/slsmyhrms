package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Sheds ────────────────────────────────────────────────────────────────────

data class AssortingShedsResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("sheds")   val sheds: List<String>?
)

// ── Selector ─────────────────────────────────────────────────────────────────

data class Selector(
    @SerializedName("selector_id")       val selectorId: Int,
    @SerializedName("selector_shr_name") val selectorShrName: String?,
    @SerializedName("selector_name")     val selectorName: String?
) {
    fun label(): String =
        selectorShrName?.takeIf { it.isNotBlank() } ?: selectorName ?: "S$selectorId"
}

data class SelectorResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("selectors") val selectors: List<Selector>?
)

// ── Trolly Validation (assorting / type='S') ─────────────────────────────────

data class AssortingTrollyValidateResponse(
    @SerializedName("status")        val status: String?,
    @SerializedName("message")       val message: String?,
    @SerializedName("trolly_id")     val trollyId: Int?,
    @SerializedName("trolly_no")     val trollyNo: String?,
    @SerializedName("trolly_name")   val trollyName: String?,
    @SerializedName("trolly_weight") val trollyWeight: Double?
)

// ── Save / List ──────────────────────────────────────────────────────────────

data class AssortingSaveRequest(
    @SerializedName("date")        val date: String,
    @SerializedName("shed_type")   val shedType: String?,
    @SerializedName("branch_id")   val branchId: Int,
    @SerializedName("mc_id")       val mcId: Int,
    @SerializedName("selector_id") val selectorId: Int,
    @SerializedName("quality_id")  val qualityId: Int,
    @SerializedName("trolly_id")   val trollyId: Int?,
    @SerializedName("trolly_no")   val trollyNo: String?,
    @SerializedName("gross_wt")    val grossWt: Double,
    @SerializedName("tare_wt")     val tareWt: Double,
    @SerializedName("net_wt")      val netWt: Double,
    @SerializedName("user_id")     val userId: Int
)

data class AssortingSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

data class AssortingEntry(
    @SerializedName("id")            val id: Int?,
    @SerializedName("entry_date")    val entryDate: String?,
    @SerializedName("shed_type")     val shedType: String?,
    @SerializedName("mc_id")         val mcId: Int?,
    @SerializedName("mc_no")         val mcNo: String?,
    @SerializedName("selector_id")   val selectorId: Int?,
    @SerializedName("selector_name") val selectorName: String?,
    @SerializedName("quality_id")    val qualityId: Int?,
    @SerializedName("quality")       val quality: String?,
    @SerializedName("trolly_id")     val trollyId: Int?,
    @SerializedName("trolly_no")     val trollyNo: String?,
    @SerializedName("gross_wt")      val grossWt: Double?,
    @SerializedName("tare_wt")       val tareWt: Double?,
    @SerializedName("net_wt")        val netWt: Double?
)

data class AssortingEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<AssortingEntry>?
)

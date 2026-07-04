package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Finishing Qualities (tbl_finishing_quality_mst) ────────────────────────

data class FinishingQuality(
    @SerializedName("quality_id")      val qualityId: Int,
    @SerializedName("shr_name")        val shrName: String?,
    @SerializedName("quality_name")    val qualityName: String?,
    @SerializedName("bags_yards")      val bagsYards: Int?,
    @SerializedName("std_weight_bale") val stdWeightBale: Double?
) {
    fun label(): String =
        shrName?.takeIf { it.isNotBlank() } ?: qualityName ?: "Q$qualityId"
}

data class FinishingQualityResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("qualities") val qualities: List<FinishingQuality>?
)

// ── Customers (tbl_customer_mst) ───────────────────────────────────────────

data class Customer(
    @SerializedName("customer_id")   val customerId: Int,
    @SerializedName("shr_name")      val shrName: String?,
    @SerializedName("customer_name") val customerName: String?
) {
    fun label(): String =
        shrName?.takeIf { it.isNotBlank() } ?: customerName ?: "C$customerId"
}

data class CustomerResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("customers") val customers: List<Customer>?
)

// ── Bales Production Entries (tbl_daily_bales_transaction) ─────────────────

data class BalesProdEntry(
    @SerializedName("id")              val id: Int?,
    @SerializedName("tran_date")       val tranDate: String?,
    @SerializedName("spell_id")        val spellId: Int?,
    @SerializedName("fng_quality_id")  val fngQualityId: Int?,
    @SerializedName("quality")         val quality: String?,
    @SerializedName("customer_id")     val customerId: Int?,
    @SerializedName("customer")        val customer: String?,
    @SerializedName("prod_bales")      val prodBales: Int?,
    @SerializedName("issue_bales")     val issueBales: Int?,
    @SerializedName("act_bale_weight") val actBaleWeight: Double?,
    @SerializedName("std_bale_weight") val stdBaleWeight: Double?
)

data class BalesProdEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<BalesProdEntry>?,
    @SerializedName("total")   val total: Int?
)

data class BalesProdSaveRequest(
    @SerializedName("date")            val date: String,
    @SerializedName("spell_id")        val spellId: Int,
    @SerializedName("fng_quality_id")  val fngQualityId: Int,
    @SerializedName("customer_id")     val customerId: Int,
    @SerializedName("prod_bales")      val prodBales: Int,
    @SerializedName("act_bale_weight") val actBaleWeight: Double?
)

data class BalesProdUpdateRequest(
    @SerializedName("fng_quality_id")  val fngQualityId: Int?,
    @SerializedName("customer_id")     val customerId: Int?,
    @SerializedName("prod_bales")      val prodBales: Int?,
    @SerializedName("act_bale_weight") val actBaleWeight: Double?
)

data class BalesProdSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

// ── Bales Issue Entry — same table, issue_bales column ─────────────────────

data class BalesIssueSaveRequest(
    @SerializedName("date")            val date: String,
    @SerializedName("spell_id")        val spellId: Int,
    @SerializedName("fng_quality_id")  val fngQualityId: Int,
    @SerializedName("customer_id")     val customerId: Int,
    @SerializedName("issue_bales")     val issueBales: Int,
    @SerializedName("act_bale_weight") val actBaleWeight: Double?
)

data class BalesIssueUpdateRequest(
    @SerializedName("fng_quality_id")  val fngQualityId: Int?,
    @SerializedName("customer_id")     val customerId: Int?,
    @SerializedName("issue_bales")     val issueBales: Int?,
    @SerializedName("act_bale_weight") val actBaleWeight: Double?
)

// ── Bales Stock (quality + customer wise) ──────────────────────────────────

data class BalesStockItem(
    @SerializedName("quality")         val quality: String?,
    @SerializedName("customer")        val customer: String?,
    @SerializedName("std_weight_bale") val stdWeightBale: Double?,
    @SerializedName("stock")           val stock: Int?,
    @SerializedName("std_weight")      val stdWeight: Double?
)

data class BalesStockResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("rows")    val rows: List<BalesStockItem>?,
    @SerializedName("total")   val total: Int?
)

data class BalesStockPairResponse(
    @SerializedName("status")     val status: String?,
    @SerializedName("message")    val message: String?,
    @SerializedName("stock")      val stock: Int?,
    @SerializedName("std_weight") val stdWeight: Double?
)

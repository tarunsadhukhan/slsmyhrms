package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Spell ────────────────────────────────────────────────────────────────────

data class Spell(
    @SerializedName("spell_id")   val spellId: Int,
    @SerializedName("spell_name") val spellName: String?,
    @SerializedName("working_hours") val workingHours: Double? = 8.0
) {
    override fun toString() = spellName ?: ""
}

data class SpellResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("spells") val spells: List<Spell>?
)

// ── Doff master rows ─────────────────────────────────────────────────────────

data class DoffMachine(
    @SerializedName("mc_id")   val mcId: Int,
    @SerializedName("mc_name") val mcName: String?,
    @SerializedName("mc_code") val mcCode: String?
) {
    override fun toString(): String =
        listOfNotNull(mcCode?.takeIf { it.isNotBlank() }, mcName?.takeIf { it.isNotBlank() })
            .joinToString(" - ").ifBlank { "MC $mcId" }
}

data class DoffMachineResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("machines") val machines: List<DoffMachine>?
)

data class SpinningQuality(
    @SerializedName("quality_id")   val qualityId: Int,
    @SerializedName("quality_name") val qualityName: String?
) {
    override fun toString() = qualityName ?: ""
}

data class SpinningQualityResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("qualities") val qualities: List<SpinningQuality>?
)

data class Trolly(
    @SerializedName("trolly_id")     val trollyId: Int,
    @SerializedName("trolly_name")   val trollyName: String?,
    @SerializedName("trolly_weight") val trollyWeight: Double?,
    @SerializedName("bucket_weight") val bucketWeight: Double?
) {
    override fun toString() = trollyName ?: "T$trollyId"
}

data class TrollyResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("trollies")  val trollies: List<Trolly>?
)

// ── Defaults helper response ─────────────────────────────────────────────────

data class DoffDefaultsResponse(
    @SerializedName("status")        val status: String?,
    @SerializedName("quality_id")    val qualityId: Int?,
    @SerializedName("quality_name")  val qualityName: String?,
    @SerializedName("trolly_id")     val trollyId: Int?,
    @SerializedName("trolly_name")   val trollyName: String?,
    @SerializedName("trolly_weight") val trollyWeight: Double?,
    @SerializedName("bucket_weight") val bucketWeight: Double?,
    @SerializedName("eb_id")         val ebId: Int?,
    @SerializedName("emp_code")      val empCode: String?,
    @SerializedName("emp_name")      val empName: String?
)

// ── Transaction list / save ──────────────────────────────────────────────────

data class DoffTransaction(
    @SerializedName("id")            val id: Int?,
    @SerializedName("doff_date")     val doffDate: String?,
    @SerializedName("spell_id")      val spellId: Int?,
    @SerializedName("spell_name")    val spellName: String?,
    @SerializedName("mc_id")         val mcId: Int?,
    @SerializedName("mc_name")       val mcName: String?,
    @SerializedName("mc_code")       val mcCode: String?,
    @SerializedName("quality_id")    val qualityId: Int?,
    @SerializedName("quality_name")  val qualityName: String?,
    @SerializedName("trolly_id")     val trollyId: Int?,
    @SerializedName("trolly_name")   val trollyName: String?,
    @SerializedName("trolly_weight") val trollyWeight: Double?,
    @SerializedName("bucket_weight") val bucketWeight: Double?,
    @SerializedName("eb_id")         val ebId: Int?,
    @SerializedName("emp_code")      val empCode: String?,
    @SerializedName("emp_name")      val empName: String?,
    @SerializedName("gross_weight")  val grossWeight: Double?,
    @SerializedName("tare_weight")   val tareWeight: Double?,
    @SerializedName("net_weight")    val netWeight: Double?,
    @SerializedName("weight_type")   val weightType: String?,
    @SerializedName("branch_id")     val branchId: Int?
)

data class DoffListResponse(
    @SerializedName("status")       val status: String?,
    @SerializedName("message")      val message: String?,
    @SerializedName("transactions") val transactions: List<DoffTransaction>?
)

data class DoffSaveRequest(
    @SerializedName("id")           val id: Int? = null,
    @SerializedName("doff_date")    val doffDate: String,
    @SerializedName("spell_id")     val spellId: Int,
    @SerializedName("mc_id")        val mcId: Int,
    @SerializedName("quality_id")   val qualityId: Int?,
    @SerializedName("trolly_id")    val trollyId: Int?,
    @SerializedName("eb_id")        val ebId: Int,
    @SerializedName("gross_weight") val grossWeight: Double,
    @SerializedName("tare_weight")  val tareWeight: Double,
    @SerializedName("net_weight")   val netWeight: Double,
    @SerializedName("weight_type")  val weightType: String? = null,
    @SerializedName("branch_id")    val branchId: Int?,
    @SerializedName("user_id")      val userId: Int
)

data class DoffSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

// ── Validation / summary for "New Doff Entry" ────────────────────────────────

data class DoffMachineValidateResponse(
    @SerializedName("status")              val status: String?,
    @SerializedName("message")             val message: String?,
    @SerializedName("mc_id")               val mcId: Int?,
    @SerializedName("mc_no")               val mcNo: String?,
    @SerializedName("mc_name")             val mcName: String?,
    @SerializedName("mc_code")             val mcCode: String?,
    @SerializedName("mech_posting_code")   val mechPostingCode: Int?,
    @SerializedName("trolly_id")           val trollyId: Int?,
    @SerializedName("trolly_name")         val trollyName: String?,
    @SerializedName("trolly_posting_code") val trollyPostingCode: Int?,
    @SerializedName("trolly_weight")       val trollyWeight: Double?,
    @SerializedName("bucket_weight")       val bucketWeight: Double?
)

data class DoffTrollyValidateResponse(
    @SerializedName("status")        val status: String?,
    @SerializedName("message")       val message: String?,
    @SerializedName("trolly_id")     val trollyId: Int?,
    @SerializedName("trolly_no")     val trollyNo: String?,
    @SerializedName("trolly_name")   val trollyName: String?,
    @SerializedName("trolly_weight") val trollyWeight: Double?,
    @SerializedName("bucket_weight") val bucketWeight: Double?
)

data class DoffSummaryRow(
    @SerializedName("mc_id")      val mcId: Int?,
    @SerializedName("mc_no")      val mcNo: String?,
    @SerializedName("mc_name")    val mcName: String?,
    @SerializedName("no_of_doff") val noOfDoff: Int?,
    @SerializedName("total_wt")   val totalWt: Double?
)

data class DoffSummaryResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("summary") val summary: List<DoffSummaryRow>?
)

// ── Spell-wise Frame Entry ───────────────────────────────────────────────────

data class FrameEntry(
    @SerializedName("mc_id")      val mcId: Int,
    @SerializedName("quality_id") val qualityId: Int?
)

data class FrameEntriesResponse(
    @SerializedName("status")        val status: String?,
    @SerializedName("message")       val message: String?,
    @SerializedName("mc_ids")        val mcIds: List<Int>?,
    @SerializedName("entries")       val entries: List<FrameEntry>?,
    @SerializedName("is_fallback")   val isFallback: Boolean?,
    @SerializedName("fallback_date") val fallbackDate: String?
)

data class FrameMachineDefaultsResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("message")  val message: String?,
    @SerializedName("defaults") val defaults: List<FrameEntry>?
)

data class FrameEntriesSaveRequest(
    @SerializedName("date")      val date: String,
    @SerializedName("spell_id")  val spellId: Int,
    @SerializedName("branch_id") val branchId: Int,
    @SerializedName("user_id")   val userId: Int,
    @SerializedName("entries")   val entries: List<FrameEntry>
)

data class FrameEntriesSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("count")   val count: Int?
)

data class WindingQualityResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("qualities") val qualities: List<SpinningQuality>?
)

data class LastQualityByMcResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: Map<String, Int>?
)

// ── Winding Entry (individual rows) ─────────────────────────────────────────

data class WindingEbLookupResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("message")  val message: String?,
    @SerializedName("eb_id")    val ebId: Long?,
    @SerializedName("emp_code") val empCode: String?,
    @SerializedName("emp_name") val empName: String?
)

data class WindingEntry(
    @SerializedName("id")           val id: Int?,
    @SerializedName("tran_date")    val tranDate: String?,
    @SerializedName("eb_id")        val ebId: Long?,
    @SerializedName("emp_code")     val empCode: String?,
    @SerializedName("emp_name")     val empName: String?,
    @SerializedName("quality_id")   val qualityId: Int?,
    @SerializedName("quality_name") val qualityName: String?
)

data class WindingEntriesResponse(
    @SerializedName("status")        val status: String?,
    @SerializedName("entries")       val entries: List<WindingEntry>?,
    @SerializedName("is_fallback")   val isFallback: Boolean?,
    @SerializedName("fallback_date") val fallbackDate: String?
)

data class WindingEntrySaveRequest(
    @SerializedName("date")       val date: String,
    @SerializedName("spell_id")   val spellId: Int,
    @SerializedName("branch_id")  val branchId: Int,
    @SerializedName("eb_id")      val ebId: Long,
    @SerializedName("quality_id") val qualityId: Int?,
    @SerializedName("user_id")    val userId: Int
)

data class WindingEntryUpdateRequest(
    @SerializedName("eb_id")      val ebId: Long?,
    @SerializedName("quality_id") val qualityId: Int?
)

data class WindingEntrySaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

// ── Continuous Winding Entry (date + quality + prod_kgs) ─────────────────────

data class ContWindingEntry(
    @SerializedName("id")           val id: Int?,
    @SerializedName("tran_date")    val tranDate: String?,
    @SerializedName("quality_id")   val qualityId: Int?,
    @SerializedName("quality_name") val qualityName: String?,
    @SerializedName("prod_kgs")     val prodKgs: Int?
)

data class ContWindingEntriesResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("entries") val entries: List<ContWindingEntry>?
)

data class ContWindingEntrySaveRequest(
    @SerializedName("date")       val date: String,
    @SerializedName("quality_id") val qualityId: Int,
    @SerializedName("prod_kgs")   val prodKgs: Int?,
    @SerializedName("user_id")    val userId: Int
)

data class ContWindingEntrySaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

// ── Spg Doff Entry (1) – mech posting codes ──────────────────────────────────

data class Spg1MechCode(
    @SerializedName("mc_id")             val mcId: Int,
    @SerializedName("mc_name")           val mcName: String?,
    @SerializedName("mc_code")           val mcCode: String?,
    @SerializedName("mech_posting_code") val mechPostingCode: Int?
)

data class Spg1MechCodesResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("codes")   val codes: List<Spg1MechCode>?
)

// ── Spg Doff Entry (1) – summary list ────────────────────────────────────────

data class Spg1SummaryRow(
    @SerializedName("mc_id")             val mcId: Int,
    @SerializedName("mc_code")           val mcCode: String?,
    @SerializedName("mc_name")           val mcName: String?,
    @SerializedName("mech_posting_code") val mechPostingCode: Int?,
    @SerializedName("weights")           val weights: List<Double>?,
    @SerializedName("no_of_doff")        val noOfDoff: Int,
    @SerializedName("total_wt")          val totalWt: Double
)

data class Spg1SummaryResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("summary") val summary: List<Spg1SummaryRow>?
)

// ── Winding Entry (2) — S/C + trolly + weight ────────────────────────────────

data class We2Employee(
    @SerializedName("eb_id")    val ebId: Long?,
    @SerializedName("emp_code") val empCode: String?,
    @SerializedName("emp_name") val empName: String?
)

data class We2EmployeesResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("employees") val employees: List<We2Employee>?
)

data class We2EmpLookupResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("message")  val message: String?,
    @SerializedName("eb_id")    val ebId: Long?,
    @SerializedName("emp_code") val empCode: String?,
    @SerializedName("emp_name") val empName: String?
)

data class We2SummaryRow(
    @SerializedName("id")           val id: Int?,
    @SerializedName("eb_id")        val ebId: Long?,
    @SerializedName("emp_code")     val empCode: String?,
    @SerializedName("emp_name")     val empName: String?,
    @SerializedName("sc_type")      val scType: String?,
    @SerializedName("trolly_id")    val trollyId: Int?,
    @SerializedName("trolly_name")  val trollyName: String?,
    @SerializedName("gross_weight") val grossWeight: Double?,
    @SerializedName("tare_weight")  val tareWeight: Double?,
    @SerializedName("net_weight")   val netWeight: Double?
)

data class We2SummaryResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("summary") val summary: List<We2SummaryRow>?
)

data class We2SaveRequest(
    @SerializedName("date")         val date: String,
    @SerializedName("spell_id")     val spellId: Int,
    @SerializedName("branch_id")    val branchId: Int,
    @SerializedName("eb_id")        val ebId: Long,
    @SerializedName("sc_type")      val scType: String,
    @SerializedName("trolly_id")    val trollyId: Int?,
    @SerializedName("gross_weight") val grossWeight: Double,
    @SerializedName("tare_weight")  val tareWeight: Double,
    @SerializedName("net_weight")   val netWeight: Double,
    @SerializedName("user_id")      val userId: Int
)

data class We2SaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

data class We2GroupedRow(
    @SerializedName("eb_id")      val ebId: Long?,
    @SerializedName("emp_code")   val empCode: String?,
    @SerializedName("emp_name")   val empName: String?,
    @SerializedName("weights")    val weights: List<Double>?,
    @SerializedName("no_of_doff") val noOfDoff: Int,
    @SerializedName("total_wt")   val totalWt: Double
)

data class We2GroupedResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("summary") val summary: List<We2GroupedRow>?
)

// ── Quality-wise Shift-wise Report ──────────────────────────────────────────

data class QualityShiftReportRow(
    @SerializedName("quality_name") val qualityName: String?,
    @SerializedName("shift_a")      val shiftA: Double?,
    @SerializedName("shift_b")      val shiftB: Double?,
    @SerializedName("shift_c")      val shiftC: Double?,
    @SerializedName("total")        val total: Double?
)

data class GrandTotalRow(
    @SerializedName("shift_a") val shiftA: Double?,
    @SerializedName("shift_b") val shiftB: Double?,
    @SerializedName("shift_c") val shiftC: Double?,
    @SerializedName("total")   val total: Double?
)

data class QualityShiftReportResponse(
    @SerializedName("status")      val status: String?,
    @SerializedName("message")     val message: String?,
    @SerializedName("report")      val report: List<QualityShiftReportRow>?,
    @SerializedName("grand_total") val grandTotal: GrandTotalRow?
)

// ── Spreader Production Entry ───────────────────────────────────────────────

data class SpreaderMachine(
    @SerializedName("mc_id")    val mcId: Int,
    @SerializedName("mc_name")  val mcName: String?,
    @SerializedName("mc_code")  val mcCode: String?,
    @SerializedName("mc_short") val mcShort: String?,
    @SerializedName("dept_id")  val deptId: Int?,
    @SerializedName("dept_name") val deptName: String?
) {
    fun label(): String =
        listOfNotNull(
            mcName?.takeIf { it.isNotBlank() },
            mcCode?.takeIf { it.isNotBlank() },
            mcShort?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: "MC$mcId"
}

data class SpreaderMachineResponse(
    @SerializedName("status")   val status: String?,
    @SerializedName("message")  val message: String?,
    @SerializedName("machines") val machines: List<SpreaderMachine>?
)

data class SpreaderQuality(
    @SerializedName("quality_id")   val qualityId: Int,
    @SerializedName("shr_name")     val shrName: String?,
    @SerializedName("quality_name") val qualityName: String?
) {
    fun label(): String =
        shrName?.takeIf { it.isNotBlank() } ?: qualityName ?: "Q$qualityId"
}

data class SpreaderQualityResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("qualities") val qualities: List<SpreaderQuality>?
)

data class SpreaderProdEntry(
    @SerializedName("id")              val id: Int?,
    @SerializedName("entry_date")      val entryDate: String?,
    @SerializedName("spell_id")        val spellId: Int?,
    @SerializedName("mc_id")           val mcId: Int?,
    @SerializedName("mc_no")           val mcNo: String?,
    @SerializedName("quality_id")      val qualityId: Int?,
    @SerializedName("quality")         val quality: String?,
    @SerializedName("sprd_quality_id") val sprdQualityId: Int?,
    @SerializedName("sprd_quality")    val sprdQuality: String?,
    @SerializedName("bin_no")          val binNo: String?,
    @SerializedName("production")      val production: Double?
)

data class SpreaderProdEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<SpreaderProdEntry>?
)

data class SpreaderProdSaveRequest(
    @SerializedName("date")            val date: String,
    @SerializedName("spell_id")        val spellId: Int,
    @SerializedName("branch_id")       val branchId: Int,
    @SerializedName("mc_id")           val mcId: Int,
    @SerializedName("quality_id")      val qualityId: Int,
    @SerializedName("sprd_quality_id") val sprdQualityId: Int?,
    @SerializedName("bin_no")          val binNo: String?,
    @SerializedName("production")      val production: Double,
    @SerializedName("user_id")         val userId: Int
)

data class SpreaderProdSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

data class SpreaderProdUpdateRequest(
    @SerializedName("mc_id")           val mcId: Int?,
    @SerializedName("quality_id")      val qualityId: Int?,
    @SerializedName("sprd_quality_id") val sprdQualityId: Int?,
    @SerializedName("bin_no")          val binNo: String?,
    @SerializedName("production")      val production: Double?,
    @SerializedName("user_id")         val userId: Int?
)

// ── Spreader Issue Entry ────────────────────────────────────────────────────

data class SpreaderIssueEntry(
    @SerializedName("id")         val id: Int?,
    @SerializedName("entry_date") val entryDate: String?,
    @SerializedName("spell_id")   val spellId: Int?,
    @SerializedName("quality_id") val qualityId: Int?,
    @SerializedName("quality")    val quality: String?,
    @SerializedName("bin_no")     val binNo: String?,
    @SerializedName("issue")      val issue: Double?
)

data class SpreaderIssueEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<SpreaderIssueEntry>?
)

data class SpreaderIssueSaveRequest(
    @SerializedName("date")       val date: String,
    @SerializedName("spell_id")   val spellId: Int,
    @SerializedName("branch_id")  val branchId: Int,
    @SerializedName("quality_id") val qualityId: Int,
    @SerializedName("bin_no")     val binNo: String?,
    @SerializedName("issue")      val issue: Double,
    @SerializedName("user_id")    val userId: Int
)

data class SpreaderIssueSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

data class SpreaderIssueUpdateRequest(
    @SerializedName("quality_id") val qualityId: Int?,
    @SerializedName("bin_no")     val binNo: String?,
    @SerializedName("issue")      val issue: Double?,
    @SerializedName("user_id")    val userId: Int?
)

// ── Finishing → Other Entries ───────────────────────────────────────────────

data class FinishingEntry(
    @SerializedName("id")          val id: Int?,
    @SerializedName("entry_date")  val entryDate: String?,
    @SerializedName("spell_id")    val spellId: Int?,
    @SerializedName("spell_name")  val spellName: String?,
    @SerializedName("looms")       val looms: Int?,
    @SerializedName("cuts")        val cuts: Int?,
    @SerializedName("hemming")     val hemming: Int?,
    @SerializedName("heracle")     val heracle: Int?,
    @SerializedName("cutting")     val cutting: Int?,
    @SerializedName("branding")    val branding: Int?,
    @SerializedName("hand_sewer")  val handSewer: Int?,
    @SerializedName("bales")       val bales: Int?,
    @SerializedName("issue_bales") val issueBales: Int?
)

data class FinishingEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<FinishingEntry>?
)

data class FinishingSaveRequest(
    @SerializedName("date")        val date: String,
    @SerializedName("spell_id")    val spellId: Int,
    @SerializedName("looms")       val looms: Int?,
    @SerializedName("cuts")        val cuts: Int?,
    @SerializedName("hemming")     val hemming: Int?,
    @SerializedName("heracle")     val heracle: Int?,
    @SerializedName("cutting")     val cutting: Int?,
    @SerializedName("branding")    val branding: Int?,
    @SerializedName("hand_sewer")  val handSewer: Int?,
    @SerializedName("bales")       val bales: Int?,
    @SerializedName("issue_bales") val issueBales: Int?,
    @SerializedName("user_id")     val userId: Int
)

data class FinishingSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

data class FinishingUpdateRequest(
    @SerializedName("date")        val date: String?,
    @SerializedName("spell_id")    val spellId: Int?,
    @SerializedName("looms")       val looms: Int?,
    @SerializedName("cuts")        val cuts: Int?,
    @SerializedName("hemming")     val hemming: Int?,
    @SerializedName("heracle")     val heracle: Int?,
    @SerializedName("cutting")     val cutting: Int?,
    @SerializedName("branding")    val branding: Int?,
    @SerializedName("hand_sewer")  val handSewer: Int?,
    @SerializedName("bales")       val bales: Int?,
    @SerializedName("issue_bales") val issueBales: Int?,
    @SerializedName("user_id")     val userId: Int?
)

data class SpreaderQualityStockResponse(
    @SerializedName("status")     val status: String?,
    @SerializedName("message")    val message: String?,
    @SerializedName("quality_id") val qualityId: Int?,
    @SerializedName("shr_name")   val shrName: String?,
    @SerializedName("stock")      val stock: Double?
)

data class SpreaderQualityStockItem(
    @SerializedName("quality_id") val qualityId: Int?,
    @SerializedName("shr_name")   val quality: String?,
    @SerializedName("bin_no")     val binNo: String?,
    @SerializedName("stock")      val stock: Double?
)

data class SpreaderQualityStockListResponse(
    @SerializedName("status")    val status: String?,
    @SerializedName("message")   val message: String?,
    @SerializedName("qualities") val rows: List<SpreaderQualityStockItem>?
)

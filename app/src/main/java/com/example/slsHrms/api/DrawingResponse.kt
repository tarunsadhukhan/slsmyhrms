package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Drawing Sheds Response ───────────────────────────────────────────────────

data class DrawingShedsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("sheds") val sheds: List<String>?
)

// ── Drawing Machines Response ────────────────────────────────────────────────

data class DrawingMachine(
    @SerializedName("mc_id") val mcId: Int,
    @SerializedName("short_name") val mcShortName: String?,
    @SerializedName("const_meter") val contMeter: Double?
)

data class DrawingMachinesResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("machines") val machines: List<DrawingMachine>?
)

// ── Drawing Opening Meter Response ───────────────────────────────────────────

data class DrawingOpeningMeterResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("opening_meter") val openingMeter: Double?
)

// ── Drawing Entry Save Request/Response ──────────────────────────────────────

data class DrawingEntrySaveRequest(
    @SerializedName("date") val date: String,
    @SerializedName("spell_id") val spellId: Int,
    @SerializedName("shed_type") val shedType: String,
    @SerializedName("mc_id") val mcId: Int,
    @SerializedName("opening_meter") val openingMeter: Double,
    @SerializedName("closing_meter") val closingMeter: Double,
    @SerializedName("hours") val hours: Double,
    @SerializedName("const_value") val constValue: Double,
    @SerializedName("branch_id") val branchId: Int?,
    @SerializedName("user_id") val userId: Int
)

data class DrawingEntrySaveResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Int?,
    @SerializedName("unit") val unit: Double?,
    @SerializedName("eff") val eff: Double?
)

// ── Drawing Summary Response ─────────────────────────────────────────────────

data class DrawingSummaryItem(
    @SerializedName("mc_id") val mcId: Int?,
    @SerializedName("short_name") val mcShortName: String?,
    @SerializedName("unit") val unit: Double?,
    @SerializedName("eff") val eff: Double?
)

data class DrawingSummaryResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("summary") val summary: List<DrawingSummaryItem>?
)


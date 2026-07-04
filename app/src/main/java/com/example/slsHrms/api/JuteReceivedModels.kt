package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

// ── Save / Update request ────────────────────────────────────────────────────

data class JuteReceivedSaveRequest(
    @SerializedName("date")        val date: String,
    @SerializedName("quality_id")  val qualityId: Int,
    @SerializedName("no_of_bales") val noOfBales: Int?,
    @SerializedName("weight")      val weight: Int?,
    @SerializedName("branch_id")   val branchId: Int,
    @SerializedName("user_id")     val userId: Int
)

data class JuteReceivedSaveResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("id")      val id: Int?
)

// ── List ─────────────────────────────────────────────────────────────────────

data class JuteReceivedEntry(
    @SerializedName("id")          val id: Int?,
    @SerializedName("recv_date")   val recvDate: String?,
    @SerializedName("quality_id")  val qualityId: Int?,
    @SerializedName("quality")     val quality: String?,
    @SerializedName("no_of_bales") val noOfBales: Int?,
    @SerializedName("weight")      val weight: Int?
)

data class JuteReceivedEntryListResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("entries") val entries: List<JuteReceivedEntry>?
)

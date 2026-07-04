package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Shift(
    @SerializedName("id")
    val id: Int,

    @SerializedName("spell_id")
    val spellId: Int? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("start_time")
    val startTime: String? = null,

    @SerializedName("end_time")
    val endTime: String? = null,

    @SerializedName("shift_hours")
    val shiftHours: Double? = null,

    @SerializedName("working_hours")
    val workingHours: Double? = 8.0
) {
    override fun toString(): String = name
}

data class ShiftResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val shifts: List<Shift>?,

    @SerializedName("total")
    val total: Int?
)


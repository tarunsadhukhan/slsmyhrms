package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class CardTrendResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("metric")  val metric: String?,
    @SerializedName("label")   val label: String?,
    @SerializedName("series")  val series: List<String>?,
    @SerializedName("days")    val days: List<TrendDay>?,
    @SerializedName("message") val message: String?
)

data class TrendDay(
    @SerializedName("date")   val date: String?,
    @SerializedName("value")  val value: Double?,
    @SerializedName("values") val values: List<Double>?
)

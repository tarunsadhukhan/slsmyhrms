package com.example.slsHrms.api
import com.google.gson.annotations.SerializedName
data class FaceRecognitionResponse(
    @SerializedName("status")            val status: String?,
    @SerializedName("eb_id")             val ebId: Int?,
    @SerializedName("emp_code")          val empCode: String?,
    @SerializedName("emp_name")          val empName: String?,
    @SerializedName("photo_html")        val photoHtml: String?,
    @SerializedName("default_department_id")  val defaultDepartmentId: Int?,
    @SerializedName("default_designation_id") val defaultDesignationId: Int?,
    @SerializedName("default_machine_ids")    val defaultMachineIds: List<Int>?,
    @SerializedName("message")           val message: String?
)

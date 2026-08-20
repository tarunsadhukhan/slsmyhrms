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
    @SerializedName("message")           val message: String?,
    // True on a synthetic reply from OfflineInterceptor: the record is safely in
    // the outbox but has not reached the server yet.
    @SerializedName("queued")            val queued: Boolean? = null,
    // Set by the server when an offline face match was re-verified with dlib.
    @SerializedName("face_verify_status") val faceVerifyStatus: String? = null,
    @SerializedName("duplicate")         val duplicate: Boolean? = null
)

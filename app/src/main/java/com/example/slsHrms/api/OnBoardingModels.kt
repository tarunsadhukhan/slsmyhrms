package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class OnBoardingEmployeeResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("eb_id") val ebId: Int?,
    @SerializedName("emp_code") val empCode: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("department_name") val departmentName: String?,
    @SerializedName("designation_name") val designationName: String?,
    @SerializedName("branch_id") val branchId: Int?,
    @SerializedName("face_count") val faceCount: Int?,
    @SerializedName("can_register") val canRegister: Boolean?,
    @SerializedName("message") val message: String?
)

data class OnBoardingRegisterRequest(
    @SerializedName("emp_code") val empCode: String,
    @SerializedName("face_image") val faceImage: String,
    @SerializedName("branch_id") val branchId: Int? = null,
    // MobileFaceNet embedding computed on the device at enrolment, so a newly
    // onboarded worker is matchable offline immediately instead of waiting for
    // the next tools/backfill_mobile_embeddings.py run. Null when the model
    // asset is not shipped in this build, or no face was found in the photo.
    @SerializedName("embedding_mobile") val embeddingMobile: List<Float>? = null,
    @SerializedName("mobile_model_ver") val mobileModelVer: String? = null
)

data class OnBoardingRegisterResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("face_count") val faceCount: Int?,
    @SerializedName("can_register") val canRegister: Boolean?,
    // True on a synthetic reply from OfflineInterceptor: the enrolment is in the
    // outbox, not yet on the server. The 3-face limit and the employee's
    // existence are checked when it uploads.
    @SerializedName("queued") val queued: Boolean? = null
)


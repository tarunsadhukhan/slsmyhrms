package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("user")
    val user: UserData?
) {
    val isSuccess: Boolean
        get() = status == "success"
}

data class UserData(
    @SerializedName("user_id")
    val id: Int?,

    @SerializedName("email_id")
    val username: String?,

    @SerializedName("name")
    val fullName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("role")
    val role: String?
)

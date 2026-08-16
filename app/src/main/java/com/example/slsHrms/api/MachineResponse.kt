package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class Machine(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("mech_code")
    val mechCode: String? = null,

    @SerializedName("machine_no")
    val machineNo: String? = null
) {
    /**
     * Returns display text as mech_code, falling back to machine name / ID
     */
    fun getDisplayName(): String {
        val code = mechCode?.trim() ?: ""
        val machineName = name?.trim() ?: ""

        return when {
            // Show only the mech_code when available
            code.isNotEmpty() -> code
            // Fallback to machine name if no code
            machineName.isNotEmpty() -> machineName
            // Last resort - show ID if available
            id != null && id > 0 -> "Machine #$id"
            else -> "No name"
        }
    }
    
    override fun toString(): String = getDisplayName()
}

data class MachineResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val data: List<Machine>?,

    @SerializedName("total")
    val total: Int?
)


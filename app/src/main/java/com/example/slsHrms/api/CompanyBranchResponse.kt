package com.example.slsHrms.api

import com.google.gson.annotations.SerializedName

data class CompanyResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("companies")
    val companies: List<CompanyData>?,

    @SerializedName("data")
    val data: List<CompanyData>?,

    @SerializedName("company")
    val company: List<CompanyData>?,

    @SerializedName("result")
    val result: List<CompanyData>?
) {
    fun companyList(): List<CompanyData> = companies ?: data ?: company ?: result ?: emptyList()
}

data class CompanyData(
    @SerializedName("co_id")
    val id: Int?,

    @SerializedName("co_name")
    val name: String?,

    @SerializedName("co_logo")
    val logo: String?
) {
    override fun toString(): String = name ?: "Company"
}

data class BranchResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("branches")
    val branches: List<BranchData>?,

    @SerializedName("data")
    val data: List<BranchData>?,

    @SerializedName("branch")
    val branch: List<BranchData>?,

    @SerializedName("result")
    val result: List<BranchData>?
) {
    fun branchList(): List<BranchData> = branches ?: data ?: branch ?: result ?: emptyList()
}

data class BranchData(
    @SerializedName("br_id")
    val id: Int?,

    @SerializedName("co_id")
    val companyId: Int?,

    @SerializedName("br_name")
    val name: String?
) {
    override fun toString(): String = name ?: "Branch"
}


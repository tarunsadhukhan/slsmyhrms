package com.example.slsHrms.permissions

import com.google.gson.annotations.SerializedName

/**
 * A single row coming back from GET /menu-permissions.
 *
 * One row = one menu (master card, header or leaf) plus the
 * action flags (view / add / modify / delete / print / all)
 * that the logged-in user holds on that menu.
 */
data class MenuPermission(
    @SerializedName("menu_id")        val menuId: Int,
    @SerializedName("menu_key")       val menuKey: String,
    @SerializedName("menu_name")      val menuName: String,
    @SerializedName("parent_id")      val parentId: Int?,
    @SerializedName("menu_order")     val menuOrder: Int = 0,
    @SerializedName("icon")           val icon: String? = null,
    @SerializedName("activity_class") val activityClass: String? = null,
    @SerializedName("is_group")       val isGroup: Int = 0,

    @SerializedName("can_view")   val canView: Int   = 0,
    @SerializedName("can_add")    val canAdd: Int    = 0,
    @SerializedName("can_modify") val canModify: Int = 0,
    @SerializedName("can_delete") val canDelete: Int = 0,
    @SerializedName("can_print")  val canPrint: Int  = 0,
    @SerializedName("can_all")    val canAll: Int    = 0,
) {
    /** Convenience getters – fold `can_all` into every other flag. */
    val view:   Boolean get() = canAll == 1 || canView   == 1
    val add:    Boolean get() = canAll == 1 || canAdd    == 1
    val modify: Boolean get() = canAll == 1 || canModify == 1
    val delete: Boolean get() = canAll == 1 || canDelete == 1
    val print:  Boolean get() = canAll == 1 || canPrint  == 1
}

data class MenuPermissionResponse(
    @SerializedName("status")  val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("menus")   val menus: List<MenuPermission>?
) {
    val isSuccess: Boolean get() = status == "success"
}

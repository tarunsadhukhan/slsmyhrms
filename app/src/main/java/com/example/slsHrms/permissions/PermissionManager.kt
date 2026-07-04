package com.example.slsHrms.permissions

import android.content.Context
import android.view.View
import com.example.slsHrms.api.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In-memory + SharedPreferences cache of the logged-in user's menu permissions.
 *
 * USAGE
 * -----
 *   // 1) In LoginActivity, right after a successful login:
 *   PermissionManager.refresh(this, userId) { ok ->
 *       startActivity(Intent(this, DashboardActivity::class.java))
 *   }
 *
 *   // 2) Anywhere later (Dashboard, any master/entry activity):
 *   if (!PermissionManager.canView(this, "card_departments")) {
 *       binding.cardDepartments.visibility = View.GONE
 *   }
 *   PermissionManager.applyToView(this, "card_employees",
 *                                 PermissionAction.ADD,
 *                                 binding.btnAddEmployee)
 */
object PermissionManager {

    private const val PREFS       = "MenuPermPrefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_JSON    = "permissions_json"

    private val gson = Gson()

    /** menu_key -> MenuPermission */
    @Volatile private var cache: Map<String, MenuPermission> = emptyMap()

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    /**
     * Fetch the permissions for [userId] from the backend, save them, and
     * call [onDone] with `true` on success, `false` on failure.
     * On failure the previously cached value is kept.
     */
    fun refresh(ctx: Context, userId: Int, onDone: ((Boolean) -> Unit)? = null) {
        RetrofitClient.getApiService(ctx)
            .getMenuPermissions(userId)
            .enqueue(object : Callback<MenuPermissionResponse> {
                override fun onResponse(
                    call: Call<MenuPermissionResponse>,
                    response: Response<MenuPermissionResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.isSuccess == true) {
                        val list = body.menus.orEmpty()
                        cache = list.associateBy { it.menuKey }
                        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putInt(KEY_USER_ID, userId)
                            .putString(KEY_JSON, gson.toJson(list))
                            .apply()
                        onDone?.invoke(true)
                    } else {
                        onDone?.invoke(false)
                    }
                }
                override fun onFailure(call: Call<MenuPermissionResponse>, t: Throwable) {
                    onDone?.invoke(false)
                }
            })
    }

    /** Re-hydrate cache from disk. Call once from Application.onCreate or
     *  lazily from the first `can*` call. */
    fun loadFromCacheIfNeeded(ctx: Context) {
        if (cache.isNotEmpty()) return
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return
        val type = object : TypeToken<List<MenuPermission>>() {}.type
        val list: List<MenuPermission> = gson.fromJson(json, type) ?: emptyList()
        cache = list.associateBy { it.menuKey }
    }

    fun clear(ctx: Context) {
        cache = emptyMap()
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    fun get(ctx: Context, menuKey: String): MenuPermission? {
        loadFromCacheIfNeeded(ctx)
        return cache[menuKey]
    }

    fun all(ctx: Context): List<MenuPermission> {
        loadFromCacheIfNeeded(ctx)
        return cache.values.sortedBy { it.menuOrder }
    }

    fun canView  (ctx: Context, key: String) = get(ctx, key)?.view   == true
    fun canAdd   (ctx: Context, key: String) = get(ctx, key)?.add    == true
    fun canModify(ctx: Context, key: String) = get(ctx, key)?.modify == true
    fun canDelete(ctx: Context, key: String) = get(ctx, key)?.delete == true
    fun canPrint (ctx: Context, key: String) = get(ctx, key)?.print  == true

    fun can(ctx: Context, key: String, action: PermissionAction): Boolean = when (action) {
        PermissionAction.VIEW   -> canView(ctx, key)
        PermissionAction.ADD    -> canAdd(ctx, key)
        PermissionAction.MODIFY -> canModify(ctx, key)
        PermissionAction.DELETE -> canDelete(ctx, key)
        PermissionAction.PRINT  -> canPrint(ctx, key)
    }

    // ---------------------------------------------------------------------
    // View helpers – the easiest way to wire permissions into the UI.
    // ---------------------------------------------------------------------

    /** Hide [view] (View.GONE) when the user does not have [action] on [menuKey]. */
    fun applyToView(ctx: Context, menuKey: String,
                    action: PermissionAction, view: View?) {
        if (view == null) return
        view.visibility = if (can(ctx, menuKey, action)) View.VISIBLE else View.GONE
    }

    /** Convenience: hide the whole menu row when can_view = false. */
    fun applyVisibility(ctx: Context, menuKey: String, view: View?) =
        applyToView(ctx, menuKey, PermissionAction.VIEW, view)
}

enum class PermissionAction { VIEW, ADD, MODIFY, DELETE, PRINT }

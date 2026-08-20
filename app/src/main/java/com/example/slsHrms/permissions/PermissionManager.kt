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
    private const val KEY_CHECKED = "checked_at"

    // ── Offline staleness ladder (Part D, Tier 4) ─────────────────────────
    // Permissions are the one cached thing where being stale is a *security*
    // problem. But a mill phone that cannot open the attendance screen because
    // it has not seen the server today is worse than a slightly stale grant, so
    // the ladder fails toward "production continues, privileges shrink".
    //   0-7 days   trust silently
    //   7-14 days  trust, but say so
    //   >14 days   restricted: attendance + granted entry screens still work,
    //              master screens and every edit/delete are locked
    private const val WARN_DAYS_DEFAULT = 7
    private const val LOCK_DAYS_DEFAULT = 14
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Screens that administer data rather than record production. */
    private val MASTER_KEYS = setOf(
        "menu_onboarding", "card_employees", "ic_masters", "ic_employee", "ic_face_register"
    )

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
                            .putLong(KEY_CHECKED, System.currentTimeMillis())
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
     *  lazily from the first `can*` call.
     *
     *  The cached set is only used for the user it was fetched for. Without that
     *  check a second operator signing in on the same device — offline, so the
     *  refresh cannot run — would silently inherit the previous user's menus.
     */
    fun loadFromCacheIfNeeded(ctx: Context) {
        if (cache.isNotEmpty()) return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cachedFor = prefs.getInt(KEY_USER_ID, 0)
        val currentUser = ctx.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            .getInt("user_id", 0)
        if (cachedFor == 0 || currentUser == 0 || cachedFor != currentUser) return
        val json = prefs.getString(KEY_JSON, null) ?: return
        val type = object : TypeToken<List<MenuPermission>>() {}.type
        val list: List<MenuPermission> = gson.fromJson(json, type) ?: emptyList()
        cache = list.associateBy { it.menuKey }
    }

    /**
     * End the session but KEEP the cached grant on disk, so the same user can
     * sign in offline afterwards and still see their menus. Anyone else is
     * locked out of it by the user-id check in [loadFromCacheIfNeeded], and the
     * staleness ladder still shrinks privileges over time.
     */
    fun clear(ctx: Context) {
        cache = emptyMap()
    }

    /** Wipe the grant entirely — for a real account switch, not a logout. */
    fun forget(ctx: Context) {
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

    // ── Staleness ladder ─────────────────────────────────────────────────

    /** Days since the permission set was last confirmed with the server. */
    fun staleDays(ctx: Context): Int {
        val checked = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_CHECKED, 0L)
        if (checked <= 0L) return 0   // never pulled = fresh login flow, not stale
        return ((System.currentTimeMillis() - checked) / DAY_MS).toInt()
    }

    private fun warnDays(ctx: Context) =
        com.example.slsHrms.sync.SyncEngine.config(ctx, "perm_stale_warn_days", WARN_DAYS_DEFAULT)

    private fun lockDays(ctx: Context) =
        com.example.slsHrms.sync.SyncEngine.config(ctx, "perm_stale_lock_days", LOCK_DAYS_DEFAULT)

    fun isRestricted(ctx: Context): Boolean = staleDays(ctx) > lockDays(ctx)

    /** Banner text for the dashboard, or null when the cache is fresh enough. */
    fun stalenessNotice(ctx: Context): String? {
        val days = staleDays(ctx)
        return when {
            days > lockDays(ctx) ->
                "Restricted mode — permissions last checked $days days ago. " +
                    "Attendance and entries still work; master screens and edits are locked."
            days > warnDays(ctx) ->
                "Permissions last checked $days days ago — connect to refresh."
            else -> null
        }
    }

    /** Force a re-pull now; called on any 403 and on role changes. */
    fun invalidate(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_CHECKED, 0L).apply()
    }

    fun canView(ctx: Context, key: String): Boolean {
        if (get(ctx, key)?.view != true) return false
        // Restricted: production screens stay open, administration does not.
        return !(isRestricted(ctx) && key in MASTER_KEYS)
    }

    fun canAdd(ctx: Context, key: String): Boolean {
        if (get(ctx, key)?.add != true) return false
        return !(isRestricted(ctx) && key in MASTER_KEYS)
    }

    // Editing and deleting are never allowed on a stale grant — an entry can be
    // re-recorded, but a wrongly-permitted edit rewrites history.
    fun canModify(ctx: Context, key: String) =
        get(ctx, key)?.modify == true && !isRestricted(ctx)

    fun canDelete(ctx: Context, key: String) =
        get(ctx, key)?.delete == true && !isRestricted(ctx)

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

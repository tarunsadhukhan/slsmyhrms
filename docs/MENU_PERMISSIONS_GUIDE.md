# Dynamic Menu & Role/User Permission System — MyHrms

This document explains the dynamic menu permission system that has been added to MyHrms. It covers the database schema, the backend endpoint, the Android `PermissionManager`, and step-by-step instructions for wiring it into the existing screens.

---

## 1. What got added

| Area | File | Purpose |
|---|---|---|
| DB | `database/01_menu_permissions_schema.sql` | Creates `roles`, `menus`, `role_menu_permissions`, `user_menu_permissions`, `user_roles`, plus the `v_user_effective_permissions` view. |
| DB | `database/02_menu_permissions_data.sql` | Seeds every menu/sub-menu that currently exists in the Dashboard, plus 5 default roles (Admin, Manager, Supervisor, Operator, Viewer). |
| Backend | `backend_examples/permissions_blueprint.py` | Flask blueprint with `GET /menu-permissions`, `GET /menus`, `POST /role-menu-permissions`, `POST /user-menu-permissions`. |
| Android | `app/.../permissions/MenuPermission.kt` | Retrofit data model. |
| Android | `app/.../permissions/PermissionManager.kt` | In-memory + SharedPreferences cache, with `canView/canAdd/canModify/canDelete/canPrint` helpers and a `applyToView(...)` UI helper. |
| Android | `ApiRoutes.kt`, `ApiService.kt` | Added the `MENU_PERMISSIONS` route and the Retrofit method. |

No existing screens were rewritten — the system is opt-in. Once you wire `PermissionManager.refresh(...)` into `LoginActivity` and call the helpers from each Activity, every Add / Edit / Delete / Print button is automatically governed by the DB.

---

## 2. The permission model

Six flags, stored as `TINYINT(1)` columns on the permission tables:

| Flag | Meaning in the UI |
|---|---|
| `can_view` | Shows the menu / card on the Dashboard. If `0`, the entire menu row is hidden. |
| `can_add` | Shows the `+` / "Add" button. |
| `can_modify` | Enables Edit / Save / row-tap to update. |
| `can_delete` | Shows the delete icon / swipe-to-delete. |
| `can_print` | Shows the Print / Export icon. |
| `can_all` | Shortcut. If set, every other flag is treated as `1`. |

Effective permission for a user is computed by the SQL view `v_user_effective_permissions`:

```
effective(flag) = user_menu_permissions.flag   IF a row exists for (user, menu)
                  ELSE  OR( role_menu_permissions.flag of every role the user has )
                  ELSE  0
```

So roles set the default and the per-user table can override any single menu (e.g. grant `can_delete` to one specific user for one specific screen).

---

## 3. How to give permissions

### 3.1 By role (the common case)

```sql
-- Give "Manager" full Add/Edit/Print on Departments but never Delete:
INSERT INTO role_menu_permissions
     (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT r.id, m.id, 1, 1, 1, 0, 1, 0
FROM   roles r, menus m
WHERE  r.role_name = 'Manager'
   AND m.menu_key = 'card_departments'
ON DUPLICATE KEY UPDATE
     can_view=VALUES(can_view), can_add=VALUES(can_add),
     can_modify=VALUES(can_modify), can_delete=VALUES(can_delete),
     can_print=VALUES(can_print), can_all=VALUES(can_all);
```

Then attach the role to a user:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT 5, id FROM roles WHERE role_name = 'Manager';
```

### 3.2 Per-user override

```sql
-- User 5 normally has Manager rights, but on Employees give him delete too:
INSERT INTO user_menu_permissions
     (user_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT 5, id, 1, 1, 1, 1, 1, 0
FROM   menus WHERE menu_key = 'card_employees'
ON DUPLICATE KEY UPDATE
     can_view=VALUES(can_view), can_add=VALUES(can_add),
     can_modify=VALUES(can_modify), can_delete=VALUES(can_delete),
     can_print=VALUES(can_print), can_all=VALUES(can_all);
```

### 3.3 From an admin screen

Hit the endpoints from the blueprint:

```
POST /role-menu-permissions
{ "role_id": 2, "menu_id": 14, "can_view": 1, "can_add": 1, "can_modify": 1,
  "can_delete": 0, "can_print": 1, "can_all": 0 }

POST /user-menu-permissions
{ "user_id": 5, "menu_id": 14, "can_view": 1, ... }
```

Both are upserts (`ON DUPLICATE KEY UPDATE`), so you can call them repeatedly.

---

## 4. Wiring it into the mobile app

### 4.1 In `LoginActivity` — fetch the permissions right after login

Inside `performLogin(...)`, in the success branch, **before** starting `DashboardActivity`:

```kotlin
loginResponse.user?.id?.let { userId ->
    getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        .edit().putInt("user_id", userId).apply()

    // NEW — load this user's menu permissions, then go to Dashboard
    com.example.slsHrms.permissions.PermissionManager
        .refresh(this@LoginActivity, userId) { _ ->
            val i = Intent(this@LoginActivity, DashboardActivity::class.java)
            i.putExtra("USER_NAME", loginResponse.user?.fullName ?: username)
            startActivity(i)
            finish()
        }
}
```

Tip: if you would rather keep the existing navigation flow, call `PermissionManager.refresh(this, userId)` without a callback and continue starting the Dashboard immediately — the permissions will arrive a few hundred ms later and the cache will be hydrated for the next screen.

### 4.2 In `DashboardActivity` — hide menus the user cannot see

At the end of `onCreate(...)`:

```kotlin
applyDashboardPermissions()
```

Add this method:

```kotlin
private fun applyDashboardPermissions() {
    val pm = com.example.slsHrms.permissions.PermissionManager

    // Master cards
    pm.applyVisibility(this, "card_departments", binding.cardDepartments)
    pm.applyVisibility(this, "card_designations", binding.cardDesignations)
    pm.applyVisibility(this, "card_employees", binding.cardEmployees)
    pm.applyVisibility(this, "card_present", binding.cardPresent)
    pm.applyVisibility(this, "card_absent", binding.cardAbsent)

    // Attendance group
    pm.applyVisibility(this, "grp_attendance", binding.menuAttendance)
    pm.applyVisibility(this, "menu_attendance_dashboard", binding.menuAttendanceDashboard)
    pm.applyVisibility(this, "menu_onboarding", binding.menuOnBoarding)
    pm.applyVisibility(this, "menu_attendance_entry", binding.menuAttendanceEntry)
    pm.applyVisibility(this, "menu_attendance_reports", binding.menuAttendanceReports)
    pm.applyVisibility(this, "grp_other_entries", binding.headerOtherEntries)
    pm.applyVisibility(this, "menu_leave_entries", binding.menuLeaveEntries)

    // Production group
    pm.applyVisibility(this, "grp_production", binding.menuProduction)
    pm.applyVisibility(this, "grp_jute", binding.headerJute)
    pm.applyVisibility(this, "menu_jute_received", binding.menuJuteReceived)
    pm.applyVisibility(this, "menu_assorting_entry", binding.menuAssortingEntry)
    pm.applyVisibility(this, "grp_spreader_entry", binding.headerSpreaderEntry)
    pm.applyVisibility(this, "menu_production_entry", binding.menuProductionEntry)
    pm.applyVisibility(this, "menu_issue_entry", binding.menuIssueEntry)
    pm.applyVisibility(this, "menu_drawing_meter_entry", binding.menuDrawingMeterEntry)
    pm.applyVisibility(this, "menu_spinning_doff_entry", binding.menuSpinningDoffEntry)
    pm.applyVisibility(this, "grp_doff_entry", binding.headerDoffEntry)
    pm.applyVisibility(this, "menu_spellwise_frame_entry", binding.menuSpellWiseFrameEntry)
    pm.applyVisibility(this, "menu_spg_doff_entry", binding.menuSpgDoffEntry)
    pm.applyVisibility(this, "menu_spg_doff_entry1", binding.menuSpgDoffEntry1)
    pm.applyVisibility(this, "menu_winding_entry", binding.menuWindingEntry)
    pm.applyVisibility(this, "menu_weaving_entry", binding.menuWeavingEntry)
    pm.applyVisibility(this, "grp_finishing_entry", binding.headerFinishingEntry)
    pm.applyVisibility(this, "menu_other_entries", binding.menuOtherEntries)
    pm.applyVisibility(this, "menu_bales_production_entry", binding.menuBalesProductionEntry)
    pm.applyVisibility(this, "menu_bales_issue_entry", binding.menuBalesIssueEntry)
    pm.applyVisibility(this, "grp_stocks", binding.headerStocks)
    pm.applyVisibility(this, "menu_roll_stock", binding.menuRollStock)
    pm.applyVisibility(this, "menu_weight_entry", binding.menuWeightEntry)
}
```

That single method hides every menu and sub-menu the user does not have `can_view = 1` on. Group headers (`grp_*`) are hidden too, so the whole expandable section disappears when all leaves are denied.

### 4.3 In each Master / Entry activity — hide Add / Edit / Delete / Print

Example for `DepartmentMasterActivity`:

```kotlin
private val MENU_KEY = "card_departments"

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityDepartmentMasterBinding.inflate(layoutInflater)
    setContentView(binding.root)
    ...
    applyPermissions()
}

private fun applyPermissions() {
    val pm = com.example.slsHrms.permissions.PermissionManager
    pm.applyToView(this, MENU_KEY, PermissionAction.ADD, binding.btnAddDepartment)
    pm.applyToView(this, MENU_KEY, PermissionAction.PRINT, binding.btnPrint)
    // For edit/delete the buttons usually live inside the RecyclerView row,
    // so pass the flags into the adapter:
    departmentAdapter.canModify = pm.canModify(this, MENU_KEY)
    departmentAdapter.canDelete = pm.canDelete(this, MENU_KEY)
}
```

In the adapter:

```kotlin
class DepartmentAdapter(...) : RecyclerView.Adapter<...>() {
    var canModify: Boolean = false
    var canDelete: Boolean = false

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        ...
        h.btnEdit  .visibility = if (canModify) View.VISIBLE else View.GONE
        h.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
    }
}
```

The same 4 lines of `applyToView(...)` work for every other master/entry screen — just change the `MENU_KEY` constant to the matching `menu_key`.

### 4.4 Re-fetching when the admin updates permissions

`PermissionManager.refresh(...)` is safe to call any time, e.g. from a "Sync menus" button in Settings, or automatically from the Dashboard's `onResume()`:

```kotlin
override fun onResume() {
    super.onResume()
    val userId = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)
    if (userId > 0) {
        PermissionManager.refresh(this, userId) { ok ->
            if (ok) applyDashboardPermissions()
        }
    }
}
```

### 4.5 Clearing on logout

In `setupLogout()`:

```kotlin
binding.btnLogout.setOnClickListener {
    com.example.slsHrms.permissions.PermissionManager.clear(this)
    val intent = Intent(this, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

---

## 5. The complete list of `menu_key` values

These match the IDs already in `res/layout/activity_dashboard.xml`, so the Android wiring above is one-line-per-view.

```
card_departments
card_designations
card_employees
card_present
card_absent

grp_attendance
  menu_attendance_dashboard
  menu_onboarding
  menu_attendance_entry
  menu_attendance_reports
  grp_other_entries
    menu_leave_entries

grp_production
  grp_jute
    menu_jute_received
    menu_assorting_entry
  grp_spreader_entry
    menu_production_entry
    menu_issue_entry
  menu_drawing_meter_entry
  menu_spinning_doff_entry
  grp_doff_entry
    menu_spellwise_frame_entry
    menu_spg_doff_entry
    menu_spg_doff_entry1
  menu_winding_entry
  menu_weaving_entry
  grp_finishing_entry
    menu_other_entries
    menu_bales_production_entry
    menu_bales_issue_entry
  grp_stocks
    menu_roll_stock
  menu_weight_entry
```

---

## 6. Quick install checklist

1. **Database**
   - Run `database/01_menu_permissions_schema.sql`
   - Run `database/02_menu_permissions_data.sql`
   - Assign your real users:
     `INSERT INTO user_roles (user_id, role_id) SELECT <userId>, id FROM roles WHERE role_name='Admin';`
2. **Backend**
   - Drop `backend_examples/permissions_blueprint.py` next to your existing blueprints.
   - Register it: `app.register_blueprint(permissions_bp)`.
3. **Android** (Kotlin)
   - The new `permissions/` package and the `ApiRoutes.kt` / `ApiService.kt` additions are already in place.
   - In `LoginActivity` add the `PermissionManager.refresh(...)` call after a successful login.
   - In `DashboardActivity` add the `applyDashboardPermissions()` method shown in §4.2.
   - In each Master / Entry Activity add the 4-line `applyPermissions()` block from §4.3.
4. **Test**
   - Log in as a Manager — Delete buttons should disappear everywhere.
   - Log in as a Viewer — every Add / Edit / Delete disappears, but Print stays.
   - Override one menu in `user_menu_permissions` and re-login to verify the override wins.

package com.example.slsHrms.api

// Keep all endpoint paths in one place for easy backend route updates.
private const val MASTERS_API_LOCATION = "masters"

object ApiRoutes {
    const val LOGIN = "login"

    const val GET_COMPANY = "$MASTERS_API_LOCATION/get_company"
    const val GET_BRANCH = "$MASTERS_API_LOCATION/get_branch"

    const val DEPARTMENTS = "departments"
    const val GET_DEPARTMENT = "departments"   // alias – same backend route
    const val DESIGNATIONS = "designations"
    const val SHIFTS = "shifts"
    const val OCCUPATIONS = "occupations"

    const val ATTENDANCE = "attendance"
    const val ATTENDANCE_LEAVE_STATUS = "attendance_leave_status"
    const val MARK_ATTENDANCE = "mark-attendance"
    const val CHECK_FACE = "check-face"
    const val EMPLOYEE_BY_CODE = "employee/{emp_code}"

    const val EMPLOYEES = "employees"
    const val EMPLOYEE_SEARCH = "employees/search"
    const val REGISTER = "register"

    const val DASHBOARD_STATS = "dashboard-stats"
    const val DASHBOARD_CARD_TREND = "dashboard/card-trend"
    const val ATTENDANCE_DASHBOARD = "attendance-dashboard"
    const val ATTENDANCE_REPORT = "attendance-report"
    const val EMP_WISE_ATTENDANCE = "emp-wise-attendance"

    const val STATUS_MST             = "status-mst"
    const val LEAVE_TYPES            = "leave-types"
    const val LEAVE_TRANSACTIONS     = "leave-transactions"
    const val LEAVE_TRANSACTION_DETAIL = "leave-transactions/{id}"

    // ── Spinning Doff ─────────────────────────────────────────────
    const val SPELLS                  = "spells"
    const val DOFF_MACHINES           = "doff/machines"
    const val DOFF_QUALITIES          = "doff/qualities"
    const val DOFF_TROLLIES           = "doff/trollies"
    const val DOFF_TRANSACTIONS       = "doff-transactions"
    const val DOFF_TRANSACTION_DETAIL = "doff-transactions/{id}"
    const val DOFF_LAST_BY_MACHINE    = "doff/last-by-machine"
    const val DOFF_LAST_EMP           = "doff/last-emp"
    const val DOFF_VALIDATE_MACHINE   = "doff/validate-machine"
    const val DOFF_VALIDATE_TROLLY    = "doff/validate-trolly"
    const val DOFF_SUMMARY            = "doff/summary"
    const val DOFF_FRAME_MACHINES             = "doff/frame-machines"
    const val DOFF_FRAME_ENTRIES              = "doff/frame-entries"
    const val DOFF_FRAME_MACHINE_DEFAULTS     = "doff/frame-machine-defaults"
    const val DOFF_WINDING_FRAME_MACHINES     = "doff/winding-frame-machines"
    const val DOFF_WINDING_FRAME_ENTRIES      = "doff/winding-frame-entries"
    const val DOFF_WINDING_FRAME_DEFAULTS     = "doff/winding-frame-machine-defaults"
    const val DOFF_WINDING_QUALITIES          = "doff/winding-qualities"
    const val DOFF_LAST_QUALITY_BY_MC         = "doff/last-quality-by-mc"
    const val DOFF_WINDING_EB_LOOKUP          = "doff/winding-eb-lookup"
    const val DOFF_WINDING_ENTRIES_LIST       = "doff/winding-entries"
    const val DOFF_WINDING_ENTRY              = "doff/winding-entry"
    const val DOFF_WINDING_ENTRY_DETAIL       = "doff/winding-entry/{id}"

    // ── Continuous Winding Entry (date + quality + prod_kgs) ─────────────────
    const val DOFF_CONT_WINDING_ENTRIES       = "doff/cont-winding-entries"
    const val DOFF_CONT_WINDING_ENTRY         = "doff/cont-winding-entry"
    const val DOFF_CONT_WINDING_ENTRY_DETAIL  = "doff/cont-winding-entry/{id}"

    // ── Spg Doff Entry (1) ──────────────────────────────────────────────────
    const val DOFF_SPG1_MECH_CODES = "doff/spg1-mech-codes"
    const val DOFF_SPG1_SUMMARY    = "doff/spg1-summary"
    const val DOFF_SPG1_QUALITY_SHIFT_REPORT = "doff/spg1-quality-shift-report"

    // ── Winding Entry (2) — S/C + trolly + weight ────────────────────────────
    const val DOFF_WE2_EMP_LOOKUP = "doff/winding-entry-2-emp-lookup"
    const val DOFF_WE2_EMPLOYEES  = "doff/winding-entry-2-employees"
    const val DOFF_WE2            = "doff/winding-entry-2"
    const val DOFF_WE2_TROLLY     = "doff/winding-entry-2-trolly"
    const val DOFF_WE2_QUALITY_SHIFT_REPORT = "doff/winding-entry-2-quality-shift-report"
    const val DOFF_WE2_DETAIL_BY_EMP = "doff/winding-entry-2-detail-by-emp"
    const val DOFF_WE2_SUMMARY    = "doff/winding-entry-2-summary"
    const val DOFF_WE2_DETAIL     = "doff/winding-entry-2/{id}"

    // ── Spg Running Hours ────────────────────────────────────────────────────
    const val SPG_RUNNING_HOURS       = "doff/spg-running-hours"
    const val SPG_RUNNING_HOURS_ID    = "doff/spg-running-hours/{id}"

    // ── Weight Entry ─────────────────────────────────────────────────────────
    const val WEIGHT_TRANSACTIONS = "weight-transactions"
    const val WEIGHT_TRANSACTION_DETAIL = "weight-transactions/{id}"

    // ── Spreader Production / Issue Entry ────────────────────────────────────
    const val SPREADER_MACHINES        = "spreader/machines"
    const val SPREADER_QUALITIES       = "spreader/qualities"
    const val SPREADER_SPRD_QUALITIES  = "spreader/sprd-qualities"
    const val SPREADER_PROD_ENTRY      = "spreader/prod-entry"
    const val SPREADER_PROD_ENTRY_ID   = "spreader/prod-entry/{id}"
    const val SPREADER_ISSUE_ENTRY     = "spreader/issue-entry"
    const val SPREADER_ISSUE_ENTRY_ID  = "spreader/issue-entry/{id}"
    const val SPREADER_QUALITY_STOCK       = "spreader/quality-stock"
    const val SPREADER_QUALITY_STOCK_LIST  = "spreader/quality-stock-list"

    // ── Bales Production → Roll Stock (src/balesproduction blueprint) ────────
    const val BALESPROD_MC_CODES       = "balesproduction/mc-codes"
    const val BALESPROD_ROLL_STOCK     = "balesproduction/roll-stock"
    const val BALESPROD_ROLL_STOCK_ID  = "balesproduction/roll-stock/{id}"

    // ── Bales Production / Issue Entry ───────────────────────────────────────
    const val BALESPROD_FNG_QUALITIES  = "balesproduction/finishing-qualities"
    const val BALESPROD_CUSTOMERS      = "balesproduction/customers"
    const val BALESPROD_PROD_ENTRY     = "balesproduction/prod-entry"
    const val BALESPROD_PROD_ENTRY_ID  = "balesproduction/prod-entry/{id}"
    const val BALESPROD_ISSUE_ENTRY    = "balesproduction/issue-entry"
    const val BALESPROD_ISSUE_ENTRY_ID = "balesproduction/issue-entry/{id}"
    const val BALESPROD_STOCK          = "balesproduction/stock"
    const val BALESPROD_STOCK_PAIR     = "balesproduction/stock-pair"

    // ── Assorting Entry (src/assorting blueprint) ────────────────────────────
    const val ASSORTING_SHEDS           = "assorting/sheds"
    const val ASSORTING_MACHINES        = "assorting/machines"
    const val ASSORTING_QUALITIES       = "assorting/qualities"
    const val ASSORTING_SELECTORS       = "assorting/selectors"
    const val ASSORTING_VALIDATE_TROLLY = "assorting/validate-trolly"
    const val ASSORTING_ENTRY           = "assorting/entry"
    const val ASSORTING_ENTRY_ID        = "assorting/entry/{id}"

    // ── Jute Received (src/jute_received blueprint) ──────────────────────────
    const val JUTE_RECEIVED_QUALITIES = "jute-received/qualities"
    const val JUTE_RECEIVED_ENTRY     = "jute-received/entry"
    const val JUTE_RECEIVED_ENTRY_ID  = "jute-received/entry/{id}"

    // ── Finishing → Other Entries ─────────────────────────────────────────────
    const val FINISHING_ENTRY     = "otherentries/entry"
    const val FINISHING_ENTRY_ID  = "otherentries/entry/{id}"

    // ── Drawing Meter Entry ──────────────────────────────────────────────────
    const val DRAWING_SHEDS          = "drawing/sheds"
    const val DRAWING_MACHINES       = "drawing/machines"
    const val DRAWING_OPENING_METER  = "drawing/opening-meter"
    const val DRAWING_ENTRY          = "drawing/entry"
    const val DRAWING_SUMMARY        = "drawing/summary"
    const val DRAWING_SPELLS         = "drawing/spells"

    // ── Machine Assignment ────────────────────────────────────────────────────
    const val MACHINES = "machines"

    // ── Onboarding (Face Registration) ────────────────────────────────────────
    const val ONBOARDING_EMPLOYEE   = "onboarding/employee/{emp_code}"
    const val ONBOARDING_REGISTER   = "onboarding/register-face"
    const val ONBOARDING_REGISTER_FACE = "onboarding/register-face"  // Alias

    // ── Dynamic Menu & Permissions ────────────────────────────────────────────
    const val MENU_PERMISSIONS         = "menu-permissions"
    const val MENUS_MASTER             = "menus"
    const val ROLE_MENU_PERMISSIONS    = "role-menu-permissions"
    const val USER_MENU_PERMISSIONS    = "user-menu-permissions"
}

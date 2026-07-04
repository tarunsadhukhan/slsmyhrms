# Winding Entry — Implementation Plan

## Overview
On click **Winding Entry** in the dashboard menus, open a new `WindingEntryActivity` that mirrors the SPG Doff Entry (1) flow with these differences:

- Row 2 is an **employee lookup by emp_code** (instead of mech-posting-code buttons)
- employee name only (7 chars) will be fetch from the daily_doff_frames_winding ⨝ hrms_ed_official_details ⨝ hrms_ed_personal_details based on emp_code + branch_id
- and as same way as SPG Doff Entry (1) like button for each employee
- Row 3 has two full-width toggle buttons **(S)** and **(C)**: selecting **(C)** enables the Trolly No field; selecting **(S)** disables and clears it (tare = 0)
- Row 4: Gross Wt | Tare Wt | Net Wt | Save
- Saves to `daily_doff_frames_winding` with `spg_wdg = 'W'`
- Validation: employee name must be resolved, net wt must be positive; if (C) then trolly is required
- Summary shown below the entry form

---

## Screen Layout (4 rows)

### Row 1 — Date | Spell
- Date picker (calendar icon, shows dd-MM-yyyy, stores yyyy-MM-dd internally)
- Spell spinner (loads from `/spells?branch_id=`)

### Row 2 — Employee Buttons (like Mech Posting Code in SPG Doff Entry)
- On date/spell change → `GET /doff/winding-entry-2-employees?date=&spell_id=&branch_id=`
  - Reads `daily_doff_frames_winding` (spg_wdg='W') ⨝ `hrms_ed_official_details` ⨝ `hrms_ed_personal_details`
  - Returns `[{eb_id, emp_code, emp_name (SUBSTR first_name 1,7)}]`
- Each employee rendered as a `Button` (52dp × 36dp) labelled with **7-char name**
  - Default colour `#1565C0` (blue)
  - Selected colour `#2E7D32` (green) — only one active at a time
  - Clicking sets `resolvedEbId` and `resolvedEmpName`
- `TextView tvEmpStatus` shows "Loading…" / error / hidden when buttons are shown
- `LinearLayout llEmpButtons` inside `HorizontalScrollView` (scrollable when many employees)

### Row 3 — (S) / (C) buttons + Trolly No
- Two `Button`s with `weightSum=2`, each `layout_weight=1` (fit to width)
  - `btnTypeS` text `(S)`, default selected → background `#2E7D32` (green)
  - `btnTypeC` text `(C)`, unselected → background `#1565C0` (blue)
  - Toggle: selected button = green, other = blue
- `EditText etTrollyNo` (number, below the buttons)
  - **Disabled** (greyed) when `(S)` is selected
  - **Enabled** when `(C)` is selected
  - Debounced lookup 500 ms + on-focus-loss → calls `/doff/validate-trolly`
  - On success: fills `etTareWt` with `trolly_weight + bucket_weight`, shows green border + info text
  - When switching back to `(S)`: clear trolly no, reset tare to `0.000`, disable field

### Row 4 — Gross Wt | Tare Wt | Net Wt | Save
- `EditText etGrossWt` (decimal, editable) — text watcher recalcs net on change
- `EditText etTareWt` (decimal, **not** user-editable — auto-filled from trolly lookup or 0)
- `EditText etNetWt` (decimal, **not** user-editable — computed `gross - tare`, red when ≤ 0)
- `Button btnSave` (blue `#1565C0`)

### Summary Card
- Header row: `Code | Name | S/C | Trolly | Net Wt`
- `RecyclerView rvSummary` using `We2SummaryAdapter` / `item_we2_summary.xml`
- Long-press row → confirm-delete dialog → soft delete (sets `active=0`)
- Reloads on spell change, date change, after save, after delete
- Empty state: `tvSummaryEmpty` ("No records for selected date / spell")
- Loading state: `pbSummary` (ProgressBar)

---

## Validation Rules (client-side, mirrored server-side)

| Rule | Toast message |
|------|--------------|
| Spell must be selected | "Please select a spell" |
| `resolvedEbId > 0` (employee found) | "Please enter a valid Emp Code (Name not found)" |
| If `(C)` selected, trolly must be validated | "Please enter a valid Trolly No (required when C)" |
| `gross > 0` | "Please enter Gross Weight" |
| `net > 0` (strict positive) | "Net Weight must be positive" |

---

## Android Files

### New files
| File | Purpose |
|------|---------|
| `app/src/main/res/layout/activity_winding_entry.xml` | Entry + summary card layout |
| `app/src/main/res/layout/item_we2_summary.xml` | Summary RecyclerView row |
| `app/src/main/java/com/example/myhrms/WindingEntryActivity.kt` | Main activity |
| `app/src/main/java/com/example/myhrms/adapter/We2SummaryAdapter.kt` | RecyclerView adapter |

### Edited files
| File | Change |
|------|--------|
| `api/ApiRoutes.kt` | Added `DOFF_WE2_EMP_LOOKUP`, `DOFF_WE2`, `DOFF_WE2_DETAIL` |
| `api/ApiService.kt` | Added `we2EmpLookup`, `getWe2Summary`, `saveWe2`, `deleteWe2` |
| `api/DoffResponse.kt` | Added `We2EmpLookupResponse`, `We2SummaryRow`, `We2SummaryResponse`, `We2SaveRequest`, `We2SaveResponse` |
| `AndroidManifest.xml` | Registered `.WindingEntryActivity` |
| `DashboardActivity.kt` | `binding.menuWindingEntry` → launches `WindingEntryActivity` |
| `AttendanceDashboardActivity.kt` | `menuWindingEntry` → launches `WindingEntryActivity` |
| `ProductionDashboardActivity.kt` | `menuWindingEntry` → launches `WindingEntryActivity` |

### Intent extras passed on launch
```kotlin
intent.putExtra("CO_ID", selectedCompanyId)
intent.putExtra("BRANCH_ID", selectedBranchId)
```

---

## API Endpoints

### `GET /doff/winding-entry-2-emp-lookup`
**Query params:** `emp_code`, `branch_id`

**SQL:**
```sql
SELECT o.eb_id, o.emp_code,
       SUBSTR(COALESCE(p.first_name,''), 1, 6) AS emp_name
  FROM hrms_ed_official_details o
  LEFT JOIN hrms_ed_personal_details p ON p.eb_id = o.eb_id
 WHERE o.emp_code = %s
   AND (o.active IS NULL OR o.active = 1)
   [AND o.branch_id = %s]
 LIMIT 1
```

**Response:**
```json
{ "status": "success", "eb_id": 123, "emp_code": "1001", "emp_name": "Ramesh" }
```

---

### `GET /doff/winding-entry-2`
**Query params:** `date`, `spell_id`, `branch_id`

**SQL:**
```sql
SELECT w.daily_doff_frm_wdg_id AS id,
       w.eb_id, o.emp_code,
       SUBSTR(COALESCE(p.first_name,''), 1, 6) AS emp_name,
       w.sc_type, w.trolly_id, t.trolly_name,
       w.gross_weight, w.tare_weight, w.net_weight
  FROM daily_doff_frames_winding w
  LEFT JOIN hrms_ed_official_details o ON o.eb_id = w.eb_id
  LEFT JOIN hrms_ed_personal_details p ON p.eb_id = w.eb_id
  LEFT JOIN trolly_mst t ON t.trolly_id = w.trolly_id
 WHERE w.tran_date = %s
   AND (w.spell_id = %s OR w.spell = %s)
   AND w.branch_id = %s
   AND w.spg_wdg = 'W'
   AND w.net_weight IS NOT NULL
   AND (w.active IS NULL OR w.active = 1)
 ORDER BY w.daily_doff_frm_wdg_id DESC
```

**Response:** `{ "status": "success", "summary": [ ...We2SummaryRow... ] }`

---

### `POST /doff/winding-entry-2`
**Body:**
```json
{
  "date": "2026-05-04",
  "spell_id": 1,
  "branch_id": 1,
  "eb_id": 123,
  "sc_type": "S",
  "trolly_id": null,
  "gross_weight": 90.0,
  "tare_weight": 0.0,
  "net_weight": 90.0,
  "user_id": 5
}
```

**Server validations:**
- `eb_id` required
- `sc_type` ∈ `{'S', 'C'}`
- if `sc_type = 'C'` → `trolly_id` required
- `net_weight > 0`

**SQL:**
```sql
INSERT INTO daily_doff_frames_winding
    (tran_date, spell, spell_id, mc_eb_id, eb_id, sc_type,
     trolly_id, gross_weight, tare_weight, net_weight,
     spg_wdg, branch_id, active, user_id, created_at)
VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'W',%s,1,%s,NOW())
```

**Response:** `{ "status": "success", "message": "Saved", "id": 456 }`

---

### `DELETE /doff/winding-entry-2/<id>`
Soft-delete: `UPDATE daily_doff_frames_winding SET active=0 WHERE daily_doff_frm_wdg_id=%s AND spg_wdg='W'`

---

## DB Migration (lazy, auto-runs on first request)
Function `_ensure_we2_schema()` in `doff.py` — adds columns to `daily_doff_frames_winding` if missing:

| Column | Type |
|--------|------|
| `eb_id` | `INT NULL` |
| `sc_type` | `CHAR(1) NULL` |
| `trolly_id` | `INT NULL` |
| `gross_weight` | `DECIMAL(12,3) NULL` |
| `tare_weight` | `DECIMAL(12,3) NULL` |
| `net_weight` | `DECIMAL(12,3) NULL` |
| `user_id` | `INT NULL` |
| `created_at` | `DATETIME NULL` |

---

## Deployment Checklist
- [ ] `.\gradlew assembleDebug` → install `app-debug.apk`
- [ ] Restart Flask backend (`e:\sjm\AttendanceSystem`) so new routes register
- [ ] First API call triggers lazy migration (columns added automatically)
- [ ] Test `(S)` flow: Trolly No disabled, tare = 0, net = gross
- [ ] Test `(C)` flow: Trolly No enabled, auto-fills tare, net = gross − tare
- [ ] Test employee not found → red border, save blocked
- [ ] Test net ≤ 0 → save blocked
- [ ] Test summary loads, long-press delete works

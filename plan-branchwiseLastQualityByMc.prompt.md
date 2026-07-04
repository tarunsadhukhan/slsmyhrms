# Plan: Branch-wise Quality Defaults from Last Doff (Spell-wise Frame Entry)

Add a backend endpoint that returns the last `quality_id` per machine from `daily_doff_tbl` filtered by `branch_id`, then call it from `SpellWiseFrameEntryActivity` so each row's Quality spinner pre-selects the most recently used quality. All three loads (machines, qualities, last-quality) on the screen are branch-scoped.

## Existing context discovered
- Frame screen: `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\SpellWiseFrameEntryActivity.kt` hosts `FrameEntryAdapter` and gets `branchId` from `intent.getIntExtra("BRANCH_ID", 0)`.
- Adapter already exposes `setQualities(...)` and `setQualityDefaults(map, replaceAll)` in `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\adapter\FrameEntryAdapter.kt`.
- Retrofit layer: `ApiService.kt`, routes in `ApiRoutes.kt`, DTOs in `DoffResponse.kt`.
- Backend doff blueprint: `e:\sjm\attendancesystem\src\doff\doff.py` (registered as `doff_bp` in `src/__init__.py`). DB helper: `from db import get_db` (mysql-connector style, `cursor(dictionary=True)`).
- Tables:
  - `daily_doff_tbl(daily_doff_tbl_id, doff_date, spell, mc_id, quality_id, branch_id, active, updated_date_time, ...)`
  - `spinning_quality_mst(spg_quality_mst_id, spg_quality, branch_id, ...)`
  - `machine_mst(machine_id, mech_code as mc_no, dept_id)` joined to `dept_mst.branch_id`.
- An older endpoint `/doff/frame-machine-defaults` already returns last quality per machine but mixes frame-entries and doff rows; the new endpoint will be the single, doff-only source of truth requested.

## Steps
1. **Backend – add route** `GET /api/spinning/last-quality-by-mc` in `src/doff/doff.py` (under `doff_bp`). Validate `branch_id` (400 if missing). Query `daily_doff_tbl` joined to `machine_mst`+`dept_mst` filtered by `dm.branch_id=%s` and `d.active=1`; for each `d.mc_id` pick the row with `MAX(d.daily_doff_tbl_id)` (correlated subquery or `INNER JOIN (SELECT mc_id, MAX(daily_doff_tbl_id) max_id … GROUP BY mc_id) lf`). Return `{"success": true, "data": {"<mc_id>": <quality_id>, ...}}` (string keys, ints as values). Skip rows where `quality_id IS NULL`.
2. **Backend – verify branch filter on `/doff/qualities`** in same file: it already accepts `branch_id` and filters `branch_id IS NULL OR branch_id = %s`; confirm UI passes it (it does via `getDoffQualities(branchId)`). No code change unless we want strict equality.
3. **Android – Routes & API**: add `LAST_QUALITY_BY_MC = "api/spinning/last-quality-by-mc"` to `ApiRoutes`; add `getLastQualityByMc(@Query("branch_id") branchId: Int): Call<LastQualityByMcResponse>` to `ApiService`. Add DTO `LastQualityByMcResponse(success: Boolean, data: Map<String, Int>?)` in `DoffResponse.kt`.
4. **Android – wire into Activity**: in `SpellWiseFrameEntryActivity.onCreate` load chain, add `loadLastQualityByMc { … }` that calls the new endpoint and on success does `adapter.setQualityDefaults(map.mapKeys { it.key.toInt() }, replaceAll = true)` before `loadSpells { reloadEntries() }`. Keep existing `reloadEntries()` behavior so per-date saved qualities still overlay (`replaceAll = false`).
5. **Order of execution preserved**: `loadMachines → loadQualities → loadLastQualityByMc → loadSpells → reloadEntries`. If any step fails, fall through with `onDone()` so the screen still renders.
6. **Manual tests**:
   - Backend: `curl "http://<host>:5051/api/spinning/last-quality-by-mc?branch_id=1"` → expect `{"success":true,"data":{"12":3,"15":7,...}}`; missing param → 400.
   - SQL spot-check: `SELECT mc_id, quality_id, daily_doff_tbl_id FROM daily_doff_tbl WHERE branch_id=1 AND mc_id=12 ORDER BY daily_doff_tbl_id DESC LIMIT 1;` matches API.
   - App: open Spinning Frame Entry on a branch with prior doff history → each row's spinner shows the last-used quality; toggling rows preserves it; saving and reopening still shows the latest (per-date overlay still works).

## Further considerations
1. Endpoint naming: the existing `doff/frame-machine-defaults` overlaps. Add new endpoint as spec'd and leave old; retire old in a later pass.
2. Tie-breaker: `MAX(daily_doff_tbl_id)` (auto-increment) is sufficient. If id is not monotonic, switch to `ROW_NUMBER() OVER (PARTITION BY mc_id ORDER BY updated_date_time DESC, daily_doff_tbl_id DESC)`.
3. Quality master scope: current SQL returns qualities with `branch_id IS NULL` too. Confirm if unbranded global qualities should appear or strict `branch_id = %s` only.
4. `branch_id` source on Android: relies on `intent.getIntExtra("BRANCH_ID", 0)`. Fallback to `getSharedPreferences("LoginPrefs",…).getInt("branch_id", 0)` if intent is 0.


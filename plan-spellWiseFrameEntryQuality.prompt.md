# Plan: Add Per-Frame Quality Selection to Spell-wise Frame Entry

Add a branch-wise **Quality** dropdown to each frame row in the Spell-wise Frame Entry screen. The default quality per machine will be auto-filled from that machine's most recent frame-entry record. On Save, each selected machine's `mc_id` is sent together with its chosen `quality_id`.

## Backend changes (`e:\sjm\attendancesystem`)
1. **Schema:** Add `quality_id INT` column to the spell-wise frame-entries table (the table backing `doff/frame-entries`); index on `(mc_id, id DESC)` for fast "last record" lookup.
2. **`GET doff/frame-entries`** (date, spell_id, branch_id): return `entries: [{mc_id, quality_id}]` instead of (or in addition to) the flat `mc_ids` list, so existing rows restore both the checkbox state and the previously saved quality.
3. **New `GET doff/frame-machine-defaults?branch_id=`**: for every active frame machine in the branch, return its last-saved `quality_id` from the frame-entries table — `defaults: [{mc_id, quality_id}]`. Used to pre-fill quality when no entry exists yet for the chosen date+spell.
4. **`POST doff/frame-entries`**: change payload to accept `entries: [{mc_id, quality_id}]`; persist each row with its quality. Reuse existing `doff/qualities?branch_id=` for the dropdown source (already implemented).

## Android changes
1. **Models** in [DoffResponse.kt](app/src/main/java/com/example/slsHrms/api/DoffResponse.kt): add `FrameEntry(mc_id, quality_id)`, extend `FrameEntriesResponse` with `entries: List<FrameEntry>?`, add `FrameMachineDefaultsResponse(defaults: List<FrameEntry>?)`, change `FrameEntriesSaveRequest.mcIds` → `entries: List<FrameEntry>`.
2. **API surface** in [ApiRoutes.kt](app/src/main/java/com/example/slsHrms/api/ApiRoutes.kt) and [ApiService.kt](app/src/main/java/com/example/slsHrms/api/ApiService.kt): add `DOFF_FRAME_MACHINE_DEFAULTS = "doff/frame-machine-defaults"` and `getFrameMachineDefaults(branchId)`.
3. **Row layout** [item_frame_entry.xml](app/src/main/res/layout/item_frame_entry.xml): add a compact `Spinner` (id `spQuality`) at the right end of the row using `bg_input_rounded_light`, weighted so name shrinks.
4. **Adapter** [FrameEntryAdapter.kt](app/src/main/java/com/example/slsHrms/adapter/FrameEntryAdapter.kt): accept `qualities: List<SpinningQuality>` and a `qualityByMcId: MutableMap<Int,Int>`; bind spinner per row using a shared `ArrayAdapter<SpinningQuality>`, restore selection from the map, write back on `onItemSelected`; expose `setQualityDefaults(map)` and `selectedEntries(): List<FrameEntry>` (only for checked machines, falling back to first quality when none chosen).
5. **Activity** [SpellWiseFrameEntryActivity.kt](app/src/main/java/com/example/slsHrms/SpellWiseFrameEntryActivity.kt): in `loadMachines` chain also call `getDoffQualities(branchId)` and `getFrameMachineDefaults(branchId)`; pass qualities into the adapter and seed defaults; in `reloadEntries` parse new `entries` and overlay both checkbox set and quality map; in `saveEntries` build `FrameEntriesSaveRequest(entries = adapter.selectedEntries())` and validate each selected row has a quality.

## Further Considerations
1. UI placement of quality — Option A: per-row spinner (matches "for each mcno"); Option B: single header spinner applied to all selected frames; Option C: both (header sets default, per-row overrides). Recommend **A**.
2. Behavior when a machine has no prior record — Option A: leave first item selected; Option B: show a "-- select --" hint and block save until chosen. Recommend **A** for speed.
3. Should the per-row quality persist independently of the checkbox (so unchecking doesn't lose the choice)? Recommend **yes**, keep map intact across toggles.


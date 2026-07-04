# Machine Name Display Fix

## Problem
Machine names were showing as "No name" in the machine selection dialog because:
1. Backend was returning field names `machine_id` and `machine_name` instead of `id` and `name`
2. The `mech_code` field was missing from the response
3. The frontend's `getDisplayName()` function expected both code and name to be separate

## Solution

### 1. Backend Fix (app.py)
Updated the `/machines` endpoint to explicitly format the response with correct field mapping:

**Before:**
```python
query = """
    SELECT mm.machine_id AS id, mm.machine_name AS name, ...
"""
machines = cursor.fetchall()
return jsonify({'status': 'success', 'data': machines, 'total': len(machines)})
```

**After:**
```python
query = """
    SELECT mm.machine_id, mm.machine_name, mm.mach_code, mm.mech_shr_code, ...
"""
raw_machines = cursor.fetchall()

# Format response to match frontend expectations
machines = []
for m in raw_machines:
    machines.append({
        'id': m['machine_id'],
        'name': m['machine_name'],
        'mech_code': m['mach_code'],
        'machine_no': m['mech_shr_code']
    })
```

### 2. Frontend Fix (MachineResponse.kt)
Updated `getDisplayName()` to prioritize the `machine_name` field since it already contains the full display text (e.g., "H06 HESS HELPER LINE NO 6"):

**Before:**
```kotlin
return when {
    code.isNotEmpty() && machineName.isNotEmpty() -> "$code ($machineName)"
    code.isNotEmpty() -> code
    machineName.isNotEmpty() -> machineName
    else -> "No name"
}
```

**After:**
```kotlin
return when {
    // Machine name contains full display text (code + name)
    machineName.isNotEmpty() -> machineName
    // Fallback to just code
    code.isNotEmpty() -> code
    else -> "No name"
}
```

## Expected Result

### Before:
```
☐ No name
☐ No name
☐ No name
```

### After:
```
☐ H06 HESS HELPER LINE NO 6
☐ H07 HESS HELPER LINE NO 7
☐ H08 HESS HELPER LINE NO 8
```

## Sample Data Structure

**Backend Response:**
```json
{
  "status": "success",
  "data": [
    {
      "id": 1486,
      "name": "H06 HESS HELPER LINE NO 6",
      "mech_code": "H06",
      "machine_no": "1486"
    },
    {
      "id": 1487,
      "name": "H07 HESS HELPER LINE NO 7",
      "mech_code": "H07",
      "machine_no": "1487"
    }
  ],
  "total": 2
}
```

**Frontend Mapping:**
- `id` → Machine ID
- `name` → Full display name (shown in dialog)
- `mech_code` → Machine code (for sorting/fallback)
- `machine_no` → Machine short code

## Testing Steps

1. **Restart Backend Server:**
   ```bash
   cd E:\sjm\MyHrms
   python app.py
   ```

2. **Rebuild Android App:**
   ```bash
   cd E:\sjm\MyHrms
   .\gradlew assembleDebug
   ```

3. **Install APK:**
   - Find APK at: `E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk`
   - Install on device: `adb install -r app-debug.apk`

4. **Test Machine Selection:**
   - Open app
   - Go to Mark Attendance
   - Select a designation/occupation
   - Tap "Machine Numbers" field
   - Verify machine names show correctly (e.g., "H06 HESS HELPER LINE NO 6")

## Files Modified

1. `E:\sjm\MyHrms\app.py` - Lines 1734-1755 (machines endpoint)
2. `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\MachineResponse.kt` - Lines 15-28 (getDisplayName function)

---

**Date:** April 23, 2026  
**Status:** ✅ Fixed


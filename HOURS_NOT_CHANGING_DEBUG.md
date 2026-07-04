# Hours Field Not Changing on Spell Change - Debugging Guide

## Issue
Hours field doesn't update when changing spell selection in Drawing Meter Entry.

## Root Cause Analysis

### Most Likely Causes:

1. **Backend Not Deployed (90% likely)**
   - Updated code is in `E:\sjm\MyHrms\app.py`
   - But server runs from `e:\sjm\attendancesystem\app.py`
   - If not deployed, backend returns wrong format

2. **Backend Not Restarted (80% likely)**
   - Even if deployed, Flask must be restarted
   - Old code still in memory until restart

3. **Backend Missing working_hours (50% likely)**
   - Backend doesn't return working_hours field
   - App receives empty/null values

4. **Database Missing Data (30% likely)**
   - spell_mst table has NULL working_hours
   - Query returns default 8.0 for all

## Diagnostic Steps

### Step 1: Test Backend API

Open browser: `http://localhost:5051/shifts`

**Expected Response:**
```json
{
  "status": "success",
  "shifts": [
    {
      "id": 91,
      "name": "A1",
      "start_time": "11:00:00",
      "end_time": "06:00:00",
      "working_hours": 5.0
    }
  ]
}
```

**If you see:**
- `"data"` instead of `"shifts"` → Backend not deployed/restarted
- No `"working_hours"` field → Backend code is old
- `working_hours: 8.0` for all → Database issue

### Step 2: Check Android LogCat

Connect device and run:
```
adb logcat -s DrawingMeterEntry:D
```

Look for:
- API response when loading spells
- working_hours values in the response
- Any errors when setting hours field

### Step 3: Check Database

Run in MySQL:
```sql
SELECT spell_id, spell_name, working_hours 
FROM spell_mst 
ORDER BY spell_name;
```

**Expected:**
```
| spell_id | spell_name | working_hours |
|----------|------------|---------------|
| 91       | A1         | 5.00          |
| 92       | A2         | 3.00          |
| 95       | C          | 8.00          |
```

**If NULL:**
```sql
UPDATE spell_mst SET working_hours = 8.0 WHERE working_hours IS NULL;
-- Or set specific values:
UPDATE spell_mst SET working_hours = 5.0 WHERE spell_name = 'A1';
UPDATE spell_mst SET working_hours = 3.0 WHERE spell_name = 'A2';
UPDATE spell_mst SET working_hours = 8.0 WHERE spell_name = 'C';
```

## Solution Steps

### Quick Fix (Run this first):

1. **Deploy Backend:**
   ```cmd
   copy /Y "E:\sjm\MyHrms\app.py" "e:\sjm\attendancesystem\app.py"
   ```

2. **Restart Flask Server:**
   ```cmd
   cd e:\sjm\attendancesystem
   python app.py
   ```

3. **Test API:**
   ```
   http://localhost:5051/shifts
   ```

4. **Rebuild APK** (if backend works):
   ```cmd
   cd E:\sjm\MyHrms
   gradlew.bat assembleDebug
   ```

5. **Install and Test**

### Automated Fix:

**Run:** `DEPLOY_AND_TEST.bat`

This script:
- Deploys backend automatically
- Guides you through server restart
- Tests the API
- Optionally builds APK

## Verification Checklist

After deploying:

□ API returns JSON with "shifts" key
□ Each shift has "working_hours" field  
□ working_hours has different values (not all 8.0)
□ APK rebuilt and installed
□ Spell dropdown loads in app
□ Changing spell updates hours field
□ Hours persist after save

## Common Mistakes

❌ **Editing only E:\sjm\MyHrms\app.py**
   → Must deploy to e:\sjm\attendancesystem

❌ **Not restarting Flask server**
   → Changes don't take effect until restart

❌ **Testing old APK**
   → Must rebuild and reinstall after backend changes

❌ **Wrong backend location**
   → Server runs from e:\sjm\attendancesystem (NOT MyHrms)

## Testing Each Spell

After fixing, test with different spells:

1. Select Spell A1 → Hours should show 5
2. Select Spell A2 → Hours should change to 3
3. Select Spell C → Hours should change to 8
4. Save an entry → Hours should remain 8
5. Select another machine → Hours should still be 8
6. Change to Spell A1 → Hours should change to 5

## If Still Not Working

### Check Frontend Code:

File: `DrawingMeterEntryActivity.kt` Line 108-117

Should be:
```kotlin
spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        clearForm()
        val spell = spellList.getOrNull(position)
        spell?.workingHours?.let { hours ->
            etHours.setText(hours.toInt().toString())
        }
        loadSummary()
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

### Check Backend Code:

File: `e:\sjm\attendancesystem\app.py` Line 383

Should be:
```python
return jsonify({'status': 'success', 'shifts': data, 'total': len(data)})
```

NOT:
```python
return jsonify({'status': 'success', 'data': data, 'total': len(data)})
```

### Check API Service:

File: `ApiService.kt`

Should call:
```kotlin
@GET("/shifts")
fun getShifts(@Query("branch_id") branchId: Int? = null): Call<ShiftResponse>
```

### Check Response Model:

File: `ShiftResponse.kt`

Should have:
```kotlin
data class Shift(
    val id: Int,
    val name: String,
    val workingHours: Double? = 8.0
)

data class ShiftResponse(
    val status: String?,
    val shifts: List<Shift>?,  // NOT "data"
    val total: Int?
)
```

## Final Notes

- Backend location is CRITICAL: `e:\sjm\attendancesystem`
- Always restart Flask after deploying
- Test API before testing app
- Rebuild APK after backend changes
- working_hours field is in spell_mst table

## Quick Test Command

```cmd
curl http://localhost:5051/shifts | findstr "working_hours"
```

Should output lines containing working_hours values.

---

**Solution: Run `DEPLOY_AND_TEST.bat` to fix everything automatically!**


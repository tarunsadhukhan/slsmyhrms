# 🔧 DEPLOY HOURS FIELD FIX - STEP BY STEP

## Date: May 6, 2026

## ⚠️ CRITICAL STEPS - MUST FOLLOW IN ORDER

### Step 1: Restart Backend Server (MANDATORY!)

**Double-click:** `E:\sjm\MyHrms\restart_server.bat`

This will:
- Stop any existing Flask processes
- Start the Flask server with the updated code
- The server window will show "Server ready at http://0.0.0.0:5051"

**✅ Verify:** Look for the Flask server window and confirm it says "Server ready"

---

### Step 2: Build the Updated APK

**Double-click:** `E:\sjm\MyHrms\build_hours_fix.bat`

This will:
- Build the Android app with all the hours field fixes
- Take about 10-20 seconds
- Output: `app-debug.apk`

**✅ Verify:** Wait for "BUILD SUCCESSFUL" message

---

### Step 3: Install the APK

**APK Location:** 
```
E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

**Install:**
- Transfer to your Android device
- Install the APK (may need to allow "Install from unknown sources")
- Open the MyHrms app

---

### Step 4: Test the Hours Field Feature

#### Test 1: Initial Hours Load
1. Open Drawing Meter Entry
2. **✅ Check:** Hours field should be automatically filled (e.g., "8" or "5" or "12")
   - If it's empty, the backend may not be restarted or spell has NULL working_hours

#### Test 2: Spell Change Updates Hours
1. Click the Spell dropdown
2. Select a different spell
3. **✅ Check:** Hours field should update to the new spell's hours
4. Try 2-3 different spells
5. **✅ Check:** Each spell should show its own hours value

#### Test 3: Hours Persist After Save
1. Select a spell (note the hours value, e.g., "8")
2. Select a shed
3. Select a machine
4. Enter closing meter
5. Click SAVE
6. **✅ Check:** After save success message, hours field should STILL show "8"
7. Select another machine
8. Enter closing meter
9. Click SAVE
10. **✅ Check:** Hours field should STILL show "8"

#### Test 4: Full Workflow
1. Select "Spell A" → Hours shows 8
2. Save machine 1 → Hours still 8
3. Save machine 2 → Hours still 8
4. Change to "Spell B" → Hours updates to 12
5. Save machine 3 → Hours still 12
6. Save machine 4 → Hours still 12

---

## 🐛 Troubleshooting

### Problem: Hours field is empty

**Possible Causes:**
1. **Backend not restarted** → Restart using `restart_server.bat`
2. **Database has NULL values** → Check spell_mst.working_hours column
3. **Wrong branch/no spells** → Verify spell data exists for your branch

**Quick Fix:**
```sql
-- Update all spells to have 8 hours if NULL
UPDATE spell_mst SET working_hours = 8.0 WHERE working_hours IS NULL;
```

### Problem: Hours don't update when changing spell

**Possible Causes:**
1. Old APK installed → Rebuild and reinstall
2. Backend returning old format → Restart backend server

**Quick Check:**
- Open browser: `http://localhost:5051/shifts`
- Should see JSON with `"shifts"` key (not `"data"`)
- Each shift should have `"working_hours"` field

### Problem: Hours cleared after save

**Cause:** Old APK version installed

**Fix:** Rebuild APK using `build_hours_fix.bat` and reinstall

---

## 📋 Changes Summary

### Frontend (DrawingMeterEntryActivity.kt)
- ✅ Spell change now sets hours AFTER clearing form
- ✅ Hours preserved after successful save
- ✅ Initial load auto-fills hours from first spell

### Backend (app.py)
- ✅ `/shifts` endpoint returns `'shifts'` key (was `'data'`)
- ✅ Query includes `COALESCE(working_hours, 8.0)` for fallback

---

## 🎯 Expected User Experience

**Before Fix:**
- Had to manually type hours for EVERY machine entry
- Extremely tedious and error-prone
- Slow data entry

**After Fix:**
- Hours auto-fill when spell is selected
- Hours persist for all machines in the same spell
- Only need to change hours when switching spells
- MUCH faster data entry!

---

## 📞 Support

If hours field still doesn't work after following all steps:

1. Check Flask server window for errors
2. Check Android app logs (Logcat) for API errors
3. Verify database has working_hours data
4. Ensure both backend and app are updated versions

---

## ✅ Deployment Checklist

- [ ] Restart backend server (`restart_server.bat`)
- [ ] Build APK (`build_hours_fix.bat`)
- [ ] Install APK on device
- [ ] Test initial hours load
- [ ] Test spell change
- [ ] Test hours persistence after save
- [ ] Test full workflow with multiple machines

**All Done!** 🎉

The hours field should now work perfectly, making data entry much faster and easier!


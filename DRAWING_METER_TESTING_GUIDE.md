# Drawing Meter Entry - Testing Guide

**Feature:** Drawing Meter Entry Updates  
**Date:** May 6, 2026  
**Status:** Ready for Testing

---

## 🔍 Quick Visual Check

### Expected UI Changes:

#### 1. Shed Selection (Button Grid)
```
Before: [-- Select Shed --▼]  ← Dropdown
After:  [Shed A] [Shed B] [Shed C]  ← Horizontal buttons
```

#### 2. Meter Input (Integer Only)
```
Before: Opening: [500.00]  Closing: [700.00]  ← Accepts decimals
After:  Opening: [500]     Closing: [700]     ← Integers only
```

#### 3. Hours Auto-Fill
```
Before: Hours: [    ]  ← Empty, manual entry
After:  Hours: [8]     ← Auto-filled from spell
```

#### 4. Machine Name
```
Before: [MC1] [MC2] [MC3]  ← Shows "MC" + id
After:  [D1]  [D2]  [D3]   ← Shows short_name only
```

---

## ✅ Step-by-Step Testing

### Test 1: Shed Button Selection
1. Open Drawing Meter Entry screen
2. **Verify:** Shed buttons appear horizontally (not dropdown)
3. **Action:** Tap "Shed A" button
4. **Expected:** 
   - Button turns GREEN ✅
   - Other shed buttons remain BLUE
   - Machines load below
5. **Action:** Tap "Shed B" button
6. **Expected:**
   - "Shed B" turns GREEN
   - "Shed A" returns to BLUE
   - Different machines load

### Test 2: Hours Auto-Fill
1. **Verify:** Hours field is empty or has default value
2. **Action:** Select "Spell A" from dropdown
3. **Expected:** Hours field auto-fills (e.g., "8")
4. **Action:** Select "Spell B" from dropdown
5. **Expected:** Hours field updates to new value
6. **Note:** Value comes from `spell_mst.working_hours`

### Test 3: Machine Name Display
1. **Pre-req:** Select a shed to load machines
2. **Verify:** Machine buttons show only short names
3. **Expected:** 
   - ✅ Correct: [D1] [D2] [D3]
   - ❌ Wrong: [MCD1] [MCD2] [MCD3]
   - ❌ Wrong: [MC1] [MC2] [MC3]

### Test 4: Integer Meter Entry
1. **Action:** Select machine (turns green)
2. **Verify:** Meter displays as integer (e.g., "1500" not "1500.00")
3. **Action:** Tap in Opening meter field
4. **Action:** Try entering decimal: "500.5"
5. **Expected:** Only "500" or "5005" (no decimal point accepted)
6. **Action:** Enter integer: "500"
7. **Expected:** Accepts and displays "500"
8. **Action:** Tap in Closing meter field
9. **Action:** Try entering decimal: "700.5"
10. **Expected:** Only accepts integers
11. **Action:** Enter "700"
12. **Expected:** 
    - Closing shows "700"
    - Unit calculates: 700 - 500 = **200** (integer)
    - Efficiency calculates: (200 / 8 * 8) / 100 * 100 = **200.00%**

### Test 5: Complete Entry Flow
1. **Select Date:** Pick entry date
2. **Select Spell:** Choose spell → Hours auto-fill ✅
3. **Select Shed:** Tap shed button → Turns green ✅
4. **Select Machine:** Tap machine → Turns green, meter displays ✅
5. **Enter Closing:** Type integer value → Unit/Eff calculate ✅
6. **Tap Save:** 
   - Validates all fields
   - Saves entry
   - Shows success message
   - Clears form
   - Updates summary

### Test 6: Form Reset
1. **Pre-req:** Have shed and machine selected (green buttons)
2. **Action:** Change date or spell
3. **Expected:**
   - Selected shed button returns to BLUE
   - Machine buttons clear
   - All fields reset
4. **Action:** Select shed again
5. **Expected:** Can select again normally

### Test 7: Validation
1. **Action:** Tap Save without selecting shed
2. **Expected:** "Please select a shed" message
3. **Action:** Select shed, tap Save without machine
4. **Expected:** "Please select a machine" message
5. **Action:** Select machine, leave closing empty, tap Save
6. **Expected:** "Please enter closing meter" message
7. **Action:** Enter all values, tap Save
8. **Expected:** Success!

---

## 🐛 Edge Cases to Test

### Edge Case 1: First Entry of Day
- Opening meter should be 0
- Can enter any closing value
- Unit = Closing - 0

### Edge Case 2: Subsequent Entries
- Opening meter auto-fills from previous closing
- Cannot be less than previous closing

### Edge Case 3: Zero Hours
- If hours = 0, efficiency should show 0.00%
- Should not crash

### Edge Case 4: No Sheds Available
- Should show status message "No sheds found"
- Should not crash

### Edge Case 5: No Machines for Shed
- Should show status message "No machines found for selected shed"
- Should not crash

### Edge Case 6: Large Numbers
- Try entering large integers (e.g., 999999)
- Should accept and calculate correctly

---

## 📊 Expected Calculations

### Formula:
```
Unit = Closing - Opening (integer)
Efficiency = ((Unit / Hours * 8) / constValue * 100)
```

### Test Values:
| Opening | Closing | Hours | Const | Unit | Efficiency |
|---------|---------|-------|-------|------|------------|
| 500     | 700     | 8     | 100   | 200  | 200.00%    |
| 0       | 1000    | 8     | 100   | 1000 | 1000.00%   |
| 1000    | 1500    | 4     | 100   | 500  | 1000.00%   |
| 2000    | 2100    | 10    | 100   | 100  | 80.00%     |

---

## ✅ Success Criteria

All these must be true:

- [ ] Shed buttons display horizontally
- [ ] Selected shed button is GREEN
- [ ] Unselected shed buttons are BLUE
- [ ] Machine buttons show short_name only (no "MC")
- [ ] Selected machine button is GREEN
- [ ] Hours auto-fill when spell selected
- [ ] Opening meter shows integer (no decimal)
- [ ] Closing meter accepts only integers (keyboard blocks decimal)
- [ ] Meter (const_meter) displays as integer
- [ ] Unit displays as integer
- [ ] Efficiency displays with 2 decimal places
- [ ] Save validates shed selection
- [ ] Save validates machine selection
- [ ] Save works with integer values
- [ ] Summary displays correct values
- [ ] Clear form resets button colors
- [ ] No crashes on any action

---

## 🚫 Known Issues to Watch For

### Issue 1: Decimal Point Still Appears
**Symptom:** Can still type "500.5" in meter fields  
**Cause:** Input type not set to "number"  
**Fix:** Already fixed in `activity_drawing_meter_entry.xml`

### Issue 2: Hours Not Auto-Filling
**Symptom:** Hours field stays empty after spell selection  
**Cause:** Backend not returning working_hours OR field not mapped  
**Check:** 
- Backend logs for /shifts response
- ShiftResponse.kt has workingHours field
**Fix:** Already implemented in code

### Issue 3: Machine Shows "MC" Prefix
**Symptom:** Buttons show "MC1", "MC2" instead of "D1", "D2"  
**Cause:** mcShortName is null, falling back to mcId  
**Check:** Database has short_name values
**Fix:** Code uses `mc.mcShortName ?: ""` (shows empty if null)

### Issue 4: Shed Buttons Not Turning Green
**Symptom:** All buttons stay blue when tapped  
**Cause:** Button click handler not working  
**Fix:** Already implemented in `selectShed()` method

---

## 📱 Testing Environment

### Device Requirements:
- Android 6.0+ (API 23+)
- USB Debugging enabled
- Connected via ADB

### Check Device:
```powershell
adb devices
```

### Install APK:
```powershell
cd E:\sjm\MyHrms
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Watch Logs During Testing:
```powershell
adb logcat | Select-String "DrawingMeter"
```

---

## 🔄 If Issues Found

### Rebuild and Reinstall:
```powershell
cd E:\sjm\MyHrms
.\gradlew clean assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Check Backend:
```powershell
# Verify backend is running
# Check /shifts endpoint returns working_hours
Invoke-WebRequest -Uri "http://localhost:5000/shifts?branch_id=1"
```

### View File:
```powershell
# If need to check implementation
code "E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\DrawingMeterEntryActivity.kt"
```

---

## 📋 Test Report Template

### Date: _____________
### Tester: _____________
### Device: _____________
### Build: app-debug.apk (May 6, 2026)

| Test Case | Status | Notes |
|-----------|--------|-------|
| Shed buttons display | ☐ Pass ☐ Fail | |
| Shed selection (green) | ☐ Pass ☐ Fail | |
| Hours auto-fill | ☐ Pass ☐ Fail | |
| Machine name display | ☐ Pass ☐ Fail | |
| Machine selection (green) | ☐ Pass ☐ Fail | |
| Integer meter entry | ☐ Pass ☐ Fail | |
| Meter display (integer) | ☐ Pass ☐ Fail | |
| Unit calculation | ☐ Pass ☐ Fail | |
| Efficiency calculation | ☐ Pass ☐ Fail | |
| Save validation | ☐ Pass ☐ Fail | |
| Form reset | ☐ Pass ☐ Fail | |
| Summary display | ☐ Pass ☐ Fail | |

### Overall Result: ☐ PASS ☐ FAIL

### Issues Found:
1. 
2. 
3. 

### Screenshots:
- Shed buttons: _____________
- Machine buttons: _____________
- Meter entry: _____________
- Summary: _____________

---

## 🎯 Quick 5-Minute Test

**Speed test for rapid verification:**

1. ✅ Open Drawing Meter Entry
2. ✅ See horizontal shed buttons (not dropdown)
3. ✅ Tap shed → Turns green
4. ✅ Select spell → Hours field fills
5. ✅ Tap machine → Shows short name only (no "MC")
6. ✅ Try entering decimal in meter → Blocks it
7. ✅ Enter integer meters → Calculates unit
8. ✅ Tap Save → Success

**If all 8 steps pass: READY FOR PRODUCTION** ✅

---

**Testing Guide Complete!**  
For detailed implementation: See `DRAWING_METER_UPDATES_COMPLETE.md`


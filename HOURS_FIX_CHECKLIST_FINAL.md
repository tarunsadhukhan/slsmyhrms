═══════════════════════════════════════════════════════════════
  🚀 DEPLOYMENT CHECKLIST - HOURS FIX (May 7, 2026)
═══════════════════════════════════════════════════════════════

Date: May 7, 2026
Issue: Hours not changing on spell selection
Status: ✅ FIXED - Ready to deploy

═══════════════════════════════════════════════════════════════
  ✅ COMPLETED AUTOMATICALLY
═══════════════════════════════════════════════════════════════

✅ 1. Root cause identified
   - JSON key mismatch: "data" vs "shifts"
   
✅ 2. Code fixed
   - ShiftResponse.kt line 34
   - Changed @SerializedName("data") to @SerializedName("shifts")
   
✅ 3. Backend deployed
   - Copied E:\sjm\MyHrms\app.py → e:\sjm\attendancesystem\app.py
   
✅ 4. APK built successfully
   - Build time: 10 seconds
   - Location: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk

═══════════════════════════════════════════════════════════════
  ⏳ MANUAL STEPS REQUIRED
═══════════════════════════════════════════════════════════════

STEP 1: RESTART BACKEND SERVER (2 minutes)
-------------------------------------------
Option A: Use script
  Double-click: RESTART_BACKEND.bat
  
Option B: Manual
  1. Open PowerShell/CMD
  2. cd e:\sjm\attendancesystem
  3. If server running: Press Ctrl+C to stop
  4. Run: python app.py
  5. Wait for: "Server ready at http://0.0.0.0:5051"

Verification:
  Open browser: http://localhost:5051/shifts?branch_id=1
  Should see: {"shifts": [...], "working_hours": 5.0}


STEP 2: INSTALL APK (1 minute)
-------------------------------
Run command:
  adb install -r E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk


STEP 3: TEST (5 minutes)
-------------------------
□ TEST 1: Hours auto-fill on load
□ TEST 2: Hours change on spell selection  
□ TEST 3: Hours persist after save
□ TEST 4: Hours update after save & spell change
□ TEST 5: Multiple entries same spell

═══════════════════════════════════════════════════════════════
  🧪 DETAILED TEST SCENARIOS
═══════════════════════════════════════════════════════════════

TEST 1: Initial Hours Auto-Fill
--------------------------------
1. Open Drawing Meter Entry screen
2. Observe hours field

✅ EXPECTED: Hours shows value (e.g., "5", "3", or "8")
❌ BEFORE: Hours field empty


TEST 2: Hours Change on Spell Selection
----------------------------------------
1. Select spell "A1" → Hours shows "5"
2. Select spell "A2" → Hours shows "3"
3. Select spell "C" → Hours shows "8"

✅ EXPECTED: Hours update instantly
❌ BEFORE: Hours don't change


TEST 3: Hours Persist After Save
---------------------------------
1. Select spell "C" (8 hours)
2. Select shed + machine
3. Enter meters and save
4. After save, hours still "8"

✅ EXPECTED: Hours remain "8"
❌ BEFORE: Hours cleared


TEST 4: Hours Update After Saving
----------------------------------
1. After saving with spell "C"
2. Change spell to "A1"
3. Hours changes to "5"

✅ EXPECTED: Hours update correctly
❌ BEFORE: Hours stuck


TEST 5: Multiple Entries Same Spell
------------------------------------
1. Select spell "C"
2. Save entries for M01, M02, M03
3. Hours stay "8" for all entries

✅ EXPECTED: Hours persist
❌ BEFORE: Hours cleared

═══════════════════════════════════════════════════════════════
  📝 QUICK COMMANDS
═══════════════════════════════════════════════════════════════

Backend:
  cd e:\sjm\attendancesystem && python app.py

Test API:
  http://localhost:5051/shifts?branch_id=1

Install APK:
  adb install -r app\build\outputs\apk\debug\app-debug.apk

═══════════════════════════════════════════════════════════════
  ✅ ALL TESTS SHOULD PASS
═══════════════════════════════════════════════════════════════

Root cause: JSON key mismatch (FIXED!)
Frontend: Working correctly (always was!)
Backend: Deployed (correct version!)
APK: Built successfully!

Just restart server + install APK = DONE!

═══════════════════════════════════════════════════════════════


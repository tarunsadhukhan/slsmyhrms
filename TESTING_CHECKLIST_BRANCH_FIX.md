# Testing Checklist - Branch Validation Fix

**Date:** April 23, 2026 20:00  
**Device:** RMX3395 - Android 14  
**Status:** ✅ APK Installed & Running

---

## ✅ Installation Complete

- ✅ APK Built: 6.71 MB
- ✅ Installed on Device: RMX3395 - 14
- ✅ App Launched: LoginActivity
- ✅ Backend Running: http://localhost:5051

---

## 📋 Testing Steps

### Step 1: Login
- [ ] Open MyHrms app (should be open now)
- [ ] Enter username and password
- [ ] Select company and branch
- [ ] Login successfully

### Step 2: Navigate to Attendance Entry
- [ ] From dashboard, tap "Attendance Entry"
- [ ] Verify date selector works
- [ ] Check all fields are visible

### Step 3: Test Employee Lookup (Main Fix)
- [ ] Enter employee code: **13177**
- [ ] Click the check button (✓)
- [ ] **Expected Results:**
  - ✅ Employee name appears: "KRISHNA PRASAD"
  - ✅ Photo loads (if available)
  - ✅ Employee info card displays
  - ✅ No error messages
  - ✅ Backend logs show: `GET /employee/13177?branch_id=29 HTTP/1.1" 200`

### Step 4: Test with Invalid Employee
- [ ] Clear the employee code field
- [ ] Enter: **99999**
- [ ] Click check button
- [ ] **Expected Results:**
  - ❌ "Employee not found" message
  - ❌ No crash

### Step 5: Complete Attendance Entry
- [ ] Select department (e.g., PREPARING)
- [ ] Select designation (e.g., BREAKER FEEDER)
- [ ] Select shift
- [ ] Click Submit
- [ ] **Expected Results:**
  - ✅ Attendance saved successfully
  - ✅ Success message appears

---

## 🔍 What to Watch For

### ✅ Good Signs:
- Employee lookup works without errors
- Branch validation happens automatically
- Photo displays correctly
- Backend logs show `200 OK` responses

### ❌ Red Flags:
- 400 errors in backend logs
- "Employee not found" for valid employees
- App crashes
- Missing branch_id in requests

---

## 🖥️ Backend Monitoring

### Check Backend Logs:
Look for these request patterns:
```
GET /employee/13177?branch_id=29 HTTP/1.1" 200 -
```

**NOT:**
```
GET /employee/13177 HTTP/1.1" 400 -
```

### Manual API Test:
```powershell
# Test the API directly
curl "http://localhost:5051/employee/13177?branch_id=29"
```

**Expected Response:**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "KRISHNA  PRASAD",
  "branch_id": 29,
  "message": "Employee found: KRISHNA  PRASAD"
}
```

---

## 🐛 Troubleshooting

### Issue: Employee not found (but should exist)
**Solution:**
1. Check backend logs for the exact request
2. Verify branch_id is being sent in query string
3. Verify employee 13177 exists in branch 29
4. Test API directly with curl command above

### Issue: 400 Error Still Appears
**Solution:**
1. Verify you installed the latest APK (6.71 MB, dated April 23 20:00)
2. Force stop the app and restart
3. Clear app cache if needed
4. Check ApiService.kt has the updated signature

### Issue: App Crashes
**Solution:**
1. Check logcat: `adb logcat | grep -i myhrms`
2. Verify backend is running: `curl http://localhost:5051/ping`
3. Check network connectivity

---

## 📊 Test Results

### Employee Lookup Test:
- [ ] **Test 1:** Valid employee in correct branch → ✅ PASS / ❌ FAIL
- [ ] **Test 2:** Invalid employee code → ✅ PASS / ❌ FAIL
- [ ] **Test 3:** Photo loads correctly → ✅ PASS / ❌ FAIL
- [ ] **Test 4:** No 400 errors in backend → ✅ PASS / ❌ FAIL

### Integration Test:
- [ ] **Test 5:** Complete attendance entry → ✅ PASS / ❌ FAIL
- [ ] **Test 6:** Update existing attendance → ✅ PASS / ❌ FAIL
- [ ] **Test 7:** Multiple employee lookups → ✅ PASS / ❌ FAIL

---

## ✅ Success Criteria

All of these must be true:
- ✅ No 400 errors when looking up employees
- ✅ Employee name and photo display correctly
- ✅ branch_id appears in API requests
- ✅ Backend logs show 200 OK responses
- ✅ Attendance can be submitted successfully

---

## 🎯 Quick Test Commands

### Check App is Running:
```powershell
adb shell dumpsys activity | Select-String "myhrms"
```

### Monitor Backend Logs:
```powershell
# Backend should show requests with branch_id
# Watch for: GET /employee/13177?branch_id=29
```

### Restart App if Needed:
```powershell
adb shell am force-stop com.example.myhrms
adb shell am start -n com.example.myhrms/.LoginActivity
```

### Reinstall if Issues:
```powershell
cd E:\sjm\MyHrms
.\gradlew.bat installDebug
```

---

## 📝 Notes

**Key Change:**
The app now sends `branch_id` parameter when looking up employees, ensuring proper validation and eliminating 400 errors.

**Files Modified:**
- `ApiService.kt` - Added branch_id query parameter
- `AttendanceActivity.kt` - Passes branch_id from login

**APK Details:**
- Version: April 23, 2026 20:00
- Size: 6.71 MB
- Device: RMX3395 - Android 14

---

## 🚀 Ready to Test!

1. The app is now running on your device
2. Backend is active at http://localhost:5051
3. Follow the testing steps above
4. Mark each test as PASS or FAIL
5. Report any issues you encounter

**Expected Result:** All tests should PASS ✅

---

**Last Updated:** April 23, 2026 20:00  
**Status:** Installation Complete - Ready for Testing


# 🚀 QUICK START - Attendance Update Deployment

**Updated:** April 24, 2026 8:50 PM  
**Status:** ✅ Android APK Ready | ⚠️ Backend Update Required

---

## ⚡ 3-Step Deployment

### Step 1: Install Android APK (2 minutes)
```powershell
adb install -r E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

### Step 2: Update Backend (5 minutes)
```powershell
# Run helper script
E:\sjm\MyHrms\apply_backend_updates.ps1

# Or manually:
# 1. Open: e:\sjm\attendancesystem\app.py
# 2. Apply changes from: BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py
# 3. Restart: python app.py
```

### Step 3: Test (1 minute)
```powershell
# Test API
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&branch_id=29"

# Should return data with eb_id and machine_nos fields
```

---

## ✅ What's Fixed

**Before:**
- Only sending date and emp_code
- Missing spell and name parameters

**After:**
- Sends: date, emp_code, emp_name, shift_name, branch_id
- Returns: eb_id, machine_nos (machine numbers)
- Display: Date, Spell, EB No, Name, Designation, MC Nos, Working Hours

---

## 📋 Backend Changes Summary

Update `/attendance-report` in `e:\sjm\attendancesystem\app.py`:

1. ✅ Accept both `date` (single) and `from_date/to_date` (range)
2. ✅ Add `emp_name` parameter (searches name)
3. ✅ Add `shift_name` parameter (filters by spell)
4. ✅ Add `p.eb_id` to SELECT query
5. ✅ Fetch machine numbers from `daily_ebmc_attendance`
6. ✅ Return `machine_nos` as comma-separated string

**Complete code in:** `BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py`

---

## 🧪 Quick Test

### Test Attendance Update (Single Date)
```powershell
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&emp_code=13111&emp_name=John&shift_name=Morning&branch_id=29"
```

### Test Attendance Report (Date Range)
```powershell
curl "http://192.168.0.223:5051/attendance-report?from_date=2026-04-01&to_date=2026-04-30&department_id=1&branch_id=29"
```

### Expected Response
```json
{
  "status": "success",
  "data": [{
    "emp_code": "13111",
    "eb_id": 5678,
    "emp_name": "John Doe",
    "shift_name": "Morning",
    "designation_name": "Operator",
    "machine_nos": "1001, 1002, 1003",
    "working_hours": 8.0
  }]
}
```

---

## 📚 Documentation

| File | Purpose |
|------|---------|
| **ATTENDANCE_UPDATE_READY_TO_DEPLOY.md** | Complete deployment guide |
| **BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py** | Exact backend code changes |
| **apply_backend_updates.ps1** | Helper script |
| **HOW_TO_VIEW_LOGS.md** | Android logging guide |

---

## ⚠️ Important

- **Backend location:** `e:\sjm\attendancesystem\app.py` (NOT MyHrms)
- **Backup first:** Always backup before editing
- **Test both:** Single date AND date range queries

---

**Need help?** See `ATTENDANCE_UPDATE_READY_TO_DEPLOY.md` for detailed instructions.


# ✅ Dashboard Department-Wise Update - Backend Complete

**Date:** April 24, 2026 10:45 PM  
**Status:** ✅ Backend Updated - Restart Required

---

## 🎯 What Changed

### NEW Two-Section Department Display:

Instead of one filtered list, the dashboard now returns TWO separate department lists:

#### 1. **Present Departments** (`department_present`)
- Shows departments that have attendance records TODAY
- Based on `daily_attendance` table
- Only shows departments where `present > 0`

#### 2. **Master Departments** (`department_master`)  
- Shows ALL departments that have employees
- Based on `hrms_ed_official_details` table
- Shows all departments regardless of today's attendance

---

## 📊 API Response Format

### Before (OLD):
```json
{
  "department_wise": [
    {"department_id": 7, "department_name": "BEAMING", "present": 3, "absent": 2}
  ]
}
```

### After (NEW):
```json
{
  "department_present": [
    {"department_id": 7, "department_name": "BEAMING", "present": 3, "absent": 2}
  ],
  "department_master": [
    {"department_id": 7, "department_name": "BEAMING", "present": 3, "absent": 2},
    {"department_id": 8, "department_name": "WEAVING", "present": 0, "absent": 10},
    {"department_id": 9, "department_name": "FINISHING", "present": 0, "absent": 5}
  ]
}
```

---

## 🔧 Query Details

### Present Departments Query:
```sql
SELECT sdm.sub_dept_id AS department_id,
       sdm.sub_dept_desc AS department_name,
       COUNT(o.eb_id) AS total_employees,
       COALESCE(da.present, 0) AS present
FROM sub_dept_mst sdm
LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id
LEFT JOIN hrms_ed_official_details o ON sdm.sub_dept_id = o.sub_dept_id
LEFT JOIN (
    SELECT da.branch_id, heod.sub_dept_id, COUNT(*) AS present
    FROM daily_attendance da
    LEFT JOIN hrms_ed_official_details heod ON da.eb_id = heod.eb_id
    WHERE da.attendance_date = %s AND da.is_active = 1
    GROUP BY da.branch_id, heod.sub_dept_id
) da ON dm.branch_id = da.branch_id AND sdm.sub_dept_id = da.sub_dept_id
WHERE dm.branch_id = 29
  AND COALESCE(da.present, 0) > 0  -- Only departments with present
GROUP BY sdm.sub_dept_id, sdm.sub_dept_desc
ORDER BY sdm.sub_dept_desc
```

### Master Departments Query:
Same query but **WITHOUT** the `COALESCE(da.present, 0) > 0` filter - shows all departments.

---

## 🚀 Deployment

### Step 1: Restart Backend Server

```powershell
cd e:\sjm\attendancesystem

# Stop current server (Ctrl+C)

# Restart
python app.py
```

### Step 2: Test API

```powershell
curl "http://192.168.0.223:5051/dashboard-stats?date=2026-04-24&branch_id=29"
```

**Expected Response:**
- `department_present` array with departments having attendance today
- `department_master` array with all departments

---

## 📱 Frontend Update Needed

The Android app currently uses `department_wise`. It needs to be updated to use:
- `department_present` for "Present" card
- `department_master` for "Master" card (renamed from "Absent")

---

## ✅ Backend Changes Summary

**File:** `e:\sjm\attendancesystem\src\dashboard\dashboard.py`

**Changes:**
1. ✅ Removed old `department_wise` logic
2. ✅ Added `department_present` query (with present > 0 filter)
3. ✅ Added `department_master` query (all departments)
4. ✅ Both queries use your specified LEFT JOIN structure
5. ✅ Response now includes both lists

---

## ⚠️ Next Steps

### Required:
1. **Restart backend server** ← Do this first!
2. Test API with curl to verify both lists return correctly
3. Update Android app to use the two new fields

### Android Updates Needed:
- Update `DashboardStatsResponse.kt` data model
- Update dashboard to show two cards/sections
- Rename "Absent" to "Master"

---

**Status:** ✅ Backend Complete  
**Action Required:** Restart backend server  
**Frontend:** Needs update to use new API fields


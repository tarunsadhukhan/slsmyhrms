# ✅ Complete Implementation - Machine Attendance Tracking (daily_ebmc_attendance)

## Backend Location: E:\sjm\AttendanceSystem

---

## Changes Made

### 1. ✅ Added Query Constant to query.py
**File:** `E:\sjm\AttendanceSystem\src\attendance\query.py`

**Added:**
```python
INSERT_MACHINE_ATTENDANCE = """
    INSERT INTO daily_ebmc_attendance
      (daily_atten_id, eb_id, mech_id, attendance_date,
       branch_id, is_active, update_date_time)
    VALUES (%s, %s, %s, %s, %s, %s, NOW())
"""
```

**Purpose:** Centralized SQL query for inserting machine attendance records.

---

### 2. ✅ Updated Face Recognition Endpoint
**File:** `E:\sjm\AttendanceSystem\src\attendance\attendance.py`
**Function:** `mark_attendance()` (Line ~105-115)

**Added Code:**
```python
# Get the inserted attendance ID
attendance_id = cursor.lastrowid

# Save machine data to daily_ebmc_attendance if machines are provided
machine_ids = data.get('machine_ids', [])
if machine_ids and isinstance(machine_ids, list):
    for machine_id in machine_ids:
        cursor.execute(Q.INSERT_MACHINE_ATTENDANCE,
                     (attendance_id, eb_id, machine_id, att_date, branch_id, 1))

db.commit()
```

**Endpoint:** `POST /attendance`

---

### 3. ✅ Updated Manual Attendance Endpoint
**File:** `E:\sjm\AttendanceSystem\src\attendance\attendance.py`
**Function:** `mark_attendance_manual()` (Line ~205-215)

**Added Code:**
```python
# Get the inserted attendance ID
attendance_id = cursor.lastrowid

# Save machine data to daily_ebmc_attendance if machines are provided
machine_ids = data.get('machine_ids', [])
if machine_ids and isinstance(machine_ids, list):
    for machine_id in machine_ids:
        cursor.execute(Q.INSERT_MACHINE_ATTENDANCE,
                     (attendance_id, eb_id, machine_id, att_date, branch_id, 1))

db.commit()
```

**Endpoint:** `POST /mark-attendance`

---

## Implementation Pattern

### Code Flow:
```python
# 1. Insert to daily_attendance
cursor.execute(Q.INSERT_ATTENDANCE, (...))

# 2. Get the auto-generated daily_atten_id
attendance_id = cursor.lastrowid

# 3. Insert machines to daily_ebmc_attendance
machine_ids = data.get('machine_ids', [])
if machine_ids and isinstance(machine_ids, list):
    for machine_id in machine_ids:
        cursor.execute(Q.INSERT_MACHINE_ATTENDANCE,
                     (attendance_id, eb_id, machine_id, att_date, branch_id, 1))

# 4. Commit transaction
db.commit()
```

---

## Database Schema

### Table: daily_ebmc_attendance
```sql
CREATE TABLE daily_ebmc_attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    daily_atten_id INT NOT NULL,        -- FK to daily_attendance.daily_atten_id
    eb_id INT NOT NULL,                  -- Employee base ID
    mech_id INT NOT NULL,                -- Machine ID
    attendance_date DATE NOT NULL,       -- Date of attendance
    branch_id INT,                       -- Branch identifier
    is_active TINYINT DEFAULT 1,         -- Active flag
    update_date_time DATETIME,           -- Timestamp (auto: NOW())
    KEY idx_daily_atten (daily_atten_id),
    KEY idx_eb_date (eb_id, attendance_date)
);
```

---

## API Request Examples

### Face Recognition with Machines
```json
POST /attendance
{
  "image": "data:image/jpeg;base64,/9j/4AAQ...",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 199,
  "attendance_date": "2026-04-24",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0,
  "machine_ids": [1344, 1345, 1346],
  "branch_id": 29
}
```

### Manual Attendance with Machines
```json
POST /mark-attendance
{
  "emp_code": "13177",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 199,
  "attendance_date": "2026-04-24",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0,
  "machine_ids": [1344, 1345, 1346],
  "branch_id": 29
}
```

---

## Testing

### 1. Restart Backend Server
```powershell
cd E:\sjm\AttendanceSystem
python app.py
```

### 2. Test Manual Attendance
```powershell
curl -X POST http://192.168.0.223:5051/mark-attendance `
  -H "Content-Type: application/json" `
  -d '{
    "emp_code": "13177",
    "att_type": "R",
    "branch_id": 29,
    "department_id": 1,
    "shift_id": 5,
    "designation_id": 199,
    "attendance_date": "2026-04-24",
    "shift_hours": 8.0,
    "working_hours": 8.0,
    "idle_hours": 0.0,
    "machine_ids": [1344, 1345, 1346]
  }'
```

### 3. Verify Database
```sql
-- Check last attendance record
SELECT * FROM daily_attendance 
ORDER BY daily_atten_id DESC LIMIT 1;

-- Check machine records (replace <ID> with daily_atten_id from above)
SELECT 
    dea.daily_atten_id,
    dea.eb_id,
    dea.mech_id,
    m.machine_name,
    m.machine_no,
    dea.attendance_date,
    dea.branch_id
FROM daily_ebmc_attendance dea
JOIN machine_mst m ON dea.mech_id = m.machine_id
WHERE dea.daily_atten_id = <ID>;
```

### Expected Result:
- 1 row in `daily_attendance` with `daily_atten_id` (e.g., 5001)
- 3 rows in `daily_ebmc_attendance` (one for each machine: 1344, 1345, 1346)
- All rows have `daily_atten_id = 5001`

---

## Code Architecture Benefits

### ✅ Follows Best Practices
1. **Separation of Concerns:** SQL queries in `query.py`, business logic in `attendance.py`
2. **DRY Principle:** Query constant `Q.INSERT_MACHINE_ATTENDANCE` reused in both endpoints
3. **Maintainability:** Easy to update query in one place
4. **Consistency:** Same pattern as existing `Q.INSERT_ATTENDANCE`

### ✅ Transaction Safety
- Machine inserts happen BEFORE `db.commit()`
- If any machine insert fails, entire transaction rolls back
- Maintains referential integrity

### ✅ Flexibility
- `machine_ids` is optional (can be `null` or `[]`)
- Multiple machines supported (loops through list)
- Same implementation for Face and Manual attendance

---

## Files Modified

1. ✅ `E:\sjm\AttendanceSystem\src\attendance\query.py` - Added `INSERT_MACHINE_ATTENDANCE` query
2. ✅ `E:\sjm\AttendanceSystem\src\attendance\attendance.py` - Added machine tracking to both endpoints

---

## Android App Status

✅ **Already Implemented:**
- `FaceRecognitionRequest.kt` - Has `machine_ids` and `branch_id` fields
- `MarkAttendanceRequest.kt` - Has `machine_ids` and `branch_id` fields
- `AttendanceActivity.kt` - Sends machine IDs in both Face and Manual modes

**No Android changes needed** - The app is already sending the data correctly!

---

## Summary

### What Was Done:
1. ✅ Added `INSERT_MACHINE_ATTENDANCE` query to `query.py`
2. ✅ Updated Face Recognition endpoint to save machines
3. ✅ Updated Manual attendance endpoint to save machines
4. ✅ Used query constants (no inline SQL)
5. ✅ Proper transaction handling with `cursor.lastrowid`

### Result:
- Both Face Recognition and Manual attendance now save machine data
- Machines linked to attendance via `daily_atten_id`
- Multiple machines per attendance supported
- Follows project architecture (queries in query.py)

### Next Steps:
1. Restart backend server
2. Test with mobile app
3. Verify database records
4. Monitor backend logs for any errors

---

**Date:** April 24, 2026  
**Status:** ✅ Complete Implementation  
**Backend:** E:\sjm\AttendanceSystem  
**Architecture:** Query constants in query.py ✅


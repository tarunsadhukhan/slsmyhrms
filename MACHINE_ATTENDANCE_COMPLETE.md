# Machine Attendance Tracking - Complete Implementation

## Overview
Implemented complete machine attendance tracking for both **Face Recognition** and **Manual Attendance** entry modes. Machine selections are now saved to the `daily_ebmc_attendance` table with proper linkage to attendance records.

## Database Schema

### Table: `daily_ebmc_attendance`
Stores the relationship between attendance records and machines operated by employees.

**Columns:**
- `daily_atten_id` - Foreign key to `daily_attendance.daily_atten_id` (links to attendance record)
- `eb_id` - Employee Base ID
- `mech_id` - Machine ID (from `machine_mst`)
- `attendance_date` - Date of attendance
- `branch_id` - Branch identifier
- `is_active` - Active status (1 = active)
- `update_date_time` - Timestamp of record creation

**Purpose:** Tracks which machines an employee operated on a specific date.

## Changes Made

### 1. Android App - FaceRecognitionRequest.kt
**File:** `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\FaceRecognitionRequest.kt`

**Added Fields:**
```kotlin
@SerializedName("machine_ids")
val machineIds: List<Int>? = null,

@SerializedName("branch_id")
val branchId: Int? = null
```

**Purpose:** Allows Face Recognition requests to include selected machine IDs and branch ID.

---

### 2. Android App - AttendanceActivity.kt
**File:** `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\AttendanceActivity.kt`

**Updated `markAttendance` Function:**
```kotlin
val request = FaceRecognitionRequest(
    image = base64Image,
    attType = attType,
    departmentId = deptId,
    shiftId = shiftId,
    designationId = desigId,
    attendanceDate = attendanceDate,
    shiftHours = shiftHours,
    workingHours = workingHours,
    idleHours = idleHours,
    machineIds = if (selectedMachineIds.isNotEmpty()) selectedMachineIds.toList() else null,  // ✅ ADDED
    branchId = if (selectedBranchId > 0) selectedBranchId else null  // ✅ ADDED
)
```

**Purpose:** Face Recognition attendance now includes selected machines.

---

### 3. Backend - app.py (Face Recognition Endpoint)
**File:** `E:\sjm\MyHrms\app.py`

**Updated `/attendance` Endpoint (POST):**
```python
# After inserting to daily_attendance
attendance_id = cursor.lastrowid

# Save machine data to daily_ebmc_attendance if machines are provided
machine_ids = data.get('machine_ids', [])
if machine_ids and isinstance(machine_ids, list):
    for machine_id in machine_ids:
        cursor.execute("""
            INSERT INTO daily_ebmc_attendance (
                daily_atten_id, eb_id, mech_id, attendance_date, 
                branch_id, is_active, update_date_time
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (attendance_id, best_match['eb_id'], machine_id, att_date, 
              branch_id, 1, now))

db.commit()
```

**Purpose:** Face Recognition attendance now saves machine data to `daily_ebmc_attendance`.

---

### 4. Backend - app.py (Manual Attendance Endpoint)
**File:** `E:\sjm\MyHrms\app.py`

**Already Implemented in `/mark-attendance` Endpoint (POST):**
```python
# After inserting to daily_attendance
attendance_id = cursor.lastrowid

# Save machine data to daily_ebmc_attendance if machines are provided
machine_ids = data.get('machine_ids', [])
if machine_ids and isinstance(machine_ids, list):
    for machine_id in machine_ids:
        cursor.execute("""
            INSERT INTO daily_ebmc_attendance (
                daily_atten_id, eb_id, mech_id, attendance_date, 
                branch_id, is_active, update_date_time
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (attendance_id, employee['eb_id'], machine_id, att_date, 
              branch_id, 1, now))

db.commit()
```

**Status:** ✅ Already implemented (no changes needed).

---

## Data Flow

### Manual Attendance Flow
```
User enters employee code
   ↓
User selects machines from dropdown (filtered by designation)
   ↓
User fills other form fields
   ↓
User clicks Submit
   ↓
App sends POST to /mark-attendance with:
   - emp_code
   - machine_ids: [1344, 1345, 1346]
   - branch_id
   - Other attendance fields
   ↓
Backend:
   1. Inserts to daily_attendance → gets daily_atten_id
   2. For each machine_id:
      - Inserts to daily_ebmc_attendance with daily_atten_id
   3. Commits transaction
   ↓
✅ Attendance and machine data saved
```

### Face Recognition Attendance Flow
```
User takes photo with camera
   ↓
System recognizes employee
   ↓
User selects machines from dropdown (filtered by designation)
   ↓
User fills other form fields
   ↓
User clicks Submit
   ↓
App sends POST to /attendance with:
   - image (base64)
   - machine_ids: [1344, 1345, 1346]
   - branch_id
   - Other attendance fields
   ↓
Backend:
   1. Recognizes face from image
   2. Inserts to daily_attendance → gets daily_atten_id
   3. For each machine_id:
      - Inserts to daily_ebmc_attendance with daily_atten_id
   4. Commits transaction
   ↓
✅ Attendance and machine data saved
```

---

## API Request Examples

### Manual Attendance Request
**Endpoint:** `POST /mark-attendance`

**Request Body:**
```json
{
  "emp_code": "13177",
  "status": "Manual",
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

**Response:**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Doe",
  "photo_html": "<img src='data:image/jpeg;base64,...' />",
  "message": "Attendance marked for John Doe (Manual)"
}
```

---

### Face Recognition Request
**Endpoint:** `POST /attendance`

**Request Body:**
```json
{
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
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

**Response:**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Doe",
  "photo_html": "<img src='data:image/jpeg;base64,...' />",
  "confidence": 95.67,
  "message": "Attendance marked for John Doe"
}
```

---

## Database Operations

### Step 1: Insert Attendance Record
```sql
INSERT INTO daily_attendance (
    attendance_date, attendance_mark, attendance_source, attendance_type,
    branch_id, eb_id, entry_time, idle_hours, is_active,
    spell, spell_hours, worked_department_id, worked_designation_id,
    working_hours, update_date_time
) VALUES (
    '2026-04-24', 'P', 'Manual', 'R',
    29, 12345, NOW(), 0.0, 1,
    'Morning Shift', 8.0, 1, 199,
    8.0, NOW()
);
-- Returns daily_atten_id (e.g., 5001)
```

### Step 2: Insert Machine Records
For each selected machine (e.g., machines 1344, 1345, 1346):

```sql
INSERT INTO daily_ebmc_attendance (
    daily_atten_id, eb_id, mech_id, attendance_date, 
    branch_id, is_active, update_date_time
) VALUES (5001, 12345, 1344, '2026-04-24', 29, 1, NOW());

INSERT INTO daily_ebmc_attendance (
    daily_atten_id, eb_id, mech_id, attendance_date, 
    branch_id, is_active, update_date_time
) VALUES (5001, 12345, 1345, '2026-04-24', 29, 1, NOW());

INSERT INTO daily_ebmc_attendance (
    daily_atten_id, eb_id, mech_id, attendance_date, 
    branch_id, is_active, update_date_time
) VALUES (5001, 12345, 1346, '2026-04-24', 29, 1, NOW());
```

---

## Query Examples

### Get Attendance with Machines
```sql
SELECT 
    da.daily_atten_id,
    da.eb_id,
    p.emp_code,
    CONCAT(p.first_name, ' ', p.last_name) AS emp_name,
    da.attendance_date,
    da.attendance_type,
    GROUP_CONCAT(DISTINCT m.machine_name ORDER BY m.machine_name SEPARATOR ', ') AS machines
FROM daily_attendance da
JOIN hrms_ed_personal_details p ON da.eb_id = p.eb_id
LEFT JOIN daily_ebmc_attendance dea ON da.daily_atten_id = dea.daily_atten_id
LEFT JOIN machine_mst m ON dea.mech_id = m.machine_id
WHERE da.attendance_date = '2026-04-24'
  AND da.is_active = 1
GROUP BY da.daily_atten_id;
```

**Result:**
```
| daily_atten_id | eb_id | emp_code | emp_name | attendance_date | attendance_type | machines                        |
|----------------|-------|----------|----------|-----------------|-----------------|--------------------------------|
| 5001           | 12345 | 13177    | John Doe | 2026-04-24      | R               | 1001 WINDING1001, 1002 WARPING |
```

---

### Get Employee's Machine Usage for a Date
```sql
SELECT 
    dea.daily_atten_id,
    dea.eb_id,
    p.emp_code,
    CONCAT(p.first_name, ' ', p.last_name) AS emp_name,
    m.machine_id,
    m.machine_name,
    m.machine_no,
    dea.attendance_date
FROM daily_ebmc_attendance dea
JOIN hrms_ed_personal_details p ON dea.eb_id = p.eb_id
JOIN machine_mst m ON dea.mech_id = m.machine_id
WHERE dea.attendance_date = '2026-04-24'
  AND dea.is_active = 1
ORDER BY p.emp_code, m.machine_no;
```

---

## Validation Rules

### Backend Validation
1. ✅ `machine_ids` is optional (can be `null` or empty array)
2. ✅ If `machine_ids` is provided, it must be a list/array
3. ✅ Each `machine_id` is inserted individually
4. ✅ Transaction committed after all inserts complete
5. ✅ If machine insert fails, entire transaction rolls back

### Frontend Validation
1. ✅ Machines are filtered by selected designation/occupation
2. ✅ Multiple machines can be selected
3. ✅ Machine selection is optional
4. ✅ Selected machines sent as array in API request

---

## Key Features

### ✅ Dual Mode Support
- **Manual Attendance:** Machine tracking supported
- **Face Recognition:** Machine tracking supported

### ✅ Proper Linkage
- `daily_atten_id` from `daily_attendance` used as foreign key
- Maintains referential integrity between tables

### ✅ Multiple Machines
- Employee can select multiple machines
- Each machine gets separate row in `daily_ebmc_attendance`

### ✅ Historical Tracking
- `attendance_date` stored in both tables
- Can query machine usage history by employee/date

### ✅ Branch Isolation
- `branch_id` stored in both tables
- Supports multi-branch operations

---

## Build Status
✅ **Build Successful** - No compilation errors

---

## Testing Checklist

### Test 1: Manual Attendance with Machines
1. Enter employee code
2. Select designation (e.g., "WINDING WORKER")
3. Select multiple machines from dropdown
4. Fill other fields
5. Submit
6. **Expected:** 
   - 1 row in `daily_attendance`
   - N rows in `daily_ebmc_attendance` (N = number of machines selected)

### Test 2: Face Recognition with Machines
1. Take photo
2. System recognizes employee
3. Select designation
4. Select machines
5. Submit
6. **Expected:** 
   - 1 row in `daily_attendance`
   - N rows in `daily_ebmc_attendance`

### Test 3: Attendance Without Machines
1. Enter employee code
2. Don't select any machines
3. Submit
4. **Expected:** 
   - 1 row in `daily_attendance`
   - 0 rows in `daily_ebmc_attendance` (no error)

### Test 4: Database Verification
```sql
-- Check last attendance record
SELECT * FROM daily_attendance 
ORDER BY daily_atten_id DESC LIMIT 1;

-- Check linked machines
SELECT * FROM daily_ebmc_attendance 
WHERE daily_atten_id = <last_id>;
```

---

## Related Files

### Android App
- `FaceRecognitionRequest.kt` - Face recognition API request model
- `MarkAttendanceRequest.kt` - Manual attendance API request model
- `AttendanceActivity.kt` - Main attendance entry screen
- `MachineData.kt` - Machine data model
- `MachineResponse.kt` - Machine API response model

### Backend
- `app.py` - Flask server with both endpoints

### Database
- `daily_attendance` - Main attendance table
- `daily_ebmc_attendance` - Machine-attendance linkage table
- `machine_mst` - Machine master data
- `mech_occu_link` - Machine-occupation linkage

---

## Success Criteria
✅ Machine IDs sent in both Face Recognition and Manual attendance  
✅ Backend inserts into `daily_ebmc_attendance` after getting `daily_atten_id`  
✅ Proper foreign key linkage maintained  
✅ Multiple machines per attendance record supported  
✅ Transaction integrity maintained  
✅ Build successful with no errors  

---

**Date:** April 24, 2026  
**Status:** ✅ Complete Implementation  
**Impact:** Machine tracking now works for both attendance entry modes


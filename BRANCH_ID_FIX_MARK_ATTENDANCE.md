# Branch ID Fix for POST /mark-attendance API

## Issue
The `branch_id` parameter was not being sent in the POST `/mark-attendance` API call, which is required by the backend for proper attendance recording and validation.

## Changes Made

### 1. MarkAttendanceRequest.kt
**File:** `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\MarkAttendanceRequest.kt`

**Added `branch_id` field to the data class:**
```kotlin
@SerializedName("branch_id")
val branchId: Int? = null
```

### 2. AttendanceActivity.kt
**File:** `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\AttendanceActivity.kt`

**Updated `markAttendanceManual` function to include `branchId` in the request:**

```kotlin
val request = com.example.slsHrms.api.MarkAttendanceRequest(
    empCode = empCode,
    status = "Manual",
    attType = attType,
    departmentId = deptId,
    shiftId = shiftId,
    designationId = desigId,
    attendanceDate = attendanceDate,
    shiftHours = shiftHours,
    workingHours = workingHours,
    idleHours = idleHours,
    machineIds = if (selectedMachineIds.isNotEmpty()) selectedMachineIds.toList() else null,
    branchId = if (selectedBranchId > 0) selectedBranchId else null  // ✅ ADDED
)
```

## How It Works

1. The `selectedBranchId` is already available in the `AttendanceActivity` (set from intent in `onCreate`)
2. When marking manual attendance, the `branchId` is now included in the API request
3. The backend receives the `branch_id` and uses it for:
   - Validation
   - Recording in the `daily_attendance` table
   - Branch-specific operations

## API Request Example

### Before (Missing branch_id)
```json
{
  "emp_code": "13177",
  "status": "Manual",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 3,
  "attendance_date": "2026-04-24",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

### After (With branch_id) ✅
```json
{
  "emp_code": "13177",
  "status": "Manual",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 3,
  "attendance_date": "2026-04-24",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0,
  "branch_id": 29
}
```

## Build Status
✅ **Build Successful** - No compilation errors

## Testing
To test the fix:
1. Login to the app with a specific branch
2. Mark manual attendance
3. Verify in backend logs that `branch_id` is being sent in the request
4. Verify in database that `branch_id` is correctly saved in `daily_attendance` table

## Related Files
- `/mark-attendance` API endpoint in `app.py`
- `MarkAttendanceRequest.kt` - Request model
- `AttendanceActivity.kt` - Activity that calls the API

---

**Date:** April 24, 2026
**Status:** ✅ Fixed and Build Successful


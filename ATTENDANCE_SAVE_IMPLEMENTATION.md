# Attendance Save Implementation - Complete Parameters

## Date: April 23, 2026

## Overview
The attendance system is now fully configured to save all input parameters to the `daily_attendance` table, including date, shift, department, designation, hours, and attendance type.

## Database Table: `daily_attendance`

The attendance data is saved to the `daily_attendance` table with the following columns:

### Columns Saved:
1. **attendance_date** - Date of attendance (from date picker)
2. **attendance_mark** - Attendance status ('P' for Present)
3. **attendance_source** - Source of entry ('Face' or 'Manual')
4. **attendance_type** - Type (R=Regular, O=OT, C=Cash) - from tab selection
5. **branch_id** - Branch ID (from employee's official details)
6. **eb_id** - Employee Base ID (from employee record)
7. **entry_time** - Timestamp when attendance was marked
8. **idle_hours** - Idle hours (from form input)
9. **is_active** - Active status (always 1)
10. **spell** - Shift name (retrieved from spell_mst using shift_id)
11. **spell_hours** - Shift hours (from form input)
12. **worked_department_id** - Department ID (from department spinner)
13. **worked_designation_id** - Designation ID (from occupation spinner)
14. **working_hours** - Working hours (from form input)
15. **update_date_time** - Last update timestamp

## Mobile App (Android) - AttendanceActivity

### Form Inputs:
- **Date Picker** (tvDate) → attendance_date
- **Department Spinner** → department_id (worked_department_id)
- **Shift Spinner** → shift_id (spell)
- **Shift Hours Input** (etShiftHours) → shift_hours (spell_hours)
- **Occupation Spinner** → designation_id (worked_designation_id)
- **Idle Hours Input** (etIdleHours) → idle_hours
- **Working Hours Input** (etWorkingHours) → working_hours
- **Regular/OT/Cash Tabs** → att_type (R/O/C)
- **Employee Code** (etEmployeeCode) → emp_code

### API Requests:

#### Face Recognition Mode:
```kotlin
FaceRecognitionRequest(
    image = base64Image,
    attType = attType,              // R/O/C
    departmentId = deptId,
    shiftId = shiftId,
    designationId = desigId,
    attendanceDate = attendanceDate, // yyyy-MM-dd
    shiftHours = shiftHours,
    workingHours = workingHours,
    idleHours = idleHours
)
```
Sent to: `POST /attendance`

#### Manual Entry Mode:
```kotlin
MarkAttendanceRequest(
    empCode = empCode,
    status = "Manual",
    attType = attType,              // R/O/C
    departmentId = deptId,
    shiftId = shiftId,
    designationId = desigId,
    attendanceDate = attendanceDate, // yyyy-MM-dd
    shiftHours = shiftHours,
    workingHours = workingHours,
    idleHours = idleHours
)
```
Sent to: `POST /mark-attendance`

## Backend (Flask - app.py)

### Endpoint: POST /attendance (Face Recognition)
```python
@app.route('/attendance', methods=['POST'])
def check_attendance_face():
    # Receives from request:
    data = request.get_json()
    face_image_b64 = data.get('image')
    att_type = data.get('att_type', 'R')
    department_id = data.get('department_id')
    shift_id = data.get('shift_id')
    designation_id = data.get('designation_id')
    shift_hours = data.get('shift_hours', 0)
    working_hours = data.get('working_hours', 0)
    idle_hours = data.get('idle_hours', 0)
    attendance_date_str = data.get('attendance_date')
    
    # Retrieves spell_name from spell_mst:
    spell_name = None
    if shift_id:
        cursor.execute("SELECT spell_name FROM spell_mst WHERE spell_id = %s", (shift_id,))
        spell_row = cursor.fetchone()
        spell_name = spell_row['spell_name'] if spell_row else None
    
    # Inserts to daily_attendance:
    cursor.execute("""
        INSERT INTO daily_attendance (
            attendance_date, attendance_mark, attendance_source, attendance_type,
            branch_id, eb_id, entry_time, idle_hours, is_active,
            spell, spell_hours, worked_department_id, worked_designation_id,
            working_hours, update_date_time
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, (att_date, 'P', 'Face', att_type,
          branch_id, best_match['eb_id'], now, idle_hours, 1,
          spell_name, shift_hours, department_id, designation_id,
          working_hours, now))
```

### Endpoint: POST /mark-attendance (Manual)
```python
@app.route('/mark-attendance', methods=['POST'])
def mark_attendance_manual():
    # Receives from request:
    data = request.get_json()
    emp_code = data.get('emp_code', '').strip()
    status = data.get('status', 'Manual')
    att_type = data.get('att_type', 'R')
    department_id = data.get('department_id')
    shift_id = data.get('shift_id')
    designation_id = data.get('designation_id')
    shift_hours = data.get('shift_hours', 0)
    working_hours = data.get('working_hours', 0)
    idle_hours = data.get('idle_hours', 0)
    attendance_date_str = data.get('attendance_date')
    
    # Retrieves spell_name from spell_mst:
    spell_name = None
    if shift_id:
        cursor.execute("SELECT spell_name FROM spell_mst WHERE spell_id = %s", (shift_id,))
        spell_row = cursor.fetchone()
        spell_name = spell_row['spell_name'] if spell_row else None
    
    # Inserts to daily_attendance:
    cursor.execute("""
        INSERT INTO daily_attendance (
            attendance_date, attendance_mark, attendance_source, attendance_type,
            branch_id, eb_id, entry_time, idle_hours, is_active,
            spell, spell_hours, worked_department_id, worked_designation_id,
            working_hours, update_date_time
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, (att_date, 'P', status, att_type,
          branch_id, employee['eb_id'], now, idle_hours, 1,
          spell_name, shift_hours, department_id, designation_id,
          working_hours, now))
```

## AttendanceSystem Backend Updates (Applied)

### Updated Files:

#### 1. src/attendance/query.py
**Updated INSERT_ATTENDANCE query to include all fields:**
```python
INSERT_ATTENDANCE = """
    INSERT INTO daily_attendance
      (eb_id, emp_code, attendance_date, attendance_time,
       attendance_source, att_type, photo_att,
       attendance_mark, is_active, branch_id,
       spell, spell_hours, worked_department_id, worked_designation_id,
       working_hours, idle_hours, entry_time, update_date_time)
    VALUES (%s, %s, %s, TIME(NOW()), %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
"""
```

#### 2. src/attendance/attendance.py
**Updated mark_attendance (Face) to extract and pass all parameters:**
- Extracts department_id, shift_id, designation_id from request
- Retrieves spell_name from spell_mst using shift_id
- Passes all parameters to INSERT_ATTENDANCE query

**Updated mark_attendance_manual to extract and pass all parameters:**
- Extracts department_id, shift_id, designation_id from request
- Retrieves spell_name from spell_mst using shift_id
- Passes all parameters to INSERT_ATTENDANCE query

#### 3. src/schemas/attendance.py
**Added branch_id to ManualAttendanceSchema:**
```python
class ManualAttendanceSchema(Schema):
    required = ['emp_code']
    optional = ['att_type', 'attendance_date', 'branch_id',
                'department_id', 'designation_id', 'shift_id',
                'shift_hours', 'working_hours', 'idle_hours']
```

## Data Flow Summary

1. **User fills attendance form** on Android app:
   - Selects date, department, shift, occupation
   - Enters shift hours, working hours, idle hours
   - Selects tab (Regular/OT/Cash)
   - Either captures face photo OR enters employee code

2. **Android app sends API request**:
   - If face captured → POST /attendance with image + all parameters
   - If manual entry → POST /mark-attendance with emp_code + all parameters

3. **Backend processes request**:
   - Validates employee (face recognition or code lookup)
   - Retrieves spell_name from spell_mst using shift_id
   - Retrieves branch_id from employee's official details
   - Generates timestamps (entry_time, update_date_time)

4. **Backend saves to daily_attendance table**:
   - All form parameters are saved
   - Includes computed values (spell_name, branch_id, timestamps)
   - Sets attendance_mark='P', is_active=1

## Validation Rules (Android)

Before submitting, the app validates:
1. Employee code entered and verified (for manual mode)
2. Department selected (not default option)
3. Shift selected
4. Occupation selected (not default option)
5. Shift hours > 0
6. Working hours > 0
7. (Working hours - Idle hours) > 0

## Testing

To test the implementation:

1. **Start the backend server:**
   ```bash
   cd E:\sjm\MyHrms
   python app.py
   ```

2. **Build and run the Android app:**
   ```bash
   cd E:\sjm\MyHrms
   .\gradlew assembleDebug
   ```

3. **Mark attendance:**
   - Fill all form fields
   - Select Regular/OT/Cash tab
   - Either take a photo or enter employee code
   - Click Submit

4. **Verify in database:**
   ```sql
   SELECT * FROM daily_attendance 
   ORDER BY entry_time DESC 
   LIMIT 10;
   ```

## Notes

- The system uses `spell_mst` table for shifts (not a `shifts` table)
- Department refers to `sub_dept_mst` table
- Designation/Occupation refers to `designation_mst` table
- `worked_department_id` and `worked_designation_id` store the department/designation where the employee actually worked (which may differ from their assigned department/designation)
- The spell_name is stored as text in the `spell` column for historical record keeping
- All timestamps use server time (NOW())

## Status: ✅ COMPLETE

All attendance input parameters are now being saved to the `daily_attendance` table correctly in both MyHrms and AttendanceSystem backends.


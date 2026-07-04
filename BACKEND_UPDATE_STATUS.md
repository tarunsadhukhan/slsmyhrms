# ⚠️ BACKEND UPDATE STATUS

**Date:** April 24, 2026 9:00 PM  
**Status:** ❌ **BACKEND NOT YET UPDATED** - Manual Update Required

---

## 📍 Backend File Location

**File to update:** `e:\sjm\attendancesystem\src\attendance\attendance.py`  
**Function:** `attendance_report()` (starts at line 345)  
**Current status:** Using OLD implementation

---

## ❌ What's Currently in Backend (OLD)

The backend currently only supports:
```python
@attendance_bp.route('/attendance-report', methods=['GET'])
def attendance_report():
    from_date     = request.args.get('from_date')      # ✅ Has
    to_date       = request.args.get('to_date')        # ✅ Has
    department_id = request.args.get('department_id')  # ✅ Has
    emp_code      = request.args.get('emp_code')       # ✅ Has
    # ❌ Missing: date (single date parameter)
    # ❌ Missing: emp_name (name filter)
    # ❌ Missing: shift_name (spell filter)
```

Response currently returns:
- ✅ id, emp_code, emp_name, department_name, designation_name
- ✅ shift_name, attendance_date, attendance_time
- ✅ status, att_type, shift_hours, working_hours, idle_hours, has_photo
- ❌ **Missing: eb_id**
- ❌ **Missing: machine_nos**

---

## ✅ What Needs to Be Added (NEW)

### 1. Accept Single Date Parameter
```python
# Add support for single date query
attendance_date = request.args.get('date')  # NEW
from_date = request.args.get('from_date')
to_date = request.args.get('to_date')

# Dynamic date condition
if attendance_date:
    query_date_condition = "da.attendance_date = %s"
    date_params = [attendance_date]
elif from_date and to_date:
    query_date_condition = "da.attendance_date BETWEEN %s AND %s"
    date_params = [from_date, to_date]
```

### 2. Add emp_name Filter
```python
emp_name = request.args.get('emp_name', '').strip()  # NEW

# In SQL building
if emp_name:
    sql += " AND (p.first_name LIKE %s OR p.middle_name LIKE %s OR p.last_name LIKE %s)"
    params.extend([f'%{emp_name}%', f'%{emp_name}%', f'%{emp_name}%'])
```

### 3. Add shift_name Filter
```python
shift_name = request.args.get('shift_name', '').strip()  # NEW

# In SQL building
if shift_name and shift_name != 'All Shifts':
    sql += " AND da.spell = %s"
    params.append(shift_name)
```

### 4. Add eb_id to Response
```python
# In SELECT query (check Q.GET_ATTENDANCE_REPORT_BASE)
# Make sure it includes: p.eb_id

# In response
data = [{
    'id': row['id'],
    'emp_code': row['emp_code'],
    'eb_id': row['eb_id'],  # ← ADD THIS
    # ... rest of fields
}]
```

### 5. Add machine_nos to Response
```python
# For each row, fetch machines
for row in rows:
    cursor.execute('''
        SELECT mm.mech_code 
        FROM daily_ebmc_attendance dea
        JOIN machine_mst mm ON dea.mech_id = mm.machine_id
        WHERE dea.daily_atten_id = %s AND dea.is_active = 1
        ORDER BY mm.mech_code
    ''', (row['id'],))
    machine_rows = cursor.fetchall()
    machine_nos = ', '.join([m['mech_code'] or '' for m in machine_rows if m['mech_code']])
    
    data.append({
        # ... existing fields ...
        'machine_nos': machine_nos  # ← ADD THIS
    })
```

---

## 🔧 Step-by-Step Update Instructions

### Step 1: Backup the File
```powershell
cd e:\sjm\attendancesystem\src\attendance
Copy-Item attendance.py attendance_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').py
```

### Step 2: Open the File
```powershell
notepad e:\sjm\attendancesystem\src\attendance\attendance.py
```

### Step 3: Update the Function
**Find:** Line 345 - `def attendance_report():`

**Replace the entire function** with the code from:
`E:\sjm\MyHrms\BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py`

Or manually apply changes:
1. Add `date`, `emp_name`, `shift_name` parameters
2. Add dynamic date condition logic
3. Add filters for emp_name and shift_name
4. Ensure query includes `p.eb_id`
5. Add machine fetching loop
6. Add `eb_id` and `machine_nos` to response

### Step 4: Check Query File
The SQL query is in: `e:\sjm\attendancesystem\src\attendance\queries.py`

Make sure `GET_ATTENDANCE_REPORT_BASE` includes:
```sql
SELECT ..., p.eb_id, ...
```

### Step 5: Restart Server
```powershell
cd e:\sjm\attendancesystem
# Stop current server (Ctrl+C)
python app.py
```

---

## 🧪 Testing After Update

### Test 1: Single Date Query (NEW)
```powershell
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&branch_id=29"
```
**Expected:** Should return data for single date

### Test 2: With Name Filter (NEW)
```powershell
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&emp_name=John&branch_id=29"
```
**Expected:** Should filter by name

### Test 3: With Spell Filter (NEW)
```powershell
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&shift_name=Morning&branch_id=29"
```
**Expected:** Should filter by shift

### Test 4: Date Range Query (EXISTING - Should Still Work)
```powershell
curl "http://192.168.0.223:5051/attendance-report?from_date=2026-04-01&to_date=2026-04-30&department_id=1&branch_id=29"
```
**Expected:** Should return date range data

### Test 5: Response Format (Check NEW Fields)
Response should now include:
```json
{
  "status": "success",
  "data": [{
    "emp_code": "13111",
    "eb_id": 5678,           // ← NEW
    "emp_name": "John Doe",
    "shift_name": "Morning",
    "designation_name": "Operator",
    "machine_nos": "1001, 1002",  // ← NEW
    "working_hours": 8.0
  }]
}
```

---

## ✅ Update Checklist

- [ ] Backup created
- [ ] File opened for editing
- [ ] Added `date` parameter support
- [ ] Added `emp_name` parameter and filter
- [ ] Added `shift_name` parameter and filter
- [ ] Ensured `eb_id` in SELECT query
- [ ] Added machine fetching loop
- [ ] Added `machine_nos` to response
- [ ] Saved file
- [ ] Server restarted
- [ ] Test 1: Single date works
- [ ] Test 2: Name filter works
- [ ] Test 3: Spell filter works
- [ ] Test 4: Date range still works
- [ ] Test 5: Response has eb_id and machine_nos

---

## 📚 Complete Code Reference

**Full updated function code:**
See `E:\sjm\MyHrms\BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py`

**Helper script:**
Run `E:\sjm\MyHrms\apply_backend_updates.ps1`

---

## ⚠️ Important Notes

1. **File is modular:** The backend uses blueprints, so the route is in `src/attendance/attendance.py`, not the main `app.py`

2. **Query file:** SQL queries are in `src/attendance/queries.py` (check if `GET_ATTENDANCE_REPORT_BASE` needs updating)

3. **Both modes:** The updated function should support BOTH:
   - Single date: `?date=2026-04-24`
   - Date range: `?from_date=...&to_date=...`

4. **Backwards compatibility:** Existing Attendance Report page should continue to work

---

**Current Status:** ❌ Backend needs manual update  
**Action Required:** Follow steps above to update backend file  
**Estimated Time:** 10-15 minutes


# Backend Verification - daily_ebmc_attendance Insert Code

## Code Location in app.py

### 1. Face Recognition Endpoint: POST /attendance
**Lines 1055-1070** in `app.py`:

```python
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

---

### 2. Manual Attendance Endpoint: POST /mark-attendance
**Lines 1170-1185** in `app.py`:

```python
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

---

## Verification Commands

### Check Code Exists in File
```powershell
Select-String -Path "E:\sjm\MyHrms\app.py" -Pattern "daily_ebmc_attendance" -Context 2,5
```

### Count Occurrences
```powershell
(Select-String -Path "E:\sjm\MyHrms\app.py" -Pattern "daily_ebmc_attendance" -AllMatches).Count
```

### View Full Context Around Inserts
```powershell
# Face Recognition endpoint (around line 1060)
Get-Content "E:\sjm\MyHrms\app.py" | Select-Object -Skip 1054 -First 25

# Manual Attendance endpoint (around line 1175)
Get-Content "E:\sjm\MyHrms\app.py" | Select-Object -Skip 1169 -First 25
```

---

## How to Restart Backend

### 1. Stop Running Server
```powershell
Get-Process python | Where-Object { $_.Path -like "*python*" } | Stop-Process -Force
```

### 2. Start Server
```powershell
cd E:\sjm\MyHrms
python app.py
```

### 3. Verify Server Started
Look for output:
```
✅ face_recognition loaded
 * Serving Flask app 'app'
 * Running on http://0.0.0.0:5051
```

---

## Test API with Machine IDs

### Test Manual Attendance with Machines
```powershell
$body = @{
    emp_code = "13177"
    status = "Manual"
    att_type = "R"
    department_id = 1
    shift_id = 5
    designation_id = 199
    attendance_date = "2026-04-24"
    shift_hours = 8.0
    working_hours = 8.0
    idle_hours = 0.0
    machine_ids = @(1344, 1345, 1346)
    branch_id = 29
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://192.168.0.223:5051/mark-attendance" -Method POST -Body $body -ContentType "application/json"
```

### Check Database After Test
```sql
-- Check last attendance record
SELECT * FROM daily_attendance 
ORDER BY daily_atten_id DESC LIMIT 1;

-- Get the daily_atten_id from above result, then:
SELECT * FROM daily_ebmc_attendance 
WHERE daily_atten_id = <last_id>;
```

---

## Code Status: ✅ CONFIRMED PRESENT

The `daily_ebmc_attendance` insert code **IS PRESENT** in both endpoints:

1. ✅ **Line 1059-1070**: Face Recognition endpoint `/attendance`
2. ✅ **Line 1173-1185**: Manual Attendance endpoint `/mark-attendance`

Both implementations:
- Get `attendance_id` from `cursor.lastrowid` after inserting to `daily_attendance`
- Loop through `machine_ids` array
- Insert each machine to `daily_ebmc_attendance` with proper foreign key linkage
- Commit the transaction

---

## If Code Still Not Working

### Possible Issues:

1. **Server Not Restarted**
   - Solution: Kill Python processes and restart `python app.py`

2. **Using Old APK**
   - Solution: Rebuild and reinstall APK
   - Command: `.\gradlew assembleDebug`
   - Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`

3. **Table Doesn't Exist**
   - Solution: Create table manually:
   ```sql
   CREATE TABLE IF NOT EXISTS daily_ebmc_attendance (
       id INT AUTO_INCREMENT PRIMARY KEY,
       daily_atten_id INT NOT NULL,
       eb_id INT NOT NULL,
       mech_id INT NOT NULL,
       attendance_date DATE NOT NULL,
       branch_id INT,
       is_active TINYINT DEFAULT 1,
       update_date_time DATETIME,
       KEY idx_daily_atten (daily_atten_id),
       KEY idx_eb_date (eb_id, attendance_date)
   );
   ```

4. **Database Connection Issue**
   - Check `DB_CONFIG` in `app.py` (line 62-67)
   - Verify database credentials

5. **Machine IDs Not Sent from App**
   - Rebuild APK with latest code
   - Verify `selectedMachineIds` is not empty before submit

---

## Quick Debug Steps

1. **View actual code in file:**
   ```powershell
   code E:\sjm\MyHrms\app.py
   ```
   - Go to line 1059 (Face Recognition)
   - Go to line 1173 (Manual Attendance)
   - Verify code is present

2. **Check if changes were saved:**
   ```powershell
   (Get-Item "E:\sjm\MyHrms\app.py").LastWriteTime
   ```

3. **Restart backend server:**
   - Stop all Python processes
   - Run `python app.py` in terminal
   - Watch for any error messages

4. **Test with cURL:**
   ```bash
   curl -X POST http://192.168.0.223:5051/mark-attendance \
     -H "Content-Type: application/json" \
     -d '{"emp_code":"13177","status":"Manual","att_type":"R","machine_ids":[1344],"branch_id":29}'
   ```

---

**Verified:** April 24, 2026  
**Status:** ✅ Code is present in both endpoints  
**Lines:** 1059-1070 (Face), 1173-1185 (Manual)


# Mark Attendance API Reference

## 📋 Overview

The **mark-attendance** API is used for **manual attendance entry** where the user enters an employee code (without using face recognition). This endpoint validates the employee, retrieves their information, and saves the attendance record to the database.

---

## 🔗 API Endpoint

```
POST /mark-attendance
```

**Base URL:** `http://YOUR_SERVER_IP:5051`


**Full URL Example:** `http://192.168.0.223:5051/mark-attendance`

---

## 📤 Request

### Headers
```http
Content-Type: application/json
```

### Method
```
POST
```

### Request Body (JSON Payload)

```json
{
  "emp_code": "13177",
  "status": "Manual",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 3,
  "attendance_date": "2026-04-23",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

---

## 📋 Payload Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `emp_code` | String | ✅ Yes | Employee code (unique identifier) | `"13177"` |
| `status` | String | ✅ Yes | Source of attendance entry | `"Manual"` or `"Face"` |
| `att_type` | String | ✅ Yes | Type of attendance | `"R"` (Regular), `"O"` (OT), `"C"` (Cash) |
| `department_id` | Integer | ⚠️ Optional | Department ID where employee worked | `1` |
| `shift_id` | Integer | ⚠️ Optional | Shift ID (spell_id from spell_mst table) | `5` |
| `designation_id` | Integer | ⚠️ Optional | Designation ID (occupation) | `3` |
| `attendance_date` | String | ⚠️ Optional | Date of attendance (YYYY-MM-DD format) | `"2026-04-23"` |
| `shift_hours` | Double | ⚠️ Optional | Total shift duration in hours | `8.0` |
| `working_hours` | Double | ⚠️ Optional | Actual working hours | `8.0` |
| `idle_hours` | Double | ⚠️ Optional | Idle/break time in hours | `0.0` |

### Parameter Details

#### `emp_code` (Required)
- **Type:** String
- **Validation:** Must exist in `hrms_ed_personal_details` table
- **Validation:** Employee must be active (`active = 1`)
- **Example:** `"13177"`, `"EMP001"`, `"12345"`

#### `status` (Required)
- **Type:** String
- **Default:** `"Manual"`
- **Values:** 
  - `"Manual"` - Manual attendance entry by admin/HR
  - `"Face"` - Face recognition attendance (use `/attendance` endpoint instead)
- **Saved as:** `attendance_source` in database

#### `att_type` (Required)
- **Type:** String (1 character)
- **Default:** `"R"`
- **Values:**
  - `"R"` - Regular attendance
  - `"O"` - Overtime (OT) attendance
  - `"C"` - Cash payment attendance
- **Saved as:** `attendance_type` in database

#### `department_id` (Optional)
- **Type:** Integer
- **Description:** Sub-department ID where employee worked
- **Reference Table:** `sub_dept_mst`
- **Saved as:** `worked_department_id` in database
- **Example:** `1`, `5`, `12`

#### `shift_id` (Optional)
- **Type:** Integer
- **Description:** Shift/spell ID from shift master
- **Reference Table:** `spell_mst` (spell_id)
- **Note:** System retrieves `spell_name` and stores it in `spell` column
- **Example:** `1`, `2`, `5`

#### `designation_id` (Optional)
- **Type:** Integer
- **Description:** Designation/occupation ID
- **Reference Table:** `designation_mst`
- **Saved as:** `worked_designation_id` in database
- **Example:** `3`, `7`, `15`

#### `attendance_date` (Optional)
- **Type:** String (ISO 8601 date format)
- **Format:** `YYYY-MM-DD`
- **Default:** Current date if not provided
- **Validation:** Cannot be future date (validated in mobile app)
- **Example:** `"2026-04-23"`, `"2026-04-22"`

#### `shift_hours` (Optional)
- **Type:** Double/Number
- **Description:** Total scheduled shift duration
- **Default:** `0` if not provided
- **Validation:** Must be > 0 (validated in mobile app)
- **Saved as:** `spell_hours` in database
- **Example:** `8.0`, `10.0`, `12.0`

#### `working_hours` (Optional)
- **Type:** Double/Number
- **Description:** Actual hours worked
- **Default:** `0` if not provided
- **Validation:** Must be > 0 (validated in mobile app)
- **Saved as:** `working_hours` in database
- **Example:** `8.0`, `7.5`, `9.0`

#### `idle_hours` (Optional)
- **Type:** Double/Number
- **Description:** Break/idle time in hours
- **Default:** `0` if not provided
- **Validation:** `working_hours - idle_hours` must be > 0
- **Saved as:** `idle_hours` in database
- **Example:** `0.0`, `0.5`, `1.0`

---

## 📥 Response

### Success Response (200 OK)

```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src=\"data:image/jpeg;base64,/9j/4AAQSkZJRg...\" />",
  "message": "Attendance marked for John Michael Doe (Manual)"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | `"success"` if attendance marked successfully |
| `emp_code` | String | Employee code that was processed |
| `emp_name` | String | Full name of employee (first + middle + last) |
| `photo_html` | String | Employee photo as HTML img tag with base64 data (can be null) |
| `message` | String | Success message with employee name |

---

### Error Responses

#### 400 Bad Request - Missing Employee Code
```json
{
  "status": "error",
  "message": "Employee code is required"
}
```

#### 404 Not Found - Employee Not Found
```json
{
  "status": "error",
  "message": "Employee 13177 not found or inactive"
}
```

#### 500 Internal Server Error
```json
{
  "status": "error",
  "message": "Error message describing the issue"
}
```

---

## 💾 Database Operations

### Tables Involved

#### 1. **hrms_ed_personal_details** (Employee Lookup)
- Used to verify employee exists and is active
- Retrieves: `eb_id`, `emp_code`, `first_name`, `middle_name`, `last_name`

#### 2. **hrms_ed_official_details** (Branch Info)
- Retrieves: `branch_id` for attendance record

#### 3. **employee_face_mst** (Photo)
- Retrieves: `photo_html` for response

#### 4. **spell_mst** (Shift Name)
- Retrieves: `spell_name` using `shift_id`
- Maps to `spell` column in attendance

#### 5. **daily_attendance** (Insert Record)
- Primary table where attendance is saved

### Data Saved to `daily_attendance` Table

| Column | Source | Value | Description |
|--------|--------|-------|-------------|
| `attendance_date` | Payload | `"2026-04-23"` | Date of attendance |
| `attendance_mark` | Fixed | `"P"` | Always "P" (Present) |
| `attendance_source` | Payload | `"Manual"` | Source (Manual or Face) |
| `attendance_type` | Payload | `"R"/"O"/"C"` | Type of attendance |
| `branch_id` | Employee | From employee record | Employee's branch |
| `eb_id` | Employee | From employee record | Employee base ID |
| `entry_time` | System | Current datetime | When record created |
| `idle_hours` | Payload | `0.0` | Idle/break hours |
| `is_active` | Fixed | `1` | Active record flag |
| `spell` | Lookup | `"Morning Shift"` | Shift name from spell_mst |
| `spell_hours` | Payload | `8.0` | Shift duration |
| `worked_department_id` | Payload | `1` | Department ID |
| `worked_designation_id` | Payload | `3` | Designation ID |
| `working_hours` | Payload | `8.0` | Working hours |
| `update_date_time` | System | Current datetime | Last update time |

---

## 🔄 Complete Request/Response Example

### cURL Example
```bash
curl -X POST http://192.168.0.223:5051/mark-attendance \
  -H "Content-Type: application/json" \
  -d '{
    "emp_code": "13177",
    "status": "Manual",
    "att_type": "R",
    "department_id": 1,
    "shift_id": 5,
    "designation_id": 3,
    "attendance_date": "2026-04-23",
    "shift_hours": 8.0,
    "working_hours": 8.0,
    "idle_hours": 0.0
  }'
```

### PowerShell Example
```powershell
$body = @{
    emp_code = "13177"
    status = "Manual"
    att_type = "R"
    department_id = 1
    shift_id = 5
    designation_id = 3
    attendance_date = "2026-04-23"
    shift_hours = 8.0
    working_hours = 8.0
    idle_hours = 0.0
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://192.168.0.223:5051/mark-attendance" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### JavaScript/Fetch Example
```javascript
fetch('http://192.168.0.223:5051/mark-attendance', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    emp_code: '13177',
    status: 'Manual',
    att_type: 'R',
    department_id: 1,
    shift_id: 5,
    designation_id: 3,
    attendance_date: '2026-04-23',
    shift_hours: 8.0,
    working_hours: 8.0,
    idle_hours: 0.0
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

### Python Example
```python
import requests
import json

url = "http://192.168.0.223:5051/mark-attendance"
headers = {"Content-Type": "application/json"}
payload = {
    "emp_code": "13177",
    "status": "Manual",
    "att_type": "R",
    "department_id": 1,
    "shift_id": 5,
    "designation_id": 3,
    "attendance_date": "2026-04-23",
    "shift_hours": 8.0,
    "working_hours": 8.0,
    "idle_hours": 0.0
}

response = requests.post(url, headers=headers, data=json.dumps(payload))
print(response.json())
```

---

## 🔍 Validation Rules

### Backend Validation (Flask)
1. ✅ **emp_code is required** - Returns 400 if missing
2. ✅ **Employee must exist** - Returns 404 if not found
3. ✅ **Employee must be active** - Returns 404 if inactive
4. ✅ **Shift ID must exist** - Retrieves spell_name or stores NULL

### Frontend Validation (Android App)
1. ✅ **Employee code required** (manual mode only)
2. ✅ **Employee verification required** (must click Check ✓)
3. ✅ **Department must be selected**
4. ✅ **Shift must be selected**
5. ✅ **Occupation must be selected**
6. ✅ **Shift hours > 0**
7. ✅ **Working hours > 0**
8. ✅ **Working hours - idle hours > 0**
9. ✅ **Date cannot be in future**

---

## 🆚 Comparison: mark-attendance vs attendance

| Feature | `/mark-attendance` | `/attendance` |
|---------|-------------------|---------------|
| **Method** | POST | POST |
| **Input** | Employee code | Face image (base64) |
| **Use Case** | Manual entry | Face recognition |
| **Photo Saved** | ❌ No | ✅ Yes (in `photo_att`) |
| **Status** | "Manual" | "Face" |
| **Employee Verification** | By code lookup | By face matching |
| **Required Fields** | emp_code | image |

---

## 📊 Database Query Flow

```
1. Receive POST request with JSON payload
   ↓
2. Extract emp_code from payload
   ↓
3. Query hrms_ed_personal_details + hrms_ed_official_details
   - Verify employee exists
   - Verify employee is active
   - Get eb_id, branch_id, name
   ↓
4. Get employee photo from employee_face_mst (optional)
   ↓
5. Get shift name from spell_mst using shift_id
   ↓
6. Insert attendance record into daily_attendance
   - attendance_date, attendance_mark='P'
   - attendance_source='Manual', attendance_type
   - branch_id, eb_id, entry_time
   - spell, spell_hours
   - worked_department_id, worked_designation_id
   - working_hours, idle_hours
   ↓
7. Return success response with employee details
```

---

## 🧪 Testing

### Test Case 1: Valid Manual Attendance
**Request:**
```json
{
  "emp_code": "13177",
  "status": "Manual",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 3,
  "attendance_date": "2026-04-23",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

**Expected Response:** `200 OK`
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "message": "Attendance marked for John Michael Doe (Manual)"
}
```

### Test Case 2: Missing Employee Code
**Request:**
```json
{
  "status": "Manual",
  "att_type": "R"
}
```

**Expected Response:** `400 Bad Request`
```json
{
  "status": "error",
  "message": "Employee code is required"
}
```

### Test Case 3: Invalid Employee Code
**Request:**
```json
{
  "emp_code": "INVALID999",
  "status": "Manual",
  "att_type": "R"
}
```

**Expected Response:** `404 Not Found`
```json
{
  "status": "error",
  "message": "Employee INVALID999 not found or inactive"
}
```

---

## 🔐 Security Considerations

### Current Implementation
- ✅ Employee validation (must exist and be active)
- ✅ SQL injection prevention (parameterized queries)
- ✅ Input sanitization (trim, type validation)

### Recommended for Production
- ⚠️ **Authentication:** Add JWT token or API key
- ⚠️ **Authorization:** Verify user has permission to mark attendance
- ⚠️ **Rate Limiting:** Prevent abuse
- ⚠️ **HTTPS:** Encrypt data in transit
- ⚠️ **Audit Logging:** Track who marked attendance

---

## 📝 Notes

1. **Date Format:** Always use `YYYY-MM-DD` format for `attendance_date`
2. **Time Zone:** Server uses local time for `entry_time` and `update_date_time`
3. **Attendance Mark:** Always set to `"P"` (Present) automatically
4. **Status Field:** Use `"Manual"` for this endpoint (not `"Face"`)
5. **Photo Att:** No photo saved with manual attendance (photo_att = NULL)
6. **Multiple Entries:** System allows multiple attendance entries per day

---

## 🔗 Related Endpoints

- **POST /attendance** - Mark attendance with face recognition
- **POST /check-face** - Identify employee by face (no attendance saved)
- **GET /employee/{emp_code}** - Get employee details by code (supports optional `?branch_id=29`)
- **GET /employees** - Get all employees
- **GET /attendance-report** - Get attendance reports

---

## 📞 Support

For issues or questions:
- Check [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for troubleshooting
- Review [COMPLETE_TESTING_GUIDE.md](COMPLETE_TESTING_GUIDE.md) for test procedures
- See [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for quick commands

---

**Document:** MARK_ATTENDANCE_API_REFERENCE.md  
**Version:** 1.0  
**Last Updated:** April 23, 2026  
**Status:** ✅ Complete


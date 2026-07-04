# Complete API Reference with cURL Commands

## MyHrms - All API Endpoints with Payload Examples

**Base URL:** `http://localhost:5051` or `http://YOUR_SERVER_IP:5051`

---

## 📋 Table of Contents

1. [Authentication APIs](#1-authentication-apis)
2. [Employee APIs](#2-employee-apis)
3. [Attendance APIs](#3-attendance-apis)
4. [Master Data APIs](#4-master-data-apis)
5. [Dashboard APIs](#5-dashboard-apis)

---

## 1. Authentication APIs

### POST /login
**Description:** User login

**cURL Command:**
```bash
curl -X POST http://localhost:5051/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**Request Payload:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "status": "success",
  "user_id": 1,
  "username": "admin",
  "message": "Login successful"
}
```

---

## 2. Employee APIs

### GET /employees
**Description:** Get all active employees

**cURL Command:**
```bash
curl -X GET http://localhost:5051/employees
```

**No Payload Required**

**Success Response (200):**
```json
{
  "status": "success",
  "employees": [
    {
      "id": 1,
      "emp_code": "13177",
      "name": "John Michael Doe",
      "department_id": 1,
      "designation_id": 3,
      "branch_id": 29,
      "photo_html": "<img src='data:image/jpeg;base64,...' />",
      "is_active": 1,
      "created_at": "2026-04-23 10:30:00"
    }
  ]
}
```

---

### GET /employee/{emp_code}
**Description:** Get employee by employee code with optional branch filter

**cURL Command:**
```bash
# Without branch filter
curl -X GET http://localhost:5051/employee/13177

# With branch filter (recommended)
curl -X GET "http://localhost:5051/employee/13177?branch_id=29"
```

**URL Parameters:**
- `emp_code` (required) - Employee code (e.g., 13177)

**Query Parameters:**
- `branch_id` (optional) - Filter by branch ID

**No Request Payload**

**Success Response (200):**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src='data:image/jpeg;base64,/9j/4AAQSkZJRg...' />",
  "branch_id": 29,
  "message": "Employee found: John Michael Doe"
}
```

**Error Response (404):**
```json
{
  "status": "error",
  "message": "Employee with code 13177 not found or inactive"
}
```

**Error Response (404 with branch filter):**
```json
{
  "status": "error",
  "message": "Employee with code 13177 in branch 29 not found or inactive"
}
```

**PowerShell Example:**
```powershell
# Without branch filter
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177" -Method GET

# With branch filter (recommended)
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=29" -Method GET
```

---

### GET /employees/search
**Description:** Search employees by name or code

**cURL Command:**
```bash
curl -X GET "http://localhost:5051/employees/search?q=john"
```

**Query Parameters:**
- `q` (required) - Search query (partial match for name or emp_code)

**Success Response (200):**
```json
{
  "status": "success",
  "employees": [
    {
      "id": 1,
      "emp_code": "13177",
      "name": "John Michael Doe",
      "photo_html": "<img src='...' />",
      "department_id": 1,
      "designation_id": 3,
      "shift_id": 5
    }
  ]
}
```

---

### POST /register
**Description:** Register new employee with face

**cURL Command:**
```bash
curl -X POST http://localhost:5051/register \
  -H "Content-Type: application/json" \
  -d '{
    "emp_code": "EMP001",
    "name": "Jane Smith",
    "department_id": 1,
    "designation_id": 3,
    "shift_id": 5,
    "image": "BASE64_ENCODED_IMAGE_STRING"
  }'
```

**Request Payload:**
```json
{
  "emp_code": "EMP001",
  "name": "Jane Smith",
  "department_id": 1,
  "designation_id": 3,
  "shift_id": 5,
  "image": "/9j/4AAQSkZJRg... (base64 image)"
}
```

**Success Response (200):**
```json
{
  "status": "success",
  "emp_code": "EMP001",
  "message": "Employee registered successfully"
}
```

---

## 3. Attendance APIs

### POST /mark-attendance (Manual Entry)
**Description:** Mark attendance manually using employee code

**cURL Command:**
```bash
curl -X POST http://localhost:5051/mark-attendance \
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

**Request Payload:**
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

**Payload Parameters:**
- `emp_code` (required, string) - Employee code
- `status` (required, string) - "Manual" or "Face"
- `att_type` (required, string) - "R" (Regular), "O" (OT), "C" (Cash)
- `department_id` (optional, integer) - Department ID
- `shift_id` (optional, integer) - Shift ID
- `designation_id` (optional, integer) - Designation ID
- `attendance_date` (optional, string) - Date in YYYY-MM-DD format
- `shift_hours` (optional, float) - Shift duration in hours
- `working_hours` (optional, float) - Working hours
- `idle_hours` (optional, float) - Idle hours

**Success Response (200):**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src='data:image/jpeg;base64,...' />",
  "message": "Attendance marked for John Michael Doe (Manual)"
}
```

**Error Response (400):**
```json
{
  "status": "error",
  "message": "Employee code is required"
}
```

**Error Response (404):**
```json
{
  "status": "error",
  "message": "Employee 13177 not found or inactive"
}
```

**PowerShell Example:**
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

Invoke-RestMethod -Uri "http://localhost:5051/mark-attendance" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

**Python Example:**
```python
import requests
import json

url = "http://localhost:5051/mark-attendance"
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

### POST /attendance (Face Recognition)
**Description:** Mark attendance using face recognition

**cURL Command:**
```bash
curl -X POST http://localhost:5051/attendance \
  -H "Content-Type: application/json" \
  -d '{
    "image": "BASE64_ENCODED_IMAGE_STRING",
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

**Request Payload:**
```json
{
  "image": "/9j/4AAQSkZJRg... (base64 encoded image)",
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

**Payload Parameters:**
- `image` (required, string) - Base64 encoded face image
- `att_type` (required, string) - "R", "O", or "C"
- `department_id` (optional, integer)
- `shift_id` (optional, integer)
- `designation_id` (optional, integer)
- `attendance_date` (optional, string) - YYYY-MM-DD
- `shift_hours` (optional, float)
- `working_hours` (optional, float)
- `idle_hours` (optional, float)

**Success Response (200):**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src='...' />",
  "message": "Attendance marked for John Michael Doe"
}
```

**Error Response (400):**
```json
{
  "status": "error",
  "message": "No face detected in the image"
}
```

---

### POST /check-face
**Description:** Identify employee by face (no attendance saved)

**cURL Command:**
```bash
curl -X POST http://localhost:5051/check-face \
  -H "Content-Type: application/json" \
  -d '{
    "image": "BASE64_ENCODED_IMAGE_STRING"
  }'
```

**Request Payload:**
```json
{
  "image": "/9j/4AAQSkZJRg... (base64 encoded image)"
}
```

**Success Response (200):**
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src='...' />",
  "message": "Face matched: John Michael Doe"
}
```

---

### GET /attendance-report
**Description:** Get attendance report for a date range

**cURL Command:**
```bash
curl -X GET "http://localhost:5051/attendance-report?from_date=2026-04-01&to_date=2026-04-23"
```

**Query Parameters:**
- `from_date` (required) - Start date (YYYY-MM-DD)
- `to_date` (required) - End date (YYYY-MM-DD)
- `branch_id` (optional) - Filter by branch
- `emp_code` (optional) - Filter by employee code
- `department_id` (optional) - Filter by department

**Success Response (200):**
```json
{
  "status": "success",
  "data": [
    {
      "id": 123,
      "emp_code": "13177",
      "emp_name": "John Michael Doe",
      "department_name": "Production",
      "designation_name": "Operator",
      "shift_name": "Morning Shift",
      "attendance_date": "2026-04-23",
      "attendance_time": "09:15:30",
      "status": "Manual",
      "att_type": "R",
      "shift_hours": 8.0,
      "working_hours": 8.0,
      "idle_hours": 0.0
    }
  ],
  "total": 1
}
```

---

### GET /attendance/{attendance_id}
**Description:** Get single attendance record details for editing

**cURL Command:**
```bash
curl -X GET http://localhost:5051/attendance/123
```

**URL Parameters:**
- `attendance_id` (required) - Attendance record ID

**Success Response (200):**
```json
{
  "status": "success",
  "data": {
    "id": 123,
    "eb_id": 456,
    "emp_code": "13177",
    "emp_name": "John Michael Doe",
    "attendance_date": "2026-04-23",
    "att_type": "R",
    "status": "Manual",
    "department_id": 1,
    "designation_id": 3,
    "shift_id": 5,
    "shift_name": "Morning Shift",
    "shift_hours": 8.0,
    "working_hours": 8.0,
    "idle_hours": 0.0,
    "branch_id": 29,
    "photo_html": null
  }
}
```

**Error Response (404):**
```json
{
  "status": "error",
  "message": "Attendance record 123 not found"
}
```

---

### PUT /attendance/{attendance_id}
**Description:** Update an existing attendance record

**cURL Command:**
```bash
curl -X PUT http://localhost:5051/attendance/123 \
  -H "Content-Type: application/json" \
  -d '{
    "emp_code": "13177",
    "attendance_date": "2026-04-23",
    "att_type": "R",
    "department_id": 1,
    "shift_id": 5,
    "designation_id": 3,
    "shift_hours": 8.0,
    "working_hours": 8.0,
    "idle_hours": 0.0
  }'
```

**URL Parameters:**
- `attendance_id` (required) - Attendance record ID to update

**Request Payload:**
```json
{
  "emp_code": "13177",
  "attendance_date": "2026-04-23",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 5,
  "designation_id": 3,
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

**Payload Parameters:**
- `emp_code` (required, string) - Employee code
- `attendance_date` (required, string) - Date in YYYY-MM-DD format
- `att_type` (required, string) - "R" (Regular), "O" (OT), "C" (Cash)
- `department_id` (optional, integer) - Department ID
- `shift_id` (optional, integer) - Shift ID
- `designation_id` (optional, integer) - Designation ID
- `shift_hours` (optional, float) - Shift duration in hours
- `working_hours` (optional, float) - Working hours
- `idle_hours` (optional, float) - Idle hours

**Success Response (200):**
```json
{
  "status": "success",
  "message": "Attendance record updated successfully",
  "attendance_id": 123
}
```

**Error Response (404):**
```json
{
  "status": "error",
  "message": "Attendance record 123 not found"
}
```

**PowerShell Example:**
```powershell
$body = @{
    emp_code = "13177"
    attendance_date = "2026-04-23"
    att_type = "R"
    department_id = 1
    shift_id = 5
    designation_id = 3
    shift_hours = 8.0
    working_hours = 8.0
    idle_hours = 0.0
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5051/attendance/123" `
    -Method PUT `
    -ContentType "application/json" `
    -Body $body
```

---

## 4. Master Data APIs

### GET /departments
**Description:** Get all departments

**cURL Command:**
```bash
curl -X GET http://localhost:5051/departments
```

**With Filters:**
```bash
curl -X GET "http://localhost:5051/departments?co_id=1&branch_id=29"
```

**Query Parameters:**
- `co_id` (optional) - Company ID
- `branch_id` (optional) - Branch ID

**Success Response (200):**
```json
{
  "status": "success",
  "departments": [
    {
      "id": 1,
      "name": "Production",
      "dept_id": 5,
      "branch_id": 29
    },
    {
      "id": 2,
      "name": "Quality Control",
      "dept_id": 6,
      "branch_id": 29
    }
  ]
}
```

**PowerShell Example:**
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/departments?branch_id=29" -Method GET
```

---

### POST /departments
**Description:** Create new department

**cURL Command:**
```bash
curl -X POST http://localhost:5051/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HR Department",
    "branch_id": 29
  }'
```

**Request Payload:**
```json
{
  "name": "HR Department",
  "branch_id": 29
}
```

---

### GET /shifts
**Description:** Get all shifts

**cURL Command:**
```bash
curl -X GET http://localhost:5051/shifts
```

**With Filter:**
```bash
curl -X GET "http://localhost:5051/shifts?branch_id=29"
```

**Query Parameters:**
- `branch_id` (optional) - Filter by branch

**Success Response (200):**
```json
{
  "status": "success",
  "shifts": [
    {
      "id": 1,
      "spell_id": 5,
      "name": "Morning Shift",
      "shift_hours": 8.0,
      "branch_id": 29
    },
    {
      "id": 2,
      "spell_id": 6,
      "name": "Evening Shift",
      "shift_hours": 8.0,
      "branch_id": 29
    }
  ]
}
```

---

### POST /shifts
**Description:** Create new shift

**cURL Command:**
```bash
curl -X POST http://localhost:5051/shifts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Night Shift",
    "shift_hours": 8.0,
    "branch_id": 29
  }'
```

---

### GET /designations
**Description:** Get designations (occupations)

**cURL Command:**
```bash
curl -X GET "http://localhost:5051/designations?branch_id=29"
```

**Query Parameters:**
- `branch_id` (required) - Branch ID
- `sub_dept_id` (optional) - Department ID

**With Department Filter:**
```bash
curl -X GET "http://localhost:5051/designations?branch_id=29&sub_dept_id=1"
```

**Success Response (200):**
```json
{
  "status": "success",
  "designations": [
    {
      "id": 1,
      "name": "Operator"
    },
    {
      "id": 2,
      "name": "Supervisor"
    },
    {
      "id": 3,
      "name": "Manager"
    }
  ]
}
```

**Error Response (400):**
```json
{
  "status": "error",
  "message": "branch_id is required"
}
```

---

### GET /occupations
**Description:** Get occupations list

**cURL Command:**
```bash
curl -X GET http://localhost:5051/occupations
```

**Success Response (200):**
```json
{
  "status": "success",
  "occupations": [
    {
      "id": 1,
      "name": "Operator"
    }
  ]
}
```

---

### POST /occupations
**Description:** Create new occupation

**cURL Command:**
```bash
curl -X POST http://localhost:5051/occupations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Technician"
  }'
```

---

## 5. Dashboard APIs

### GET /dashboard-stats
**Description:** Get dashboard statistics

**cURL Command:**
```bash
curl -X GET "http://localhost:5051/dashboard-stats?date=2026-04-23&branch_id=29"
```

**Query Parameters:**
- `date` (optional) - Date (YYYY-MM-DD), defaults to today
- `branch_id` (optional) - Filter by branch
- `co_id` (optional) - Filter by company

**Success Response (200):**
```json
{
  "status": "success",
  "date": "2026-04-23",
  "stats": {
    "total_departments": 15,
    "total_designations": 25,
    "total_shifts": 3,
    "total_employees": 150,
    "present_today": 145,
    "absent_today": 5,
    "attendance_percentage": 96.67
  }
}
```

---

## 🔧 Common cURL Options

### Basic Authentication (if implemented)
```bash
curl -X GET http://localhost:5051/employees \
  -u username:password
```

### Bearer Token (if implemented)
```bash
curl -X GET http://localhost:5051/employees \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Verbose Output (debugging)
```bash
curl -v -X GET http://localhost:5051/employee/13177
```

### Save Response to File
```bash
curl -X GET http://localhost:5051/employees -o employees.json
```

### Pretty Print JSON Response (with jq)
```bash
curl -X GET http://localhost:5051/employee/13177 | jq
```

---

## 📊 Testing Workflow

### 1. Test Server Health
```bash
curl -X GET http://localhost:5051/
```

### 2. Get Employee Info
```bash
curl -X GET http://localhost:5051/employee/13177
```

### 3. Mark Manual Attendance
```bash
curl -X POST http://localhost:5051/mark-attendance \
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

### 4. Get Attendance Report
```bash
curl -X GET "http://localhost:5051/attendance-report?start_date=2026-04-23&end_date=2026-04-23"
```

---

## 🐛 Troubleshooting

### Connection Refused
```bash
# Check if server is running
curl http://localhost:5051/

# If fails, start server:
cd E:\sjm\MyHrms
python app.py
```

### 400 Bad Request
- Check JSON syntax
- Ensure Content-Type header is set
- Verify all required fields are present

### 404 Not Found
- Verify endpoint URL is correct
- Check employee code exists

### 500 Internal Server Error
- Check server logs
- Verify database connection
- Check if all required services are running

---

## 📝 Quick Reference Card

### Most Used APIs

| API | Method | Quick Command |
|-----|--------|---------------|
| Get Employee | GET | `curl http://localhost:5051/employee/13177` |
| Mark Attendance | POST | `curl -X POST http://localhost:5051/mark-attendance -H "Content-Type: application/json" -d '{"emp_code":"13177","status":"Manual","att_type":"R"}'` |
| Get Departments | GET | `curl http://localhost:5051/departments` |
| Get Shifts | GET | `curl http://localhost:5051/shifts` |
| Get Designations | GET | `curl "http://localhost:5051/designations?branch_id=29"` |
| Get Employees | GET | `curl http://localhost:5051/employees` |

---

## 🔐 Security Notes

### Production Recommendations:
1. Use HTTPS instead of HTTP
2. Implement API authentication (JWT/OAuth)
3. Add rate limiting
4. Validate all inputs server-side
5. Use environment variables for sensitive data

---

## 📞 Support

For issues or questions:
- Check server logs: `python app.py` (console output)
- Review API documentation: `MARK_ATTENDANCE_API_REFERENCE.md`
- Test with verbose mode: `curl -v ...`

---

**Document:** ALL_APIs_CURL_REFERENCE.md  
**Version:** 1.0  
**Last Updated:** April 23, 2026  
**Total APIs Documented:** 20+  
**Status:** ✅ Complete


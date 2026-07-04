# Quick API Testing Cheat Sheet

## 🚀 Copy-Paste Ready Commands for Windows PowerShell

**Replace `localhost` with your server IP if needed (e.g., `192.168.0.223`)**

---

## ✅ Most Important APIs (PowerShell)

### 1. GET Employee by Code
**Without branch filter:**
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177" -Method GET
```

**With branch filter (recommended):**
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=29" -Method GET
```

### 2. POST Mark Attendance (Manual)
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

Invoke-RestMethod -Uri "http://localhost:5051/mark-attendance" -Method POST -ContentType "application/json" -Body $body
```

### 3. GET All Employees
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/employees" -Method GET
```

### 4. GET Departments
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/departments" -Method GET
```

### 5. GET Shifts (with branch filter)
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/shifts?branch_id=29" -Method GET
```

### 6. GET Designations (requires branch_id)
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/designations?branch_id=29" -Method GET
```

---

## 🔧 For Linux/Mac (Actual cURL)

### GET Employee
**Without branch filter:**
```bash
curl http://localhost:5051/employee/13177
```

**With branch filter (recommended):**
```bash
curl "http://localhost:5051/employee/13177?branch_id=29"
```

### POST Mark Attendance
```bash
curl -X POST http://localhost:5051/mark-attendance \
  -H "Content-Type: application/json" \
  -d '{"emp_code":"13177","status":"Manual","att_type":"R","department_id":1,"shift_id":5,"designation_id":3,"attendance_date":"2026-04-23","shift_hours":8.0,"working_hours":8.0,"idle_hours":0.0}'
```

---

## 📋 Full Command Reference

See complete documentation: **`ALL_APIs_CURL_REFERENCE.md`**

---

## ⚠️ Common Issues

### Issue: "branch_id is required" error
**Solution:** Make sure you're hitting the correct endpoint. Some APIs require query parameters.

### Issue: PowerShell curl doesn't work
**Solution:** Use `Invoke-RestMethod` or `Invoke-WebRequest` instead. PowerShell's `curl` is an alias that works differently.

---

**Location:** `E:\sjm\MyHrms\QUICK_API_CHEAT_SHEET.md`
**Last Updated:** April 23, 2026


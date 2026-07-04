# GET /employee API Update - Branch Validation Added

## Date: April 23, 2026

## 🎯 Update Summary

The `GET /employee/{emp_code}` API has been updated to support **optional branch_id validation**.

---

## ✅ Changes Made

### 1. Backend API Updated (`app.py`)
**File:** `E:\sjm\MyHrms\app.py` (lines 755-810)

**Added Feature:**
- Optional `branch_id` query parameter
- Filters employee lookup by branch
- Returns branch_id in response

**Before:**
```python
@app.route('/employee/<emp_code>', methods=['GET'])
def get_employee_by_code(emp_code):
    # Only searched by emp_code
```

**After:**
```python
@app.route('/employee/<emp_code>', methods=['GET'])
def get_employee_by_code(emp_code):
    # Accepts optional ?branch_id=29 parameter
    # Validates employee belongs to specified branch
```

---

### 2. Documentation Updated

#### Files Updated:
1. ✅ `QUICK_API_CHEAT_SHEET.md`
2. ✅ `ALL_APIs_CURL_REFERENCE.md`
3. ✅ `MARK_ATTENDANCE_API_REFERENCE.md`

---

## 📋 API Usage

### New API Format

#### Without Branch Filter (Works for all branches)
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177" -Method GET
```

#### With Branch Filter (Recommended - Validates branch)
```powershell
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=29" -Method GET
```

---

## 🔍 Request Details

### Endpoint
```
GET /employee/{emp_code}
```

### URL Parameters
- `emp_code` (required) - Employee code

### Query Parameters
- `branch_id` (optional) - Branch ID for validation

### Example Requests

**PowerShell:**
```powershell
# Without branch
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177" -Method GET

# With branch validation
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=29" -Method GET
```

**cURL (Linux/Mac):**
```bash
# Without branch
curl http://localhost:5051/employee/13177

# With branch validation
curl "http://localhost:5051/employee/13177?branch_id=29"
```

---

## 📤 Response Format

### Success Response (200)
```json
{
  "status": "success",
  "emp_code": "13177",
  "emp_name": "John Michael Doe",
  "photo_html": "<img src='data:image/jpeg;base64,...' />",
  "branch_id": 29,
  "message": "Employee found: John Michael Doe"
}
```

**New Field:** `branch_id` - Shows which branch the employee belongs to

### Error Response (404) - Without Branch Filter
```json
{
  "status": "error",
  "message": "Employee with code 13177 not found or inactive"
}
```

### Error Response (404) - With Branch Filter
```json
{
  "status": "error",
  "message": "Employee with code 13177 in branch 29 not found or inactive"
}
```

---

## 🔄 Deployment Steps

### To Apply Changes:

1. **Stop the Flask server** (Ctrl+C if running)

2. **Restart the Flask server:**
```powershell
cd E:\sjm\MyHrms
python app.py
```

3. **Test the updated API:**
```powershell
# Test without branch (should work)
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177" -Method GET

# Test with branch validation (should work)
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=29" -Method GET

# Test with wrong branch (should return 404)
Invoke-RestMethod -Uri "http://localhost:5051/employee/13177?branch_id=999" -Method GET
```

---

## 🎯 Benefits

### Why Add Branch Validation?

1. **Security:** Ensures employees can only be accessed within their branch
2. **Multi-Branch Support:** Different branches can have employees with same code
3. **Data Integrity:** Validates employee belongs to correct branch before operations
4. **Better Error Messages:** More specific error when employee not in branch

---

## 🔧 Implementation Details

### Database Query Logic

**Without branch_id:**
```sql
SELECT p.eb_id, p.emp_code, name, f.photo_html, o.branch_id
FROM hrms_ed_personal_details p
INNER JOIN hrms_ed_official_details o ON p.eb_id = o.eb_id
LEFT JOIN employee_face_mst f ON p.eb_id = f.eb_id
WHERE p.emp_code = ? AND p.active = 1
```

**With branch_id:**
```sql
SELECT p.eb_id, p.emp_code, name, f.photo_html, o.branch_id
FROM hrms_ed_personal_details p
INNER JOIN hrms_ed_official_details o ON p.eb_id = o.eb_id
LEFT JOIN employee_face_mst f ON p.eb_id = f.eb_id
WHERE p.emp_code = ? AND p.active = 1 AND o.branch_id = ?
```

---

## 📊 Use Cases

### Use Case 1: General Employee Lookup
**Scenario:** HR admin wants to find any employee
**API Call:** `GET /employee/13177`
**Result:** Returns employee if found in any branch

### Use Case 2: Branch-Specific Lookup
**Scenario:** Branch manager wants to verify employee in their branch
**API Call:** `GET /employee/13177?branch_id=29`
**Result:** Returns employee only if they belong to branch 29

### Use Case 3: Attendance System
**Scenario:** Mobile app marks attendance for specific branch
**API Call:** `GET /employee/13177?branch_id=29`
**Result:** Validates employee exists in the branch before marking attendance

---

## 🔗 Related APIs

All these APIs remain unchanged:

- `POST /mark-attendance` - Still works as before
- `POST /attendance` - Still works as before
- `GET /employees` - Lists all employees
- `GET /employees/search` - Searches employees

---

## ✅ Backward Compatibility

**✅ YES - Fully backward compatible**

- Old API calls without `branch_id` **still work**
- Existing mobile apps **don't need updates**
- `branch_id` parameter is **optional**
- Only affects filtering, not core functionality

---

## 📝 Testing Checklist

- [x] Backend code updated
- [x] Documentation updated (3 files)
- [ ] Server restarted
- [ ] API tested without branch_id
- [ ] API tested with valid branch_id
- [ ] API tested with invalid branch_id
- [ ] Mobile app tested (if applicable)

---

## 🚀 Next Steps

1. **Restart Flask server** to apply changes
2. **Test API** with both formats
3. **Update mobile app** to use branch_id (optional but recommended)
4. **Monitor logs** for any issues

---

## 📞 Support

If you encounter issues:
1. Check server logs for errors
2. Verify employee exists in the database
3. Verify branch_id is correct
4. Review `QUICK_API_CHEAT_SHEET.md` for examples

---

**Document:** EMPLOYEE_API_BRANCH_UPDATE.md  
**Version:** 1.0  
**Date:** April 23, 2026  
**Status:** ✅ Complete - Needs Server Restart


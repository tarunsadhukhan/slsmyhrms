# MyHrms Configuration Update - Complete

## Date: April 23, 2025

## ✅ Configuration Changes

### 1. **Environment Variables (.env)**
Created `.env` file with secure database configuration:
```env
DB_HOST=13.126.47.172
DB_USER=myroot
DB_PASSWORD=deb#9876
DB_NAME=sjm
FLASK_PORT=5051
FLASK_DEBUG=True
```

### 2. **Backend Updates (app.py)**

#### Database Configuration
- ✅ Now loads from `.env` file using `python-dotenv`
- ✅ Secure - credentials not in source code
- ✅ Added `.env` to `.gitignore`
- ✅ Created `.env.example` as template

#### SQL Query Updates - Branch-Based Filtering

**Total Departments:**
```sql
SELECT COUNT(*) AS cnt FROM sub_dept_mst sdm 
LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id 
WHERE dm.branch_id = ?
```

**Department-wise Statistics:**
```sql
SELECT 
    sdm.sub_dept_id AS department_id,
    sdm.sub_dept_name AS department_name,
    COUNT(DISTINCT p.emp_id) AS total_employees
FROM sub_dept_mst sdm
LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id
LEFT JOIN hrms_ed_official_details o ON sdm.sub_dept_id = o.sub_dept_id
LEFT JOIN hrms_ed_personal_details p ON o.emp_id = p.emp_id AND p.active = 1
WHERE dm.branch_id = ?
GROUP BY sdm.sub_dept_id, sdm.sub_dept_name
ORDER BY sdm.sub_dept_name
```

**All Statistics Now Filter By:**
- ✅ Branch ID (`branch_id`) - primary filter
- ✅ Company ID (`co_id`) - fallback filter
- ✅ Employee tables: `hrms_ed_personal_details` + `hrms_ed_official_details`
- ✅ Department tables: `sub_dept_mst` + `dept_mst`
- ✅ Shift table: `spell_mst`

## 🔌 API Endpoint

**URL:** `http://192.168.0.223:5051/dashboard-stats`

**Method:** GET

**Parameters:**
- `date` (optional) - Format: yyyy-MM-dd (defaults to today)
- `branch_id` (optional) - Filter by branch
- `co_id` (optional) - Filter by company

**Example Request:**
```
GET http://192.168.0.223:5051/dashboard-stats?date=2025-04-23&branch_id=29
```

**Response Structure:**
```json
{
  "status": "success",
  "date": "2025-04-23",
  "total_departments": 5,
  "total_designations": 10,
  "total_shifts": 3,
  "total_employees": 50,
  "total_present": 45,
  "present_face": 30,
  "present_manual": 15,
  "total_absent": 5,
  "department_wise": [
    {
      "department_id": 1,
      "department_name": "Production",
      "total_employees": 20,
      "present": 18,
      "absent": 2
    },
    ...
  ]
}
```

## 📱 Mobile App

**Updated Files:**
1. `ApiService.kt` - Added branch_id and co_id parameters
2. `DashboardActivity.kt` - Passes selected branch/company to API

**How It Works:**
1. User selects Company from dropdown
2. User selects Branch from dropdown
3. Dashboard automatically refreshes with branch-specific data
4. All counts and statistics are filtered by selected branch

## 🚀 Deployment

### Backend Server

**Start Server:**
```bash
cd E:\sjm\MyHrms
python app.py
```

**Server Info:**
- Port: 5051
- Host: 0.0.0.0 (listens on all interfaces)
- Access URL: http://192.168.0.223:5051
- Database: sjm @ 13.126.47.172

### Mobile App

**Build APK:**
```bash
cd E:\sjm\MyHrms
.\gradlew assembleDebug
```

**Install:**
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**APK Location:**
```
E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

## 🔒 Security

**Protected Files:**
- `.env` - Contains sensitive database credentials
- Added to `.gitignore` - Won't be committed to git
- `.env.example` - Template file (safe to commit)

**Setup for New Users:**
1. Copy `.env.example` to `.env`
2. Update with actual database credentials
3. Run `python app.py`

## 📊 Database Schema

**Key Tables:**
- `hrms_ed_personal_details` - Employee personal info
- `hrms_ed_official_details` - Employee work info (branch, dept, designation)
- `sub_dept_mst` - Sub-departments
- `dept_mst` - Departments (has branch_id for filtering)
- `designation_mst` - Designations/positions
- `spell_mst` - Shifts/spells
- `employee_face_mst` - Face images and encodings
- `attendance` - Attendance records

## ✅ Features

### Branch-Based Filtering
All dashboard statistics now filter by selected branch:
- ✅ Total employees in branch
- ✅ Total departments in branch
- ✅ Total designations in branch
- ✅ Attendance counts (present/absent) for branch employees
- ✅ Department-wise breakdown (only departments in selected branch)

### Data Accuracy
- Proper joins between employee personal and official tables
- Department filtering through dept_mst.branch_id
- Active employee filtering (active = 1)
- Distinct employee counting to avoid duplicates

## 🧪 Testing

**Test Dashboard API:**
```bash
# Using PowerShell
Invoke-WebRequest -Uri "http://192.168.0.223:5051/dashboard-stats?date=2025-04-23&branch_id=29" | Select-Object -ExpandProperty Content
```

**Expected Result:**
- Should return actual counts based on branch 29 data
- department_wise array should contain departments from branch 29 only
- All employee counts should be for branch 29 employees only

## 📝 Notes

- The date parameter defaults to current date if not provided
- If neither branch_id nor co_id is provided, returns all data
- branch_id takes precedence over co_id
- All SQL queries use LEFT JOIN to handle missing related records
- Department-wise stats show 0 employees for departments with no assigned staff

## 🔧 Troubleshooting

**If Dashboard Shows Zeros:**
1. Check database connection (verify .env settings)
2. Verify branch_id exists in database
3. Check if there are active employees for that branch
4. Verify attendance records exist for the selected date
5. Check Flask server logs for SQL errors

**Check Server Logs:**
```bash
# Flask server output shows:
🚀 Starting MyHrms Flask Server...
📊 Database: sjm @ 13.126.47.172
✅ Database migration check complete
✅ Server ready at http://0.0.0.0:5051
```

## 📦 Dependencies

**Backend (Python):**
- flask
- mysql-connector-python
- python-dotenv
- face_recognition (optional)
- numpy (optional, for face_recognition)

**Install:**
```bash
pip install flask mysql-connector-python python-dotenv
```

---

**Status:** ✅ All configuration complete and tested
**Server:** 🟢 Running on port 5051
**Mobile App:** 📱 Installed and ready to test


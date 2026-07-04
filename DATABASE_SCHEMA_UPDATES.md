# Database Schema Updates - MyHrms Application

## Date: April 23, 2026

## Overview
Updated the MyHrms application to use the correct database schema with proper table names.

## Table Name Changes

### 1. **Employee Tables**
Previously used a single `employees` table. Now split into:
- **`hrms_ed_personal_details`** - Stores personal information
  - `emp_id` (primary key)
  - `emp_code` (unique identifier)
  - `first_name`, `middle_name`, `last_name`
  - `active` (boolean flag)
  - `created_date`

- **`hrms_ed_official_details`** - Stores official/work information
  - `emp_id` (foreign key to hrms_ed_personal_details)
  - `emp_code`
  - `sub_dept_id` (department)
  - `designation_id`
  - `spell_id` (shift)
  - `branch_id`
  - `co_id` (company ID)

### 2. **Face Registration Table**
- **`employee_face_mst`** - Stores employee face images and encodings
  - `emp_code` (links to employee)
  - `face_image` (base64 encoded image)
  - `face_encoding` (JSON encoded face embedding for recognition)
  - `created_date`

### 3. **Shift Table**
Previously `shifts`, now:
- **`spell_mst`** - Stores shift/spell information
  - `spell_id` (primary key)
  - `spell_name`
  - `spell_start_time`
  - `spell_end_time`
  - `active` (boolean flag)

### 4. **Department Tables**
- **`sub_dept_mst`** - Department information
  - `sub_dept_id` (primary key)
  - `sub_dept_name`
  - `dept_id` (parent department)
  - `branch_id`
  - `co_id`

- **`designation_mst`** - Designation/Position information
  - `designation_id` (primary key)
  - `desig` (designation name)
  - `dept_id`
  - `branch_id`
  - `co_id`
  - `active` (boolean flag)

## API Changes

### Backend (Flask - app.py)

1. **GET /employees**
   - Now queries `hrms_ed_personal_details` + `hrms_ed_official_details`
   - Joins with `employee_face_mst` for face images
   - Joins with `spell_mst` for shift information

2. **POST /register**
   - Inserts into `hrms_ed_personal_details` (personal info)
   - Inserts into `hrms_ed_official_details` (official info)
   - Inserts into `employee_face_mst` (face image if provided)

3. **GET /shifts**
   - Now queries `spell_mst` table
   - Maps `spell_id` → `id`, `spell_name` → `name`

4. **GET /dashboard-stats**
   - Now accepts `branch_id` and `co_id` query parameters
   - Filters statistics by selected branch/company
   - Department-wise stats filtered by branch
   - Uses proper employee table joins

5. **GET /designations**
   - Already using `designation_mst` table
   - Filters by `branch_id` and optional `sub_dept_id`

## Mobile App Changes

### 1. **ApiService.kt**
```kotlin
@GET(ApiRoutes.DASHBOARD_STATS)
fun getDashboardStats(
    @Query("date") date: String,
    @Query("branch_id") branchId: Int? = null,
    @Query("co_id") coId: Int? = null
): Call<DashboardStatsResponse>
```

### 2. **DashboardActivity.kt**
- Updated `loadDashboardStats()` to pass `selectedBranchId` and `selectedCompanyId`
- Dashboard now shows data filtered by selected branch

## Features

### Dashboard Statistics
- **Total counts** filtered by branch/company:
  - Total Departments
  - Total Designations  
  - Total Shifts
  - Total Employees
  - Total Present
  - Present (Face Recognition)
  - Present (Manual)
  - Total Absent

- **Department-wise breakdown**:
  - Shows each department's statistics
  - Total employees per department
  - Present count per department
  - Absent count per department
  - **All filtered by selected branch**

## How It Works

1. **Branch Selection**: User selects company and branch from dropdowns
2. **Dashboard Loading**: 
   - API called with selected `branch_id` and `co_id`
   - Backend filters all queries by these parameters
3. **Department Statistics**:
   - Only departments from selected branch are shown
   - Employee counts are branch-specific
   - Attendance data is filtered by branch

## Testing

### Test the Dashboard:
1. Open the app and login
2. Select a company (e.g., co_id=2)
3. Select a branch (e.g., branch_id=29 - HO or branch_id=30 - FACTORY)
4. Dashboard should show:
   - Statistics for that specific branch
   - Department-wise data only for that branch
   - Non-zero values if there are employees/attendance records

### Test Employee Registration:
1. Go to Employee Master
2. Add new employee with:
   - Employee Code
   - Name
   - Select Department (from selected branch)
   - Select Designation (from selected branch)
   - Select Shift
   - Capture Face (optional)
3. Data will be saved to:
   - `hrms_ed_personal_details`
   - `hrms_ed_official_details`
   - `employee_face_mst` (if face captured)

## Backend Server

**Start Command:**
```bash
cd E:\sjm\MyHrms
python app.py
```

**Server URL:** `http://0.0.0.0:5051`

**Mobile Default URL:** `http://192.168.0.223:5051`

## Installation

**APK Location:**
```
E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

**Install Command:**
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Notes

- Face recognition requires `face_recognition` Python library (optional)
- Database connection configured in `app.py` → `DB_CONFIG`
- All employee queries now use joins between personal and official details tables
- Department filtering is branch-specific throughout the application


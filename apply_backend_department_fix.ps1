# ==================================================================
# PowerShell Script to Fix department_present Backend Issue
# ==================================================================

$backendFile = "e:\sjm\attendancesystem\app.py"

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "  FIXING BACKEND: department_present Empty Issue" -ForegroundColor Yellow
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""

# Check if backend file exists
if (-not (Test-Path $backendFile)) {
    Write-Host "ERROR: Backend file not found at: $backendFile" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please verify the backend location and run again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Found backend file: $backendFile" -ForegroundColor Green

# Create backup
$backupFile = "$backendFile.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Copy-Item $backendFile $backupFile
Write-Host "Created backup: $backupFile" -ForegroundColor Green

# Read the file
$content = Get-Content $backendFile -Raw

# Define the old code pattern to find
$oldPattern = @"
        # Department-wise statistics \(filtered by branch\)
        # Using better query to get department data with present count in one go
        dept_stats_query = """
            SELECT
                sdm\.sub_dept_id AS department_id,
                sdm\.sub_dept_desc AS department_name,
                COUNT\(DISTINCT o\.emp_id\) AS total_employees,
                COALESCE\(SUM\(CASE WHEN da\.attendance_date = %s THEN 1 ELSE 0 END\), 0\) AS present
            FROM sub_dept_mst sdm
            LEFT JOIN dept_mst dm ON dm\.dept_id = sdm\.dept_id
            LEFT JOIN hrms_ed_official_details o ON sdm\.sub_dept_id = o\.sub_dept_id
            LEFT JOIN daily_attendance da ON da\.eb_id = o\.eb_id
                AND da\.worked_department_id = sdm\.sub_dept_id
                AND da\.attendance_date = %s
        """
        dept_stats_params = \[stat_date, stat_date\]
"@

# Define the new code
$newCode = @"
        # Department-wise statistics (filtered by branch)
        # Query to get department data with present count based on worked_department_id
        dept_stats_query = """
            SELECT
                sdm.sub_dept_id AS department_id,
                sdm.sub_dept_desc AS department_name,
                0 AS total_employees,
                COUNT(da.eb_id) AS present
            FROM sub_dept_mst sdm
            LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id
            LEFT JOIN daily_attendance da
                ON da.worked_department_id = sdm.sub_dept_id
                AND da.attendance_date = %s
                AND da.is_active = 1
        """
        dept_stats_params = [stat_date]
"@

# Apply the fix
if ($content -match $oldPattern) {
    $content = $content -replace $oldPattern, $newCode
    Write-Host "Applied query fix" -ForegroundColor Green
} else {
    Write-Host "Could not find exact old code pattern. Attempting alternative match..." -ForegroundColor Yellow

    # Try simpler pattern
    $simpleOld = "COUNT\(DISTINCT o\.emp_id\) AS total_employees,\s+COALESCE\(SUM\(CASE WHEN da\.attendance_date = %s THEN 1 ELSE 0 END\), 0\) AS present"
    $simpleNew = "0 AS total_employees,`r`n                COUNT(da.eb_id) AS present"

    if ($content -match $simpleOld) {
        $content = $content -replace $simpleOld, $simpleNew

        # Fix params
        $content = $content -replace "dept_stats_params = \[stat_date, stat_date\]", "dept_stats_params = [stat_date]"

        # Fix JOIN
        $content = $content -replace "LEFT JOIN hrms_ed_official_details o ON sdm\.sub_dept_id = o\.sub_dept_id\s+LEFT JOIN daily_attendance da ON da\.eb_id = o\.eb_id\s+AND da\.worked_department_id = sdm\.sub_dept_id\s+AND da\.attendance_date = %s", @"
LEFT JOIN daily_attendance da
                ON da.worked_department_id = sdm.sub_dept_id
                AND da.attendance_date = %s
                AND da.is_active = 1
"@

        Write-Host "Applied query fix (alternative method)" -ForegroundColor Green
    } else {
        Write-Host "Could not find code to replace. Please apply fix manually." -ForegroundColor Red
        Write-Host "   See: FIX_BACKEND_DEPARTMENT_PRESENT.py for details" -ForegroundColor Yellow
        exit 1
    }
}

# Add logging if not present
if ($content -notmatch "print\(f`"Executing dept_stats query") {
    # Add logging after GROUP BY
    $content = $content -replace "(dept_stats_query \+= `" GROUP BY sdm\.sub_dept_id, sdm\.sub_dept_desc ORDER BY sdm\.sub_dept_desc`"\s+cursor\.execute)", @"
dept_stats_query += " GROUP BY sdm.sub_dept_id, sdm.sub_dept_desc ORDER BY sdm.sub_dept_desc"

        print(f"Executing dept_stats query with params: {dept_stats_params}")
        cursor.execute
"@
    Write-Host "Added query logging" -ForegroundColor Green
}

if ($content -notmatch "print\(f`"Department-wise query results") {
    $content = $content -replace "(cursor\.execute\(dept_stats_query, tuple\(dept_stats_params\)\)\s+dept_stats = cursor\.fetchall\(\))", @"
cursor.execute(dept_stats_query, tuple(dept_stats_params))
        dept_stats = cursor.fetchall()
        print(f"Department-wise query results: {dept_stats}")
"@
    Write-Host "Added result logging" -ForegroundColor Green
}

if ($content -notmatch "print\(f`"department_wise: \{len\(department_wise\)\}") {
    $content = $content -replace "(cursor\.close\(\)\s+db\.close\(\))", @"
print(f"department_wise: {len(department_wise)} items - {department_wise}")
        print(f"department_present: {len(department_present)} items - {department_present}")
        print(f"department_master: {len(department_master)} items - {department_master}")

        cursor.close()
        db.close()
"@
    Write-Host "Added list logging" -ForegroundColor Green
}

# Write the updated content
$content | Set-Content $backendFile -NoNewline

Write-Host ""
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "  BACKEND FIX APPLIED SUCCESSFULLY" -ForegroundColor Green
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "   1. Restart the backend server:" -ForegroundColor White
Write-Host "      cd e:\sjm\attendancesystem" -ForegroundColor Cyan
Write-Host "      python app.py" -ForegroundColor Cyan
Write-Host ""
Write-Host "   2. Install the updated APK on your phone:" -ForegroundColor White
Write-Host "      adb install -r E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
Write-Host ""
Write-Host "   3. Test the app - you should see:" -ForegroundColor White
Write-Host "      - Toast message with department_present data" -ForegroundColor Gray
Write-Host "      - Department list appears when clicking Present card" -ForegroundColor Gray
Write-Host ""
Write-Host "===================================================================" -ForegroundColor Cyan


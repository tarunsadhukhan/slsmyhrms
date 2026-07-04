# Apply Backend Updates Script
# This script helps you apply updates to the backend at e:\sjm\attendancesystem

Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Backend Update Helper" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$backendDir = "e:\sjm\attendancesystem"
$backendFile = Join-Path $backendDir "app.py"

# Check if backend directory exists
if (-not (Test-Path $backendDir)) {
    Write-Host "❌ Backend directory not found: $backendDir" -ForegroundColor Red
    Write-Host "Please verify the backend location" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Backend directory found: $backendDir" -ForegroundColor Green

# Check if app.py exists
if (-not (Test-Path $backendFile)) {
    Write-Host "❌ app.py not found in backend directory" -ForegroundColor Red
    exit 1
}

Write-Host "✅ app.py found: $backendFile" -ForegroundColor Green
Write-Host ""

# Create backup
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $backendDir "app_backup_$timestamp.py"

Write-Host "Creating backup..." -ForegroundColor Yellow
Copy-Item -Path $backendFile -Destination $backupFile -Force
Write-Host "✅ Backup created: app_backup_$timestamp.py" -ForegroundColor Green
Write-Host ""

Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Manual Update Required" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please open the following file and apply updates:" -ForegroundColor Yellow
Write-Host "  $backendFile" -ForegroundColor White
Write-Host ""
Write-Host "Update instructions are in:" -ForegroundColor Yellow
Write-Host "  E:\sjm\MyHrms\BACKEND_UPDATE_FOR_ATTENDANCESYSTEM.py" -ForegroundColor White
Write-Host ""
Write-Host "Changes needed in /attendance-report endpoint:" -ForegroundColor Yellow
Write-Host "  1. Change parameters: date, emp_code, emp_name, shift_name, branch_id" -ForegroundColor White
Write-Host "  2. Add p.eb_id to SELECT query" -ForegroundColor White
Write-Host "  3. Add machine_nos fetching logic" -ForegroundColor White
Write-Host "  4. Add emp_name and shift_name filters" -ForegroundColor White
Write-Host ""
Write-Host "Open the file now? (Y/N)" -ForegroundColor Yellow
$response = Read-Host

if ($response -eq 'Y' -or $response -eq 'y') {
    notepad $backendFile
}

Write-Host ""
Write-Host "After updating, restart the backend server:" -ForegroundColor Yellow
Write-Host "  cd e:\sjm\attendancesystem" -ForegroundColor White
Write-Host "  python app.py" -ForegroundColor White
Write-Host ""
Write-Host "Test the API:" -ForegroundColor Yellow
Write-Host '  curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&branch_id=29"' -ForegroundColor White
Write-Host ""


# Backend Update Script - Department Present Fix
# This script copies the updated app.py to attendancesystem and restarts the server

Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  BACKEND UPDATE - Department Present Fix" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Check if source file exists
$sourceFile = "E:\sjm\MyHrms\app.py"
$destFolder = "e:\sjm\attendancesystem"
$destFile = "$destFolder\app.py"

if (-not (Test-Path $sourceFile)) {
    Write-Host "❌ ERROR: Source file not found: $sourceFile" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $destFolder)) {
    Write-Host "❌ ERROR: Destination folder not found: $destFolder" -ForegroundColor Red
    Write-Host "   Please verify the attendancesystem path" -ForegroundColor Yellow
    exit 1
}

# Backup existing app.py
if (Test-Path $destFile) {
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = "$destFolder\app_backup_$timestamp.py"
    Write-Host "📦 Creating backup..." -ForegroundColor Yellow
    Copy-Item $destFile $backupFile -Force
    Write-Host "   ✅ Backup saved: $backupFile" -ForegroundColor Green
    Write-Host ""
}

# Copy updated file
Write-Host "📋 Copying updated app.py..." -ForegroundColor Yellow
try {
    Copy-Item $sourceFile $destFile -Force
    Write-Host "   ✅ File copied successfully" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ ERROR: Failed to copy file" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Show what changed
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  CHANGES MADE" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Updated dashboard-stats endpoint:" -ForegroundColor White
Write-Host "   • Fixed query to use worked_department_id" -ForegroundColor Gray
Write-Host "   • Added department_present field (only depts with present > 0)" -ForegroundColor Gray
Write-Host "   • Added department_master field (all depts with employees)" -ForegroundColor Gray
Write-Host "   • Optimized query to fetch present count in single query" -ForegroundColor Gray
Write-Host ""

# Instructions
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  NEXT STEPS" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Stop the current Flask server (if running)" -ForegroundColor Yellow
Write-Host "   Press Ctrl+C in the server terminal" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Restart the server:" -ForegroundColor Yellow
Write-Host "   cd $destFolder" -ForegroundColor Cyan
Write-Host "   python app.py" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Test the API:" -ForegroundColor Yellow
Write-Host "   Invoke-WebRequest -Uri 'http://192.168.0.223:5051/dashboard-stats?date=2026-04-25&branch_id=29' | ConvertFrom-Json" -ForegroundColor Cyan
Write-Host ""
Write-Host "4. Expected response:" -ForegroundColor Yellow
Write-Host "   {" -ForegroundColor Gray
Write-Host "     'department_present': [...]  // Only depts with attendance" -ForegroundColor Gray
Write-Host "     'department_master': [...]   // All depts with employees" -ForegroundColor Gray
Write-Host "   }" -ForegroundColor Gray
Write-Host ""

Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  ✅ BACKEND UPDATE COMPLETE" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  Remember to restart the Flask server!" -ForegroundColor Yellow
Write-Host ""


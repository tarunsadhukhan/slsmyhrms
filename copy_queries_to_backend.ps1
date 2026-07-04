# Copy Query Files to Backend
# This script copies the organized query files to the correct backend location

Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Copying Query Files to Backend" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$sourceDir = "E:\sjm\MyHrms"
$backendDir = "e:\sjm\attendancesystem"

# Check if backend directory exists
if (-not (Test-Path $backendDir)) {
    Write-Host "❌ Backend directory not found: $backendDir" -ForegroundColor Red
    Write-Host "Please create the directory or update the path" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Backend directory found: $backendDir" -ForegroundColor Green
Write-Host ""

# Copy query files
$files = @(
    "attendance_queries.py",
    "machine_queries.py",
    "employee_queries.py"
)

foreach ($file in $files) {
    $source = Join-Path $sourceDir $file
    $destination = Join-Path $backendDir $file

    if (Test-Path $source) {
        Copy-Item -Path $source -Destination $destination -Force
        Write-Host "✅ Copied: $file" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Not found: $file" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Next Steps:" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Update app.py in backend with changes from:" -ForegroundColor Yellow
Write-Host "   ATTENDANCE_UPDATE_COMPLETE_CHANGES.md" -ForegroundColor White
Write-Host ""
Write-Host "2. Restart Flask server:" -ForegroundColor Yellow
Write-Host "   cd e:\sjm\attendancesystem" -ForegroundColor White
Write-Host "   python app.py" -ForegroundColor White
Write-Host ""
Write-Host "3. Build Android APK:" -ForegroundColor Yellow
Write-Host "   cd E:\sjm\MyHrms" -ForegroundColor White
Write-Host "   .\gradlew assembleDebug" -ForegroundColor White
Write-Host ""
Write-Host "Done! ✅" -ForegroundColor Green


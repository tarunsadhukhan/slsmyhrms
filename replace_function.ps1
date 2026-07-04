# Script to replace the attendance_report function in attendance.py
$filePath = "e:\sjm\attendancesystem\src\attendance\attendance.py"
$newFunctionPath = "e:\sjm\MyHrms\new_attendance_report_function.py"

# Read the files
$content = Get-Content $filePath -Raw
$newFunction = Get-Content $newFunctionPath -Raw

# Find the function start and end
$functionPattern = '(?s)def attendance_report\(\):.*?(?=\n# "€"€ Get attendance photo|$)'

# Replace the function
$content = $content -replace $functionPattern, $newFunction

# Save the file
Set-Content $filePath $content -NoNewline

Write-Host "✅ Function replaced successfully!" -ForegroundColor Green
Write-Host "📄 File: $filePath" -ForegroundColor Cyan
Write-Host "🔄 Please restart the backend server" -ForegroundColor Yellow


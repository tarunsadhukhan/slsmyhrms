# Restart Backend Server Script

Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Backend Server Restart" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "✅ Backend files updated successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📂 Updated files:" -ForegroundColor Yellow
Write-Host "   - e:\sjm\attendancesystem\src\attendance\attendance.py" -ForegroundColor White
Write-Host "   - e:\sjm\attendancesystem\src\attendance\query.py" -ForegroundColor White
Write-Host ""
Write-Host "📋 Changes applied:" -ForegroundColor Yellow
Write-Host "   ✅ Single date parameter (date)" -ForegroundColor White
Write-Host "   ✅ Employee name filter (emp_name)" -ForegroundColor White
Write-Host "   ✅ Shift name filter (shift_name)" -ForegroundColor White
Write-Host "   ✅ eb_id in response" -ForegroundColor White
Write-Host "   ✅ machine_nos in response" -ForegroundColor White
Write-Host ""

Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Starting Backend Server" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

cd e:\sjm\attendancesystem
python app.py


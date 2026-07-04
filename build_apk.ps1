Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
cd E:\sjm\MyHrms
& .\gradlew.bat assembleDebug
Write-Host ""
Write-Host "Build complete!" -ForegroundColor Green
Write-Host "APK location: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
pause


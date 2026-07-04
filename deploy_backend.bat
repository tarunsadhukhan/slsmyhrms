@echo off
title Deploy Backend to attendancesystem
color 0B
echo.
echo ========================================
echo   DEPLOY BACKEND TO ATTENDANCESYSTEM
echo ========================================
echo.
echo This will copy the updated app.py to e:\sjm\attendancesystem
echo.
pause

echo.
echo Copying app.py...
copy /Y "E:\sjm\MyHrms\app.py" "e:\sjm\attendancesystem\app.py"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   ✅ DEPLOYMENT SUCCESSFUL!
    echo ========================================
    echo.
    echo File copied: app.py
    echo Destination: e:\sjm\attendancesystem\app.py
    echo.
    echo NEXT STEPS:
    echo.
    echo 1. STOP the Flask server if it's running
    echo    Press Ctrl+C in the Flask terminal
    echo.
    echo 2. RESTART the Flask server:
    echo    cd e:\sjm\attendancesystem
    echo    python app.py
    echo.
    echo 3. TEST the API:
    echo    http://localhost:5051/shifts
    echo.
    echo 4. REBUILD the Android APK:
    echo    Run BUILD.bat
    echo.
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   ❌ DEPLOYMENT FAILED!
    echo ========================================
    echo.
    echo Could not copy file to e:\sjm\attendancesystem
    echo.
    echo Possible reasons:
    echo - Destination folder does not exist
    echo - File is in use (close Flask server first)
    echo - Permission denied
    echo.
)

echo.
pause


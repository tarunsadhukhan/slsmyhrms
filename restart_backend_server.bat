@echo off
title Restart Flask Server at attendancesystem
color 0A
echo.
echo ========================================
echo   RESTART FLASK SERVER
echo ========================================
echo.
echo This will start the Flask server at:
echo e:\sjm\attendancesystem
echo.
echo IMPORTANT: Make sure no other Flask instance is running!
echo           Press Ctrl+C in any existing Flask terminal first.
echo.
pause

cd /d e:\sjm\attendancesystem
if not exist "app.py" (
    echo.
    echo ❌ ERROR: app.py not found in e:\sjm\attendancesystem
    echo.
    echo Run deploy_backend.bat first to copy the file!
    echo.
    pause
    exit /b 1
)

echo.
echo Starting Flask server...
echo.
echo ========================================
echo   Server will start on port 5051
echo ========================================
echo.
echo After server starts, test with:
echo http://localhost:5051/shifts
echo.
echo Press Ctrl+C to stop the server
echo.

python app.py


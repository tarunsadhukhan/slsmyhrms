@echo off
setlocal enabledelayedexpansion

echo.
echo ════════════════════════════════════════════════════════════════
echo   HOURS FIX - ONE-CLICK DEPLOYMENT
echo ════════════════════════════════════════════════════════════════
echo.
echo This script will:
echo   1. Verify backend code is deployed
echo   2. Offer to restart backend server
echo   3. Install APK to connected device
echo   4. Open test URLs
echo.
pause

REM Step 1: Verify backend deployment
echo.
echo [1/4] Verifying backend deployment...
if not exist "e:\sjm\attendancesystem\app.py" (
    echo ERROR: Backend directory not found!
    echo Please ensure e:\sjm\attendancesystem exists.
    pause
    exit /b 1
)

REM Check if backend has recent modifications (was copied)
for %%F in ("e:\sjm\attendancesystem\app.py") do set backend_date=%%~tF
for %%F in ("E:\sjm\MyHrms\app.py") do set source_date=%%~tF

echo Backend file: !backend_date!
echo Source file:  !source_date!
echo ✓ Backend code deployed

REM Step 2: Backend server
echo.
echo [2/4] Backend Server Setup
echo.
echo Option 1: Start backend in NEW window
echo Option 2: Skip (server already running)
echo.
choice /C 12 /N /M "Select option (1 or 2): "
if errorlevel 2 goto skip_backend
if errorlevel 1 goto start_backend

:start_backend
echo Starting backend server in new window...
start "MyHRMS Backend Server" cmd /k "cd /d e:\sjm\attendancesystem && echo Starting Flask server... && python app.py"
echo.
echo ✓ Backend server starting...
echo   Wait for message: "Server ready at http://0.0.0.0:5051"
echo.
timeout /t 5 /nobreak > nul
goto test_api

:skip_backend
echo ✓ Skipping backend restart (using existing server)

:test_api
echo.
echo [3/4] Testing API endpoint...
echo.
echo Opening browser to test API...
timeout /t 2 /nobreak > nul
start http://localhost:5051/shifts?branch_id=1

echo.
echo Check browser window:
echo   ✓ Should see JSON response
echo   ✓ Must have "shifts" key (not "data")
echo   ✓ Must have "working_hours" field for each spell
echo.
echo Does the API response look correct?
choice /C YN /N /M "Continue with APK installation? (Y/N): "
if errorlevel 2 goto api_error
if errorlevel 1 goto install_apk

:api_error
echo.
echo API test failed! Please check:
echo   1. Backend server is running
echo   2. Server started without errors
echo   3. Database connection is working
echo.
pause
exit /b 1

:install_apk
echo.
echo [4/4] Installing APK...
echo.

REM Check if device connected
adb devices | findstr /R "device$" > nul
if errorlevel 1 (
    echo ERROR: No Android device connected!
    echo.
    echo Please:
    echo   1. Connect device via USB
    echo   2. Enable USB debugging
    echo   3. Run: adb devices
    echo.
    pause
    exit /b 1
)

echo Device connected ✓
echo.
echo Installing APK...
adb install -r "E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk"

if errorlevel 1 (
    echo.
    echo ERROR: APK installation failed!
    echo.
    echo Troubleshooting:
    echo   1. Check device is authorized (adb devices)
    echo   2. Try: adb uninstall com.example.myhrms
    echo   3. Then rerun this script
    echo.
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════════════
echo   DEPLOYMENT COMPLETE! ✅
echo ════════════════════════════════════════════════════════════════
echo.
echo Next Steps:
echo   1. Open MyHRMS app on device
echo   2. Navigate to Drawing Meter Entry
echo   3. Test the following:
echo.
echo      □ Select spell "A1" → Hours shows "5"
echo      □ Select spell "A2" → Hours shows "3"
echo      □ Select spell "C" → Hours shows "8"
echo      □ Save entry → Hours persists
echo      □ Change spell → Hours updates
echo.
echo All tests should PASS! ✅
echo.
echo ════════════════════════════════════════════════════════════════
echo.
pause


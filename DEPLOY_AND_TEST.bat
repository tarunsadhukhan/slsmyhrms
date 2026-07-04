@echo off
title DEPLOY & TEST - Hours Field Fix
color 0B
cls

echo.
echo ═══════════════════════════════════════════════════════════════
echo   DEPLOY BACKEND AND TEST HOURS FIELD FIX
echo ═══════════════════════════════════════════════════════════════
echo.
echo This script will:
echo   1. Deploy updated app.py to e:\sjm\attendancesystem
echo   2. Show you how to restart the server
echo   3. Test the API
echo.
pause

echo.
echo ═══════════════════════════════════════════════════════════════
echo   STEP 1: DEPLOYING BACKEND
echo ═══════════════════════════════════════════════════════════════
echo.

echo Copying app.py to attendancesystem...
copy /Y "E:\sjm\MyHrms\app.py" "e:\sjm\attendancesystem\app.py"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: Failed to copy file!
    echo.
    echo Possible issues:
    echo   - Folder e:\sjm\attendancesystem does not exist
    echo   - File is locked (close Flask server first)
    echo   - Permission denied
    echo.
    pause
    exit /b 1
)

echo ✅ Backend deployed successfully!
echo.
echo ═══════════════════════════════════════════════════════════════
echo   STEP 2: RESTART FLASK SERVER
echo ═══════════════════════════════════════════════════════════════
echo.
echo IMPORTANT: You MUST restart the Flask server for changes to work!
echo.
echo Instructions:
echo   1. Find the Flask terminal window
echo   2. Press Ctrl+C to stop the server
echo   3. Run: python app.py
echo.
echo OR run the restart script:
echo   restart_backend_server.bat
echo.
echo Press any key when you've restarted the server...
pause > nul

echo.
echo ═══════════════════════════════════════════════════════════════
echo   STEP 3: TEST THE API
echo ═══════════════════════════════════════════════════════════════
echo.
echo Opening browser to test the API...
echo URL: http://localhost:5051/shifts
echo.
timeout /t 2 > nul
start http://localhost:5051/shifts

echo.
echo ═══════════════════════════════════════════════════════════════
echo   VERIFY API RESPONSE
echo ═══════════════════════════════════════════════════════════════
echo.
echo In the browser window that opened, check:
echo.
echo ✓ Response has "shifts" array (NOT "data")
echo ✓ Each spell has "working_hours" field
echo ✓ working_hours has numeric values (e.g., 5.0, 8.0, 12.0)
echo.
echo Example correct response:
echo {
echo   "status": "success",
echo   "shifts": [
echo     {"id": 91, "name": "A1", "working_hours": 5.0}
echo   ]
echo }
echo.
echo Press any key to continue to rebuild APK...
pause > nul

echo.
echo ═══════════════════════════════════════════════════════════════
echo   STEP 4: BUILD ANDROID APK
echo ═══════════════════════════════════════════════════════════════
echo.
echo Now we need to rebuild the Android APK...
echo.
echo Do you want to build the APK now? (Y/N)
set /p BUILD_APK="Enter Y or N: "

if /i "%BUILD_APK%"=="Y" (
    echo.
    echo Building APK...
    cd /d E:\sjm\MyHrms
    call gradlew.bat assembleDebug

    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✅ APK built successfully!
        echo Location: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
    ) else (
        echo.
        echo ❌ APK build failed! Check the errors above.
    )
) else (
    echo.
    echo Skipped APK build. Run BUILD.bat manually when ready.
)

echo.
echo ═══════════════════════════════════════════════════════════════
echo   DEPLOYMENT COMPLETE!
echo ═══════════════════════════════════════════════════════════════
echo.
echo Summary:
echo ✅ Backend deployed to e:\sjm\attendancesystem
echo ⏳ Flask server needs to be restarted
echo ⏳ APK needs to be installed on device
echo.
echo Next steps:
echo   1. Make sure Flask server is running
echo   2. Install the APK on your Android device
echo   3. Open Drawing Meter Entry
echo   4. Test hours auto-fill on spell change
echo.
echo Troubleshooting:
echo   - If hours don't change: Check API response in browser
echo   - If API has no working_hours: Check database spell_mst table
echo   - If app crashes: Check LogCat for errors
echo.
pause


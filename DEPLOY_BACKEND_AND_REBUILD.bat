@echo off
echo ════════════════════════════════════════════════════════════════
echo   DEPLOYING BACKEND AND REBUILDING APK
echo ════════════════════════════════════════════════════════════════
echo.

echo [1/5] Copying app.py to backend...
copy /Y "E:\sjm\MyHrms\app.py" "e:\sjm\attendancesystem\app.py"
if errorlevel 1 (
    echo ERROR: Failed to copy app.py
    pause
    exit /b 1
)
echo ✓ Backend code deployed

echo.
echo [2/5] Please manually restart the backend server:
echo   1. Stop the current server (Ctrl+C in the terminal)
echo   2. Run: python app.py
echo   3. Verify server started at http://localhost:5051
echo.
echo Press any key after server is restarted...
pause > nul

echo.
echo [3/5] Testing /shifts API...
curl -s "http://localhost:5051/shifts?branch_id=1" > test_shifts_response.json
if errorlevel 1 (
    echo ERROR: API call failed. Is server running?
    pause
    exit /b 1
)
echo ✓ API response saved to test_shifts_response.json
echo.
echo API Response:
type test_shifts_response.json
echo.

echo.
echo [4/5] Building APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo ERROR: APK build failed
    pause
    exit /b 1
)
echo ✓ APK built successfully

echo.
echo [5/5] APK Location:
echo   E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
echo.

echo ════════════════════════════════════════════════════════════════
echo   DEPLOYMENT COMPLETE!
echo ════════════════════════════════════════════════════════════════
echo.
echo Next steps:
echo   1. Install APK on device
echo   2. Test spell changes in Drawing Meter Entry
echo   3. Verify hours update when changing spell
echo.
pause


@echo off
echo ================================================
echo   MyHRMS - Machine Fix Installation
echo ================================================
echo.

REM Check if ADB is available
where adb >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ADB not found!
    echo Please install Android SDK Platform Tools
    echo OR add it to your PATH
    pause
    exit /b 1
)

echo Step 1: Checking connected devices...
adb devices
echo.

echo Step 2: Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

if %ERRORLEVEL% EQU 0 (
    echo ================================================
    echo   Installation Successful!
    echo ================================================
    echo.
    echo Next steps:
    echo 1. Open MyHRMS app on your phone
    echo 2. Login
    echo 3. Go to Mark Attendance
    echo 4. Select Designation
    echo 5. Tap Machine Numbers field
    echo 6. Verify machine names show correctly
    echo.
) else (
    echo ================================================
    echo   Installation Failed!
    echo ================================================
    echo.
    echo Troubleshooting:
    echo 1. Make sure USB Debugging is enabled
    echo 2. Connect phone via USB cable
    echo 3. Accept USB debugging prompt on phone
    echo 4. Try running 'adb devices' to verify connection
    echo.
)

pause


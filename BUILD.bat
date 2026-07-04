@echo off
title Building MyHrms APK with Hours Field Fixes
color 0A
echo.
echo ========================================
echo   Building MyHrms APK
echo ========================================
echo.
echo Please wait, this may take 10-20 seconds...
echo.

cd /d E:\sjm\MyHrms
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK Location:
    echo E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Next Steps:
    echo 1. Install the APK on your Android device
    echo 2. Restart backend server: restart_server.bat
    echo 3. Test the hours field functionality
    echo.
) else (
    echo.
    echo ========================================
    echo   BUILD FAILED!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo See BUILD_INSTRUCTIONS.md for help.
    echo.
)

pause


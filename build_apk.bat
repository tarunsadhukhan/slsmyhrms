@echo off
cd /d E:\sjm\MyHrms
call gradlew.bat clean assembleDebug
echo.
echo BUILD COMPLETE!
echo APK Location: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
echo.
echo Version auto-generated: 1.0.260506 (Build 26050601)
pause


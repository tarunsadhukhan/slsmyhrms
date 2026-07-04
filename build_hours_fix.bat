@echo off
echo Building APK with hours field fixes...
cd /d E:\sjm\MyHrms
gradlew.bat assembleDebug
echo.
echo Build complete!
echo APK location: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
pause


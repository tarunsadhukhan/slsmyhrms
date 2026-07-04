@echo off
echo ========================================
echo Android Logcat Viewer for Machine Debug
echo ========================================
echo.

set ADB=C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe

if not exist "%ADB%" (
    echo ERROR: ADB not found!
    echo Please install Android Studio or provide correct ADB path
    pause
    exit /b
)

echo Checking connected devices...
"%ADB%" devices
echo.

echo Clearing old logs...
"%ADB%" logcat -c
echo.

echo ========================================
echo Watching MACHINE_DEBUG logs...
echo Now open the app and tap Machine Numbers
echo ========================================
echo.

"%ADB%" logcat -s MACHINE_DEBUG

pause


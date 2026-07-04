@echo off
echo ========================================
echo  Restarting Flask Backend Server
echo ========================================
echo.
echo IMPORTANT: The backend MUST be restarted for the API changes to take effect!
echo.
echo Stopping any existing Flask processes...
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Flask*" 2>nul

echo.
echo Starting Flask server...
cd /d E:\sjm\MyHrms
start "Flask Backend Server" python app.py

echo.
echo ✅ Server started!
echo Check the Flask window to confirm it's running on port 5051
echo.
pause


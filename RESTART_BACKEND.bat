@echo off
echo ════════════════════════════════════════════════════════════════
echo   RESTART BACKEND SERVER
echo ════════════════════════════════════════════════════════════════
echo.

echo This script will open a new terminal and start the backend server.
echo.
echo Location: e:\sjm\attendancesystem
echo Command: python app.py
echo.

start cmd /k "cd /d e:\sjm\attendancesystem && python app.py"

echo ✓ Backend server starting in new window...
echo.
echo Wait for the message: "Server ready at http://0.0.0.0:5051"
echo.
echo Then test the API:
echo   http://localhost:5051/shifts?branch_id=1
echo.
echo Press any key to continue...
pause > nul


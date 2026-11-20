@echo off
rem One-click start for TestPilot local environment.
rem Starts Redis (if needed), backend (local profile) and frontend dev server.
rem Optional env guards: TESTPILOT_NO_PAUSE=1 (no pause at end), TESTPILOT_NO_BROWSER=1 (do not open browser).
setlocal
cd /d "%~dp0"
title TestPilot - Start Local
echo.
echo ==========================================
echo   TestPilot local startup
echo ==========================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local.ps1"
set "RC=%errorlevel%"

if not "%RC%"=="0" (
  echo.
  echo Startup FAILED. Review the messages above and logs in run-local-logs.
  echo If configuration is missing, run scripts\setup-local.ps1 once.
  if not defined TESTPILOT_NO_PAUSE pause
  exit /b %RC%
)

echo.
echo ==========================================
echo   TestPilot is ready
echo   Frontend : http://127.0.0.1:5173/
echo   Backend  : http://127.0.0.1:8080/api/health
echo   Logs     : run-local-logs
echo   Stop     : double-click stop-local.cmd
echo ==========================================
if not defined TESTPILOT_NO_BROWSER start "" http://127.0.0.1:5173/
if not defined TESTPILOT_NO_PAUSE pause
exit /b 0

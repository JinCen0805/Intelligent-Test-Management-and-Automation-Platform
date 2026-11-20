@echo off
rem One-click stop for TestPilot local environment.
rem Stops frontend, backend and the project-managed Redis instance (safe: only project processes are stopped).
setlocal
cd /d "%~dp0"
title TestPilot - Stop Local
echo.
echo ==========================================
echo   TestPilot local shutdown
echo ==========================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-local.ps1"
set "RC=%errorlevel%"

if not "%RC%"=="0" (
  echo.
  echo Stop finished with issues. Check the messages above.
)
if not defined TESTPILOT_NO_PAUSE pause
exit /b %RC%
